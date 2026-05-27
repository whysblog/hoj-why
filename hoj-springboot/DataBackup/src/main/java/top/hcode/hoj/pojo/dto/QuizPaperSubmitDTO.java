package top.hcode.hoj.pojo.dto;

import lombok.Data;

import java.util.Map;

@Data
public class QuizPaperSubmitDTO {
    /**
     * 题目 id -> 用户答案（单选 "A"；多选 "AB" 或 "B,A" 等，后台会规范化）
     */
    private Map<String, String> answers;

    /**
     * 编程题提交快照：pid -> 本次作答时前端看到的状态/得分/语言
     */
    private Map<String, ProblemSnapshotDTO> problemSnapshots;

    @Data
    public static class ProblemSnapshotDTO {
        private Integer status;
        private Integer score;
        private String language;
        private Integer maxScore;
    }
}
