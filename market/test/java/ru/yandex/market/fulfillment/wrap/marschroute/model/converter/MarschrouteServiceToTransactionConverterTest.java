package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.EntityType;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteService;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteServiceDiff;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteServiceKey;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Param;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.api.model.fulfillment.TransactionTypeParam;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Transaction;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MarschrouteServiceToTransactionConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class MarschrouteServiceToTransactionConverterTest extends BaseIntegrationTest {

    @Autowired
    private MarschrouteServiceToTransactionConverter converter;

    @Test
    void convert() {
        MarschrouteServiceKey serviceKey = new MarschrouteServiceKey()
            .setServiceId(1L)
            .setEntityId("ENTITY_ID")
            .setNativeName("NATIVE_NAME")
            .setEntityType(EntityType.ORDER)
            .setServiceDateTime(LocalDateTime.parse("2007-12-03T10:15:30"));

        MarschrouteService marschrouteService = new MarschrouteService()
            .setServiceKey(serviceKey)
            .setAdditionalEntityId("ADDITIONAL_ENTITY_ID")
            .setDiscoveryDateTime(LocalDateTime.parse("2008-12-03T10:15:30"))
            .setHash("ewlrjlfcer3254f56ewrf1e43r5f4ref")
            .setServiceDiff(new MarschrouteServiceDiff().setDiff(BigDecimal.TEN));

        Transaction converted = converter.convert(marschrouteService);

        softly.assertThat(converted.getResourceId().getYandexId())
            .as("Asserting the Yandex ID")
            .isEqualTo(marschrouteService.getAdditionalEntityId());
        softly.assertThat(converted.getResourceId().getPartnerId())
            .as("Asserting the partner ID")
            .isEqualTo(serviceKey.getEntityId());
        softly.assertThat(converted.getDateTime())
            .as("Asserting the dateTime")
            .isEqualTo(DateTime.fromLocalDateTime(marschrouteService.getDiscoveryDateTime()));
        softly.assertThat(converted.getHash())
            .as("Asserting the hash")
            .isEqualTo(marschrouteService.getHash());
        softly.assertThat(converted.getType())
            .as("Asserting the type")
            .isEqualTo(serviceKey.getEntityType().getTransactionType().get());
        softly.assertThat(converted.getAmount())
            .as("Asserting the amount")
            .isEqualTo(marschrouteService.getServiceDiff().getDiff());

        List<Param> params = converted.getParams();

        softly.assertThat(params)
            .as("Asserting the params list size")
            .hasSize(4);

        assertParam(params.get(0), TransactionTypeParam.UNIFORM_NAME.getKey(), ServiceType.OTHER.toString());
        assertParam(params.get(1), TransactionTypeParam.NATIVE_NAME.getKey(), serviceKey.getNativeName());
        assertParam(params.get(2), TransactionTypeParam.SERVICE_DATE_TIME.getKey(),
            DateTime.fromLocalDateTime(serviceKey.getServiceDateTime()).getFormattedDate());
        assertParam(params.get(3), "serviceCategory", "UNKNOWN");
    }

    @Test
    void convertNull() {
        Transaction converted = converter.convert(null);

        softly.assertThat(converted)
            .as("Asserting that the converted transaction is null")
            .isNull();

    }

    private void assertParam(Param actualParam, String expectedKey, String expectedValue) {
        softly.assertThat(actualParam.getKey())
            .as("Asserting the param's key")
            .isEqualTo(expectedKey);
        softly.assertThat(actualParam.getValue())
            .as("Asserting the param's value")
            .isEqualTo(expectedValue);
        softly.assertThat(actualParam.getComment())
            .as("Asserting that the param's comment is null")
            .isNull();
    }
}
