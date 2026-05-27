package top.hcode.hoj.pojo.vo;

import lombok.Data;

@Data
public class QuizPaperItemVO {
    private Integer no;
    private String itemType;
    private Long questionId;
    private String problemId;
    private String title;
    private Integer questionType;
    private QuizQuestionInfoVO quizQuestion;
}
