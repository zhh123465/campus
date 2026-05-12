package com.campusforum.points.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campusforum.points.domain.PointsLog;
import com.campusforum.points.dto.PointsLogVO;
import com.campusforum.points.mapper.PointsLogMapper;
import com.campusforum.user.domain.User;
import com.campusforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsLogMapper pointsLogMapper;
    private final UserMapper userMapper;

    @Transactional
    public void award(Long userId, long amount, String type, String reference) {
        if (amount <= 0) return;
        User user = userMapper.selectById(userId);
        if (user == null) return;

        PointsLog pl = new PointsLog();
        pl.setUserId(userId);
        pl.setAmount(amount);
        pl.setType(type);
        pl.setReference(reference);
        pointsLogMapper.insert(pl);

        user.setPoints((user.getPoints() != null ? user.getPoints() : 0) + amount);
        userMapper.updateById(user);

        log.info("Points awarded: userId={}, amount={}, type={}", userId, amount, type);
    }

    @Transactional
    public boolean spend(Long userId, long amount, String type, String reference) {
        if (amount <= 0) return false;
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        long balance = user.getPoints() != null ? user.getPoints() : 0;
        if (balance < amount) return false;

        PointsLog pl = new PointsLog();
        pl.setUserId(userId);
        pl.setAmount(-amount);
        pl.setType(type);
        pl.setReference(reference);
        pointsLogMapper.insert(pl);

        user.setPoints(balance - amount);
        userMapper.updateById(user);

        log.info("Points spent: userId={}, amount={}, type={}", userId, amount, type);
        return true;
    }

    public long getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return 0;
        return user.getPoints() != null ? user.getPoints() : 0;
    }

    public List<PointsLogVO> page(Long userId, Long cursor, int limit) {
        int size = Math.min(limit, 50);
        QueryWrapper<PointsLog> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (cursor != null) {
            qw.lt("id", cursor);
        }
        qw.orderByDesc("id");
        qw.last("LIMIT " + size);

        long running = getBalance(userId);
        List<PointsLog> logs = pointsLogMapper.selectList(qw);
        List<PointsLogVO> vos = new ArrayList<>();
        for (PointsLog entry : logs) {
            vos.add(PointsLogVO.builder()
                    .id(entry.getId())
                    .userId(entry.getUserId())
                    .amount(entry.getAmount())
                    .type(entry.getType())
                    .reference(entry.getReference())
                    .balance(running)
                    .createdAt(entry.getCreatedAt())
                    .build());
            running -= entry.getAmount();
        }
        return vos;
    }
}
