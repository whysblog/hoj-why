package top.hcode.hoj.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class QuizSubmitDTO {
    @NotBlank(message = "答案不能为空")
    private String answer;
}
