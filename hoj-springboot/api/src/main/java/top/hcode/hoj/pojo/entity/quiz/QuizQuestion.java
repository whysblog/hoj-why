package top.hcode.hoj.pojo.entity.quiz;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("quiz_question")
@ApiModel(value = "QuizQuestion", description = "客观选择题")
public class QuizQuestion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "题目标题")
    private String title;

    @ApiModelProperty(value = "题干")
    private String description;

    @ApiModelProperty(value = "选项A")
    private String optionA;

    @ApiModelProperty(value = "选项B")
    private String optionB;

    @ApiModelProperty(value = "选项C")
    private String optionC;

    @ApiModelProperty(value = "选项D")
    private String optionD;

    @ApiModelProperty(value = "0单选 1多选")
    private Integer questionType;

    @ApiModelProperty(value = "正确答案：单选如A；多选升序如AB")
    private String answer;

    @ApiModelProperty(value = "答案解析，支持 Markdown")
    private String explanation;

    @ApiModelProperty(value = "难度 0-2")
    private Integer difficulty;

    @ApiModelProperty(value = "0隐藏 1公开")
    private Integer status;

    @ApiModelProperty(value = "作者")
    private String author;

    @TableField("lang_category")
    @ApiModelProperty(value = "分类：cpp/python")
    private String langCategory;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;
}
