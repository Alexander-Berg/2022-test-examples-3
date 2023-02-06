package ru.yandex.market.checkout.checkouter.actualization.model;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.MatcherAssert.assertThat;

public class MultiCartContextSerializationTest extends AbstractServicesTestBase {

    @Autowired
    ObjectMapper actualizationLoggingMapper;

    @Test
    void shouldSerializeMultiCartContext() throws JsonProcessingException {
        var context = MultiCartContext.createBy(ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(1L)
                .withUid(1L)
                .withPlatform(Platform.DESKTOP)
                .withContext(Context.MARKET)
                .withActionId("cart")
                .withApiSettings(ApiSettings.PRODUCTION)
                .withColor(Color.BLUE)
                .withActualizeCount(true)
                .withCompareWithReportPrice(true)
                .withDebugAllCourierOptions(true)
                .withExperiments(Experiments.of(Experiments.MARKET_SHOW_DELIVERY_THRESHOLD_FOR_EXPRESS_KEY, "true"))
                .withFake(true)
                .withYandexPlus(true)
                .withIsMultiCart(true)
                .withMinifyOutlets(true)
                .withPrime(true)
                .withYandexEmployee(true)
                .build(), Map.of(FeedOfferId.from(1L, "some offer"), 1));

        assertThat(actualizationLoggingMapper.writeValueAsString(context), Matchers.allOf(
                Matchers.not(Matchers.emptyString())
        ));
    }
}
