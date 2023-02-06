package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.api.request.waybill.CreateInboundRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybill;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillItem;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.waybill.item.util.MarschrouteWaybillItemsTestCreator;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Inbound;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(CreateInboundRequestConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class CreateInboundRequestConverterTest extends BaseIntegrationTest {

    private final MarschrouteWaybillItemsTestCreator creator = new MarschrouteWaybillItemsTestCreator();

    @Autowired
    private CreateInboundRequestConverter converter;

    @Test
    void convert() {
        Inbound inbound = new Inbound(
                new ResourceId("YID", null),
                null,
                ImmutableList.of(creator.createFirstConsignment(), creator.createSecondConsignment(),
                        creator.createThirdConsignment(), creator.createFourthConsignment(), creator.createFifthConsignment()),
                null,
                null,
                DateTimeInterval.fromFormattedValue("2018-09-01T14:30:00+03:00/2018-09-01T15:30:00+03:00"),
                "Ы"
        );

        CreateInboundRequest converted = converter.convert(inbound);

        MarschrouteWaybill waybill = converted.getWaybill();

        softly.assertThat(waybill.getWaybillId())
            .as("Asserting the waybill id")
            .isEqualTo(inbound.getInboundId().getYandexId());
        softly.assertThat(waybill.getComment())
            .as("Asserting the waybill comment")
            .isEqualTo(inbound.getComment());
        softly.assertThat(waybill.getDate())
            .as("Asserting the waybill date")
            .isEqualTo(inbound.getInterval().getFrom().toLocalDateTime());

        List<MarschrouteWaybillItem> items = converted.getItems();

        MarschrouteWaybillItem firstConverted = items.get(0);
        MarschrouteWaybillItem secondConverted = items.get(1);
        MarschrouteWaybillItem thirdConverted = items.get(2);
        MarschrouteWaybillItem fourthConverted = items.get(3);
        MarschrouteWaybillItem fifthConverted = items.get(4);

        softly.assertThat(firstConverted)
            .as("Asserting that the first actual converted item is equal to the expected")
            .isEqualToComparingFieldByFieldRecursively(creator.getFirstWaybillItem());
        softly.assertThat(secondConverted)
            .as("Asserting that the second actual converted item is equal to the expected")
            .isEqualToComparingFieldByFieldRecursively(creator.getSecondWaybillItem());
        softly.assertThat(thirdConverted)
            .as("Asserting that the third actual converted item is equal to the expected")
            .isEqualToComparingFieldByFieldRecursively(creator.getThirdWaybillItem());
        softly.assertThat(fourthConverted)
            .as("Asserting that the fourth actual converted item is equal to the expected")
            .isEqualToComparingFieldByFieldRecursively(creator.getFourthWaybillItem());
        softly.assertThat(fifthConverted)
            .as("Asserting that the fifth actual converted item is equal to the expected")
            .isEqualToComparingFieldByFieldRecursively(creator.getFifthWaybillItem());
    }
}
