package com.campusforum.qa.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campusforum.common.R;
import com.campusforum.qa.dto.QaQuestionVO;
import com.campusforum.qa.service.QaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    @GetMapping("/{postId}")
    public R<QaQuestionVO> getByPostId(@PathVariable Long postId) {
        return R.ok(qaService.getByPostId(postId));
    }

    @PostMapping("/{postId}/accept/{commentId}")
    public R<QaQuestionVO> accept(@PathVariable Long postId, @PathVariable Long commentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(qaService.accept(postId, commentId, userId));
    }
}
