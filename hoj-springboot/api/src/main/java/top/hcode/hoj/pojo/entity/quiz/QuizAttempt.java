package top.hcode.hoj.pojo.entity.quiz;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.util.Date;

@Data
@TableName("quiz_attempt")
public class QuizAttempt {
    @TableId(type = IdType.AUTO)
    private Long id;
    @JsonIgnore
    private String uid;
    private String kind;
    private Long resourceId;
    private String title;
    private Integer score;
    private Integer maxScore;
    private Integer correctCount;
    private Integer questionCount;
    @JsonIgnore
    private String resultJson;
    private Date gmtCreate;
}
