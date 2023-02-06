package ru.yandex.market.antifraud.orders.service.loyalty.detectors;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyAntifraudContext;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetector;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetectorResult;
import ru.yandex.market.antifraud.orders.storage.entity.rules.DetectorConfiguration;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author dzvyagin
 */
public class PromoRestrictedDetectorTest {

    @Test
    public void testCheck(){
        LoyaltyDetector detector = new PromoRestrictedDetector();
        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .originRequest(LoyaltyVerdictRequestDto.builder().coins(Collections.emptyList()).build())
                .build();
        LoyaltyDetectorResult result = detector.check(context, mock(DetectorConfiguration.class));
        assertThat(result.getVerdict()).isEqualTo(LoyaltyVerdictType.OTHER);
    }

}
