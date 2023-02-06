package ru.yandex.market.oms.service.mapper.enumeration;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.oms.service.mappers.enumeration.DeliveryPartnerTypeMapper;
import ru.yandex.mj.generated.server.model.DeliveryPartnerType;


public class DeliveryPartnerTypeMapperTest {
    @Test
    public void commonAndApiEnumsAreEqual() {
        var commonValues =
                Arrays.stream(ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.values()).toList();

        var apiValues = Arrays.stream(DeliveryPartnerType.values()).toList();
        commonValues.forEach(common -> {
            var api = DeliveryPartnerTypeMapper.mapToApi(common);
            var commonFromApi = DeliveryPartnerTypeMapper.mapToCommon(api);
            Assertions.assertEquals(common, commonFromApi, "There is no equivalent in api: " + apiValues);
        });
        apiValues.forEach(api -> {
            var common = DeliveryPartnerTypeMapper.mapToCommon(api);
            var apiFromCommon = DeliveryPartnerTypeMapper.mapToApi(common);
            Assertions.assertEquals(api, apiFromCommon, "There is no equivalent in common: " + commonValues);
        });
    }
}
