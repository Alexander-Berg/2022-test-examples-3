package ru.yandex.market.deliverycalculator.storage.util.mapper;

import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.jupiter.api.Test;

import ru.yandex.market.deliverycalculator.indexerclient.DeliveryCalculatorXmlUtils;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.TariffInfoDTO;
import ru.yandex.market.deliverycalculator.model.DeliveryRule;
import ru.yandex.market.deliverycalculator.model.DeliveryService;
import ru.yandex.market.deliverycalculator.model.DeliveryServicePriceSchemaType;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class YaDeliveryModelConverterUtilsTest {

    @Test
    void convertToYaDeliveryTariffWithoutNullifyingCustomerTariffsTest() throws JAXBException {
        InputStream in = this.getClass().getResourceAsStream("convertToYaDeliveryTariffTest.xml");

        Unmarshaller unmarshaller = DeliveryCalculatorXmlUtils.createUnmarshaller();
        TariffInfoDTO tariffInfoDTO = (TariffInfoDTO) unmarshaller.unmarshal(in);

        YaDeliveryTariff yaDeliveryTariff = YaDeliveryModelConverterUtils.convertToYaDeliveryTariff(tariffInfoDTO, false);

        assertThat(yaDeliveryTariff.getRulesCount(), is(112));

        assertThat(yaDeliveryTariff.getRule().getChildren(), hasSize(1));
        DeliveryRule locationFrom213Rule = yaDeliveryTariff.getRule().getChildren().iterator().next();
        assertThat(locationFrom213Rule.getChildren(), hasSize(4));
        for (DeliveryRule regionToRule : locationFrom213Rule.getChildren()) {
            assertThat(regionToRule.getServices(), notNullValue());
            assertThat(regionToRule.getServices(), hasSize(4));
            assertThat(regionToRule.getServices(), containsInAnyOrder(
                    createCashService(),
                    createWait20Service(),
                    insuranceService(),
                    returnService()
            ));
        }
    }

    @Test
    void convertToYaDeliveryTariffWithNullifyingCustomerTariffsTest() throws JAXBException {
        InputStream in = this.getClass().getResourceAsStream("convertToYaDeliveryTariffForCustomerTest.xml");

        Unmarshaller unmarshaller = DeliveryCalculatorXmlUtils.createUnmarshaller();
        TariffInfoDTO tariffInfoDTO = (TariffInfoDTO) unmarshaller.unmarshal(in);

        YaDeliveryTariff yaDeliveryTariff = YaDeliveryModelConverterUtils.convertToYaDeliveryTariff(tariffInfoDTO, true);

        assertThat(yaDeliveryTariff.getRulesCount(), is(112));

        assertThat(yaDeliveryTariff.getRule().getChildren(), hasSize(1));
        DeliveryRule locationFrom213Rule = yaDeliveryTariff.getRule().getChildren().iterator().next();
        assertThat(locationFrom213Rule.getChildren(), hasSize(4));
        for (DeliveryRule regionToRule : locationFrom213Rule.getChildren()) {
            assertThat(regionToRule.getServices(), notNullValue());
            assertThat(regionToRule.getServices(), hasSize(4));
            assertThat(regionToRule.getServices(), containsInAnyOrder(
                createCashService(),
                createWait20Service(),
                insuranceService(),
                returnService()
            ));
            if (regionToRule.getOptions() != null) {
                assertThat(
                    regionToRule.getOptions(),
                    everyItem(allOf(
                        hasProperty("cost", equalTo(99)),
                        hasProperty("deltaCost", equalTo(0))
                    ))
                );
            }
        }
    }

    private DeliveryService createCashService() {
        DeliveryService cashService = new DeliveryService();
        cashService.setCode("CASH_SERVICE");
        cashService.setPriceSchemaType(DeliveryServicePriceSchemaType.PERCENT_CASH);
        cashService.setPriceSchemaValue(0.022);
        cashService.setMinPrice(3000);
        cashService.setMaxPrice(330000);
        cashService.setEnabled(true);
        return cashService;
    }

    private DeliveryService createWait20Service() {
        DeliveryService wait20Service = new DeliveryService();
        wait20Service.setCode("WAIT_20");
        wait20Service.setPriceSchemaType(DeliveryServicePriceSchemaType.FIX);
        wait20Service.setPriceSchemaValue(30);
        wait20Service.setMinPrice(3000);
        wait20Service.setMaxPrice(3000);
        wait20Service.setEnabled(true);
        return wait20Service;
    }

    private DeliveryService insuranceService() {
        DeliveryService insuranceService = new DeliveryService();
        insuranceService.setCode("INSURANCE");
        insuranceService.setPriceSchemaType(DeliveryServicePriceSchemaType.PERCENT_COST);
        insuranceService.setPriceSchemaValue(0.005);
        insuranceService.setMinPrice(0);
        insuranceService.setMaxPrice(75000);
        insuranceService.setEnabled(true);
        return insuranceService;
    }

    private DeliveryService returnService() {
        DeliveryService returnService = new DeliveryService();
        returnService.setCode("RETURN");
        returnService.setPriceSchemaType(DeliveryServicePriceSchemaType.PERCENT_DELIVERY);
        returnService.setPriceSchemaValue(0.75);
        returnService.setMinPrice(0);
        returnService.setMaxPrice(9999900);
        returnService.setEnabled(false);
        return returnService;
    }
}
