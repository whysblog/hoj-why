package top.hcode.hoj.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class QuizPaperDetailVO {
    private Long id;
    private String title;
    private String description;
    private String author;
    private List<QuizQuestionInfoVO> questions;
    private List<QuizPaperItemVO> items;
}
