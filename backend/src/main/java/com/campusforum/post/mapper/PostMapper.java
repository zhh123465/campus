package com.campusforum.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusforum.post.domain.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
