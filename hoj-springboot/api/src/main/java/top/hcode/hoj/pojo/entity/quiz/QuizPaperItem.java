package top.hcode.hoj.pojo.entity.quiz;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("quiz_paper_item")
@ApiModel(value = "QuizPaperItem", description = "套卷题目")
public class QuizPaperItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "套卷ID")
    private Long paperId;

    @ApiModelProperty(value = "题目ID")
    private Long questionId;

    @ApiModelProperty(value = "顺序")
    private Integer sortOrder;
}
