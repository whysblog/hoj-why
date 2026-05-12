package top.hcode.hoj.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizPaperItemsDTO {
    private List<Long> questionIds;
}
