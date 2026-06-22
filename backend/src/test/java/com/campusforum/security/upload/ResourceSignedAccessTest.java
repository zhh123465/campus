package com.campusforum.security.upload;

import com.campusforum.infra.StorageService;
import com.campusforum.infra.security.MimeTypeValidator;
import com.campusforum.resource.domain.Resource;
import com.campusforum.resource.mapper.ResourceMapper;
import com.campusforum.resource.service.ResourceService;
import com.campusforum.space.domain.Space;
import com.campusforum.space.domain.SpaceMember;
import com.campusforum.space.mapper.SpaceMapper;
import com.campusforum.space.mapper.SpaceMemberMapper;
import com.campusforum.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceSignedAccessTest {

    private ResourceService resourceService;
    private ResourceMapper resourceMapper;
    private SpaceMapper spaceMapper;
    private SpaceMemberMapper spaceMemberMapper;
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        resourceMapper = mock(ResourceMapper.class);
        spaceMapper = mock(SpaceMapper.class);
        spaceMemberMapper = mock(SpaceMemberMapper.class);
        storageService = mock(StorageService.class);
        UserMapper userMapper = mock(UserMapper.class);
        MimeTypeValidator mimeTypeValidator = mock(MimeTypeValidator.class);

        resourceService = new ResourceService(
                resourceMapper,
                userMapper,
                spaceMapper,
                spaceMemberMapper,
                storageService,
                new ObjectMapper(),
                mimeTypeValidator,
                "pdf,doc,docx,ppt,pptx,xls,xlsx,jpg,jpeg,png,gif,webp,md,markdown");
    }

    @Test
    @DisplayName("签名 URL 下载应按签发用户做资源可见性校验，而不是依赖当前请求登录态")
    void downloadAsShouldAuthorizeWithSignedUserId() {
        Resource resource = new Resource();
        resource.setId(100L);
        resource.setUploaderId(10L);
        resource.setSpaceId(20L);
        resource.setVisibility("SPACE");
        resource.setStorageKey("spaces/20/lecture.pdf");
        resource.setDeleted(0);

        Space space = new Space();
        space.setId(20L);
        space.setOwnerId(10L);
        space.setDeleted(0);

        when(resourceMapper.selectById(100L)).thenReturn(resource);
        when(spaceMapper.selectById(20L)).thenReturn(space);
        when(spaceMemberMapper.selectOne(any())).thenReturn(new SpaceMember());
        when(storageService.download("spaces/20/lecture.pdf"))
                .thenReturn(new ByteArrayInputStream("ok".getBytes()));

        assertThat(resourceService.downloadAs(100L, 99L)).isNotNull();

        verify(resourceMapper).incrementDownloadCount(100L);
        verify(storageService).download("spaces/20/lecture.pdf");
    }
}
