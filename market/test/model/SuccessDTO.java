package ru.yandex.market.mbi.partner_stat.mvc.test.model;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * DTO для удачного ответа из тестовой ручки.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
@ApiModel("Тестовый ответ")
public class SuccessDTO {

    @ApiModelProperty("Какое-то сообщение")
    @JsonProperty("result")
    private final String result;

    public SuccessDTO(final String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
