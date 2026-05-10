package com.campusforum.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusforum.qa.domain.QaQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaQuestionMapper extends BaseMapper<QaQuestion> {
}
