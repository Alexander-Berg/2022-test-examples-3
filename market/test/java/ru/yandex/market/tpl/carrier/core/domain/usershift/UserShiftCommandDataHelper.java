package ru.yandex.market.tpl.carrier.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.ArrivalData;

/**
 * @author ungomma
 */
@Component
@RequiredArgsConstructor
public class UserShiftCommandDataHelper {

    private final Clock clock;

    public ArrivalData getArrivalDto() {
        return ArrivalData.builder()
                .latitude(new BigDecimal("55.74"))
                .longitude(new BigDecimal("37.62"))
                .build();
    }
}
