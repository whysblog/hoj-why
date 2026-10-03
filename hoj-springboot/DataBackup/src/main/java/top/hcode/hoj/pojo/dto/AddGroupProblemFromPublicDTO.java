package top.hcode.hoj.pojo.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class AddGroupProblemFromPublicDTO {
    @NotNull
    private Long pid;

    @NotNull
    private Long gid;
}
