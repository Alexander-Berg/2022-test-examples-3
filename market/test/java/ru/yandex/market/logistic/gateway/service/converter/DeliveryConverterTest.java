package ru.yandex.market.logistic.gateway.service.converter;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport;
import ru.yandex.market.logistic.gateway.service.converter.delivery.CreateSelfExportConverter;
import ru.yandex.market.logistic.gateway.utils.delivery.ApiDtoFactory;
import ru.yandex.market.logistic.gateway.utils.delivery.DtoFactory;

public class DeliveryConverterTest extends BaseTest {
    // TODO: implement other tests

    @Test
    public void convertSelfExportToApiTest() {

        SelfExport selfExport = DtoFactory.createSelfExport();

        ru.yandex.market.logistic.api.model.delivery.SelfExport expected =
            new ru.yandex.market.logistic.api.model.delivery.SelfExport.SelfExportBuilder(
                ApiDtoFactory.createResourceId("111", null),
                ApiDtoFactory.createWarehouse(),
                ApiDtoFactory.DATE_TIME_INTERVAL
            )
            .setVolume(ApiDtoFactory.VOLUME)
            .setWeight(ApiDtoFactory.WEIGHT)
            .setCourier(ApiDtoFactory.createCourier())
            .setOrdersId(Arrays.asList(ApiDtoFactory.createResourceId("2", "ext2"),
                ApiDtoFactory.createResourceId("3", "ext3")))
            .build();


        ru.yandex.market.logistic.api.model.delivery.SelfExport actual =
            CreateSelfExportConverter.convertSelfExportToApi(selfExport).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual selfExport is equal to expected")
            .isEqualToComparingFieldByFieldRecursively(expected);
    }

    @Test
    public void convertSelfExportToApiTestFailOnValidation() {

        SelfExport selfExport = DtoFactory.createSelfExport();

        ru.yandex.market.logistic.api.model.delivery.SelfExport expected =
            new ru.yandex.market.logistic.api.model.delivery.SelfExport.SelfExportBuilder(
                ApiDtoFactory.createResourceId("112", null),
                ApiDtoFactory.createWarehouse(),
                ApiDtoFactory.DATE_TIME_INTERVAL
            )
                .setVolume(ApiDtoFactory.VOLUME)
                .setWeight(ApiDtoFactory.WEIGHT)
                .setCourier(ApiDtoFactory.createCourier())
                .setOrdersId(Arrays.asList(ApiDtoFactory.createResourceId("2", "ext2"), ApiDtoFactory.createResourceId("3", "ext3")))
                .build();

        ru.yandex.market.logistic.api.model.delivery.SelfExport actual =
            CreateSelfExportConverter.convertSelfExportToApi(selfExport).orElse(null);

        assertions.assertThat(actual)
            .as("Asserting the actual selfExport is equal to expected")
            .isNotEqualTo(expected);
    }
}
