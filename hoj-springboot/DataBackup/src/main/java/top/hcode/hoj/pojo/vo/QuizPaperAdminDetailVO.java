package top.hcode.hoj.pojo.vo;

import lombok.Data;
import top.hcode.hoj.pojo.entity.quiz.QuizPaper;

import java.util.List;

@Data
public class QuizPaperAdminDetailVO {
    private QuizPaper paper;
    private List<Long> questionIds;
    private List<QuizPaperItemVO> items;
}
