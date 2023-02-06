package ru.yandex.market.logistics.lom.lms.converter;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;

@DisplayName("Конвертация параметров партнера из yt")
class PartnerExternalParamsYtToLmsConverterTest extends AbstractTest {

    private final PartnerExternalParamsYtToLmsConverter converter = new PartnerExternalParamsYtToLmsConverter(
        objectMapper
    );

    @Test
    @DisplayName("Извлечение списка параметров партнера")
    void extractExternalParamList() {
        YtPartnerExternalParam param = new YtPartnerExternalParam()
            .setKey("key")
            .setPartnerValues("{\"partner_values\":"
                + "[{\"partner_id\":1,\"value\":\"10\"},"
                + "{\"partner_id\":2,\"value\":\"20\"},"
                + "{\"partner_id\":3,\"value\":\"30\"}]}");

        softly.assertThat(converter.extractPartnerExternalParamValues(param))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(List.of(
                new PartnerExternalParamGroup(1L, List.of(new PartnerExternalParam("key", null, "10"))),
                new PartnerExternalParamGroup(2L, List.of(new PartnerExternalParam("key", null, "20"))),
                new PartnerExternalParamGroup(3L, List.of(new PartnerExternalParam("key", null, "30")))
            ));
    }
}
