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
    /** 按卷内顺序逐题解析（客观题 + 编程题） */
    private List<QuizPaperItemResultVO> itemResults;
    /** @deprecated 请使用 itemResults */
    private List<QuizPaperQuestionResultVO> questionResults;
}
