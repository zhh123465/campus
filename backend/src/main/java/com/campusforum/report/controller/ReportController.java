package com.campusforum.report.controller;

import com.campusforum.common.R;
import com.campusforum.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public R<Void> create(@RequestBody Map<String, Object> body) {
        String targetType = (String) body.get("targetType");
        Long targetId = Long.valueOf(body.get("targetId").toString());
        String reason = (String) body.get("reason");
        String description = (String) body.getOrDefault("description", null);
        reportService.create(targetType, targetId, reason, description);
        return R.ok();
    }
}
