package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDeliveryInterval;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.type.MarschrouteDeliveryIntervalContainer;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.utils.TimeInterval;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(DeliveryIntervalConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class DeliveryIntervalConverterTest extends BaseIntegrationTest {

    @Autowired
    private DeliveryIntervalConverter converter;

    @Test
    void testConversion() {
        TimeInterval timeInterval = new TimeInterval("14:30:00+03:00/15:30:00+03:00");

        MarschrouteDeliveryIntervalContainer converted = converter.convert(timeInterval);

        softly.assertThat(converted.getTimeFrom().getOffsetTime())
            .as("Asserting that converted timeFrom is equal to startTime in timeInterval")
            .isEqualTo(timeInterval.getStartTime());
        softly.assertThat(converted.getInterval())
            .as("Asserting that converted interval is equal to WORKING_TIME")
            .isEqualTo(MarschrouteDeliveryInterval.WORKING_TIME);
    }
}
