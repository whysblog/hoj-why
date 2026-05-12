package top.hcode.hoj.pojo.vo;

import lombok.Data;

/**
 * 套卷提交后单题判分结果（用于答卷解析页）
 */
@Data
public class QuizPaperQuestionResultVO {
    /** 卷内序号，从 1 开始 */
    private Integer no;
    private Long questionId;
    private String title;
    /** 0 单选 1 多选 */
    private Integer questionType;
    /**
     * CORRECT — 答对<br>
     * WRONG — 答错或选项格式不符合要求（仍展示你的答案）<br>
     * UNANSWERED — 未作答或无法解析为有效选项
     */
    private String outcome;
    /** 用户提交的答案（已规范化；未作答为空串） */
    private String userAnswer;
    /** 标准答案（规范化后） */
    private String correctAnswer;
}
