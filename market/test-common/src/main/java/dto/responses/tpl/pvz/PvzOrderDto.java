package dto.responses.tpl.pvz;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * ДТОшка для некоторых полей из
 * https://a.yandex-team.ru/arc_vcs/market/market-tpl/pvz/pvz-int/src/main/java/ru/yandex/market/pvz/internal/controller/pi/order/dto/OrderDto.java
 */
@Getter
@Setter
@Accessors(chain = true)
public class PvzOrderDto {
    private Long id;
    private String externalId;
    private Long pickupPointId;
    private Long pvzMarketId;
    private String status;
    private PvzOrderVerificationCodeDto verificationCode;
    private List<PvzOrderItemDto> items;

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class PvzOrderVerificationCodeDto {
        private Boolean accepted;
        private Long attemptsLeftToVerify;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class PvzOrderItemDto {
        private List<String> cisValues;
    }
}
