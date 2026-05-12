package com.campusforum.points.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.campusforum.common.R;
import com.campusforum.points.dto.PointsLogVO;
import com.campusforum.points.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @GetMapping("/balance")
    public R<Long> balance() {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(pointsService.getBalance(userId));
    }

    @GetMapping("/logs")
    public R<List<PointsLogVO>> logs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        return R.ok(pointsService.page(uid, cursor, limit));
    }
}
