package ru.yandex.market.abo.gen.common;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ShopSwitchedOffException;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.gen.common.DeliveryMaxDaysGenerator.ORDER_BEFORE_THRESHOLD_MINUTES;

/**
 * @author komarovns
 * @date 28.03.19
 */
class DeliveryMaxDaysGeneratorTest extends EmptyTest {
    private static final long SHOP_ID = 1;
    private static final int DAY_TO = 0;
    private static final int PRIME_REGION = 55;
    private static final int SECOND_REGION = 76;
    private static final String SECOND_REGION_NAME = "Хабаровск";

    private static final int CURRENT_HOUR = 9;
    private static final int timeZoneOffset = 3;// CURRENT_HOUR - LocalDateTime.now(ZoneOffset.UTC).getHour();

    @Mock
    OfferService offerService;
    @Mock
    GeneratorProfile profile;

    @InjectMocks
    @Autowired
    DeliveryMaxDaysGenerator deliveryMaxDaysGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Т.к. интервал до времени перескока меньше 60 минут, то не понятно как моделировать, если час только начался ;(
     */
    @Test
    @Disabled
    void generateHypothesisTest() throws ShopSwitchedOffException {
        when(offerService.findWithParams(any()))
                .thenAnswer(invocation -> Collections.singletonList(createOffer(ORDER_BEFORE_THRESHOLD_MINUTES - 1)));

        Map<Integer, Integer> region2OrderBefore = Stream.of(PRIME_REGION, SECOND_REGION)
                .collect(Collectors.toMap(
                        Function.identity(),
                        r -> orderBeforeByOffset(1), // nearest orderBefore hour
                        (a, b) -> null,
                        LinkedHashMap::new
                ));
        Hypothesis hyp = deliveryMaxDaysGenerator.generateHypothesis(SHOP_ID, region2OrderBefore, timeZoneOffset);
        assertEquals(PRIME_REGION, hyp.getRegionId().intValue());
        assertTrue(hyp.getDescription().contains(SECOND_REGION_NAME));
    }

    @Disabled
    @ParameterizedTest
    @CsvSource({"1, true", "41, false"})
    void findOptionWithDayToTest(long orderBeforeMinutes, boolean optionFound) {
        Offer offer = createOffer(orderBeforeMinutes);
        LocalDeliveryOption option = DeliveryMaxDaysGenerator.findOptionWithDayTo(offer, DAY_TO, timeZoneOffset);
        assertEquals(optionFound, option != null);
    }

    private Offer createOffer(long orderBeforeMinutes) {
        return createOffer(createOption(orderBeforeMinutes));
    }

    private static Offer createOffer(LocalDeliveryOption... options) {
        Offer offer = new Offer();
        offer.setShopId(SHOP_ID);
        offer.setHyperId(0L);
        offer.setLocalDelivery(Arrays.asList(options));
        return offer;
    }

    private static LocalDeliveryOption createOption(long orderBeforeOffset) {
        LocalDeliveryOption option = new LocalDeliveryOption();
        option.setDayTo(DAY_TO);
        option.setOrderBefore(orderBeforeByOffset(orderBeforeOffset));
        return option;
    }

    // Return nearest orderBefore hour with given offsetMinutes
    private static int orderBeforeByOffset(long orderBeforeOffset) {
        return LocalDateTime.now(ZoneOffset.ofHours(timeZoneOffset))
                .plusMinutes(orderBeforeOffset + 60) // TODO: hack
                .getHour();
    }
}
