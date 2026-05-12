package top.hcode.hoj.pojo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class QuizPaperSubmitDTO {
    /**
     * 题目 id -> 用户答案（单选 "A"；多选 "AB" 或 "B,A" 等，后台会规范化）
     */
    private Map<String, String> answers;
}
