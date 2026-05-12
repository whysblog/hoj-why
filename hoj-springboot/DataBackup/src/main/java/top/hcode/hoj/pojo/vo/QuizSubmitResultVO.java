package top.hcode.hoj.pojo.vo;

import lombok.Data;

@Data
public class QuizSubmitResultVO {
    private Boolean correct;
    private String correctAnswer;
    private String message;
}
