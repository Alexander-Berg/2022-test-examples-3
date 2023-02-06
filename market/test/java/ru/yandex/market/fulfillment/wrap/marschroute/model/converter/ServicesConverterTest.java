package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrderOption;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ServicesConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class ServicesConverterTest extends BaseIntegrationTest {

    @Autowired
    private ServicesConverter converter;

    @Test
    void convertContainsTrying() {
        Service firstService = new Service(ServiceType.TRYING, null, null, null);

        Service secondService = new Service(ServiceType.CHECK, null, null, null);

        Service thirdService = new Service(ServiceType.OTHER, null, null, null);


        MarschrouteOrderOption converted = converter.convert(
            Arrays.asList(firstService, secondService, thirdService));

        softly.assertThat(converted.getCanTry())
            .as("Asserting option's canTry")
            .isEqualTo(1);
    }

    @Test
    void convertDoesNotContainTrying() {
        Service firstService = new Service(ServiceType.SORT, null, null, null);

        Service secondService = new Service(ServiceType.CHECK, null, null, null);

        Service thirdService = new Service(ServiceType.OTHER, null, null, null);


        MarschrouteOrderOption converted = converter.convert(
            Arrays.asList(firstService, secondService, thirdService));

        softly.assertThat(converted.getCanTry())
            .as("Asserting option's canTry")
            .isNull();
    }

    @Test
    void convertContainsTryingAndContainsNull() {
        Service firstService = null;

        Service secondService = new Service(ServiceType.TRYING, null, null, null);

        Service thirdService = new Service(ServiceType.OTHER, null, null, null);;


        MarschrouteOrderOption converted = converter.convert(
            Arrays.asList(firstService, secondService, thirdService));

        softly.assertThat(converted.getCanTry())
            .as("Asserting option's canTry")
            .isEqualTo(1);
    }

    @Test
    void convertDoesNotContainTryingAndContainsNull() {
        Service firstService = new Service(ServiceType.SORT, null, null, null);

        Service secondService = null;

        Service thirdService = new Service(ServiceType.OTHER, null, null, null);


        MarschrouteOrderOption converted = converter.convert(
            Arrays.asList(firstService, secondService, thirdService));

        softly.assertThat(converted.getCanTry())
            .as("Asserting option's canTry")
            .isNull();
    }

    @Test
    void convertNull() {
        MarschrouteOrderOption converted = converter.convert(null);

        softly.assertThat(converted)
            .as("Asserting that converted option is null")
            .isNull();
    }
}
