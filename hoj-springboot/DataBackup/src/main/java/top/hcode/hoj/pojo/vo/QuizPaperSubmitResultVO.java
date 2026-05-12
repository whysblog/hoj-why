package top.hcode.hoj.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuizPaperSubmitResultVO {
    private Long paperId;
    private String paperTitle;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer unansweredCount;
    private String message;
    /** 按卷内顺序逐题解析 */
    private List<QuizPaperQuestionResultVO> questionResults;
}
