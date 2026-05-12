package top.hcode.hoj.pojo.vo;

import lombok.Data;

@Data
public class QuizQuestionListVO {
    private Long id;
    private String title;
    private Integer difficulty;
    private String author;
    /** 0 单选 1 多选 */
    private Integer questionType;
}
