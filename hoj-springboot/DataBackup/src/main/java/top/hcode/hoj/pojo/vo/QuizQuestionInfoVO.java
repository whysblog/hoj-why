package top.hcode.hoj.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionInfoVO {
    private Long id;
    private String title;
    private String description;
    private Integer difficulty;
    private String author;
    /** 0 单选 1 多选 */
    private Integer questionType;
    private List<QuizOptionVO> options;
}
