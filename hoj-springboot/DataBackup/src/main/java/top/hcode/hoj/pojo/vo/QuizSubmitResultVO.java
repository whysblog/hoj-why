package top.hcode.hoj.pojo.vo;

import lombok.Data;

@Data
public class QuizSubmitResultVO {
    private Long attemptId;
    private Integer score;
    private Integer maxScore;
    private QuizQuestionInfoVO question;
    private String userAnswer;
    private Boolean correct;
    private String correctAnswer;
    private String explanation;
    private String message;
}
