package top.hcode.hoj.pojo.vo;

import lombok.Data;

/**
 * 套卷提交后单题结果（客观题或编程题，按卷内顺序）
 */
@Data
public class QuizPaperItemResultVO {
    private Integer no;
    /** quiz | problem */
    private String itemType;
    private String title;

    // —— 客观题 ——
    private Long questionId;
    private Integer questionType;
    private String outcome;
    private String userAnswer;
    private String correctAnswer;
    private String explanation;

    // —— 编程题 ——
    private String problemId;
    private Long pid;
    private Integer judgeStatus;
    private String judgeStatusName;
    private String language;
    private Integer score;
    private Integer maxScore;
}
