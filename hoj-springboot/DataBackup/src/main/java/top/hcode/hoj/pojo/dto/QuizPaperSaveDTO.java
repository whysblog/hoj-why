package top.hcode.hoj.pojo.dto;
import lombok.Data;
import java.util.List;
import top.hcode.hoj.pojo.entity.quiz.QuizPaper;
@Data
public class QuizPaperSaveDTO {
    private QuizPaper paper;
    private List<QuizPaperItemDTO> items;
}
