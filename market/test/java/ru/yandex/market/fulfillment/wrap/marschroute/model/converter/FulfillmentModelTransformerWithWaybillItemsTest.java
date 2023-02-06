package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.core.transformer.FulfillmentModelTransformer;
import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybillItem;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.waybill.item.util.MarschrouteWaybillItemsTestCreator;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Consignment;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class FulfillmentModelTransformerWithWaybillItemsTest extends BaseIntegrationTest {

    private final MarschrouteWaybillItemsTestCreator creator = new MarschrouteWaybillItemsTestCreator();

    @Autowired
    private FulfillmentModelTransformer transformer;

    @Test
    void convert() {
        Consignment firstConsignment = creator.createFirstConsignment();
        Consignment secondConsignment = creator.createSecondConsignment();
        Consignment thirdConsignment = creator.createThirdConsignment();
        Consignment fourthConsignment = creator.createFourthConsignment();
        Consignment fifthConsignment = creator.createFifthConsignment();

        List<MarschrouteWaybillItem> converted = transformer.transformFromListToList(ImmutableList.of(firstConsignment, secondConsignment,
            thirdConsignment, fourthConsignment, fifthConsignment), MarschrouteWaybillItem.class);

        MarschrouteWaybillItem firstConverted = converted.get(0);
        MarschrouteWaybillItem secondConverted = converted.get(1);
        MarschrouteWaybillItem thirdConverted = converted.get(2);
        MarschrouteWaybillItem fourthConverted = converted.get(3);
        MarschrouteWaybillItem fifthConverted = converted.get(4);

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
