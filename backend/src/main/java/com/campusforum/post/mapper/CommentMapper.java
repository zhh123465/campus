package com.campusforum.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusforum.post.domain.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
