package ru.yandex.market.tpl.common.web.exception;

import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "Пример дочернего DTO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChildDto {

    @ApiModelProperty("Имя")
    @NotBlank
    private String name;
}
