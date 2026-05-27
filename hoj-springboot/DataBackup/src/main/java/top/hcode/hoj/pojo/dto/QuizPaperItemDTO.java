package top.hcode.hoj.pojo.dto;

import lombok.Data;

@Data
public class QuizPaperItemDTO {
    private String itemType;
    private Long questionId;
    private Integer score;
}
