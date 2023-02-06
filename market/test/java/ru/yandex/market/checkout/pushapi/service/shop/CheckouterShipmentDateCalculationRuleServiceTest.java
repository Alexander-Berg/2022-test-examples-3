package ru.yandex.market.checkout.pushapi.service.shop;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule;
import ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRule;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.DELIVERY_DATE;
import static ru.yandex.market.checkout.checkouter.shop.shipment.ShipmentDateCalculationRegionalRule.BaseDateForCalculation.ORDER_CREATION_DATE;

public class CheckouterShipmentDateCalculationRuleServiceTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer checkouterMock;
    @Autowired
    private CheckouterShipmentDateCalculationRuleService tested;

    @Test
    void testShipmentDateRetrieval() throws IOException {
        String requestBody = IOUtils.readInputStream(CheckouterShipmentDateCalculationRuleServiceTest.class
                .getResourceAsStream("/files/shipmentDateCalcRules.json"));

        checkouterMock.givenThat(get(urlPathEqualTo("/shops/1122/shipment/date-calculation-rule"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(requestBody)
                        .withHeader("Content-Type", "application/json")));

        Optional<ShipmentDateCalculationRule> rule = tested.getForShop(1122);

        assertTrue(rule.isPresent());
        assertEquals(ShipmentDateCalculationRule.builder()
                .withRuleForLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(ORDER_CREATION_DATE)
                        .withDaysToAdd(2)
                        .build())
                .withRuleForNonLocalDeliveryRegion(ShipmentDateCalculationRegionalRule.builder()
                        .withBaseDateForCalculation(DELIVERY_DATE)
                        .withDaysToAdd(-1)
                        .build())
                .withHourBefore(13)
                .withHolidays(asList(LocalDate.of(2021, 2, 23),
                        LocalDate.of(2021, 2, 25)))
                .build(), rule.get());
    }

}
