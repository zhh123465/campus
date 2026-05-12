package com.campusforum.report.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusforum.report.domain.Report;
import com.campusforum.report.dto.ReportVO;
import com.campusforum.report.mapper.ReportMapper;
import com.campusforum.user.domain.User;
import com.campusforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    @Transactional
    public void create(String targetType, Long targetId, String reason, String description) {
        Report r = new Report();
        r.setReporterId(StpUtil.getLoginIdAsLong());
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReason(reason);
        r.setDescription(description);
        r.setStatus(0);
        reportMapper.insert(r);
        log.info("Report created: reporter={}, type={}, targetId={}", r.getReporterId(), targetType, targetId);
    }

    @Transactional
    public void handle(Long reportId, Integer newStatus, String note) {
        Report r = reportMapper.selectById(reportId);
        if (r == null) return;
        r.setStatus(newStatus);
        r.setHandlerId(StpUtil.getLoginIdAsLong());
        r.setHandleNote(note);
        r.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(r);
        log.info("Report handled: id={}, status={}, handler={}", reportId, newStatus, r.getHandlerId());
    }

    public List<ReportVO> page(Long cursor, int limit, String targetType, Integer status) {
        int size = Math.min(limit, 50);
        QueryWrapper<Report> qw = new QueryWrapper<>();
        if (cursor != null) {
            qw.lt("id", cursor);
        }
        if (targetType != null && !targetType.isBlank()) {
            qw.eq("target_type", targetType);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("id");
        qw.last("LIMIT " + size);

        return reportMapper.selectList(qw).stream().map(this::toVO).toList();
    }

    private ReportVO toVO(Report r) {
        String reporterName = null;
        String handlerName = null;
        if (r.getReporterId() != null) {
            User reporter = userMapper.selectById(r.getReporterId());
            if (reporter != null) reporterName = reporter.getNickname();
        }
        if (r.getHandlerId() != null) {
            User handler = userMapper.selectById(r.getHandlerId());
            if (handler != null) handlerName = handler.getNickname();
        }
        return ReportVO.builder()
                .id(r.getId())
                .reporterId(r.getReporterId())
                .reporterName(reporterName)
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reason(r.getReason())
                .description(r.getDescription())
                .status(r.getStatus())
                .handlerId(r.getHandlerId())
                .handlerName(handlerName)
                .handleNote(r.getHandleNote())
                .createdAt(r.getCreatedAt())
                .handledAt(r.getHandledAt())
                .build();
    }
}
