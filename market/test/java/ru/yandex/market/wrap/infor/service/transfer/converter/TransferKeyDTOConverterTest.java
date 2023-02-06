package ru.yandex.market.wrap.infor.service.transfer.converter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.model.TransferkeyDTO;

class TransferKeyDTOConverterTest extends SoftAssertionSupport {

    private static final String YANDEX_ID = "yandexId";
    private static final String PARTNER_ID = "partnerId";

    private static final Map<String, ResourceId> RESOURCE_ID_MAP = ImmutableMap.of(
        "partnerId_1", new ResourceId("yandexId_1", "partnerId_1"),
        "partnerId_2", new ResourceId("yandexId_2", "partnerId_2")
    );

    private TransferKeyDTOConverter converter = new TransferKeyDTOConverter();

    /**
     * Сценарий #1:
     * <p>Проверка конвертации одного идентификатора в стандартный TransferkeyDTO
     */
    @Test
    void convertSingleResourceId() {
        TransferkeyDTO transferKey = converter.convert(new ResourceId(YANDEX_ID, PARTNER_ID));

        softly
            .assertThat(transferKey.getTransferkey())
            .as("Asserting transfer key (partnerId)")
            .isEqualTo(PARTNER_ID);
    }

    /**
     * Сценарий #1:
     * <p>Проверка конвертации списка идентификаторов в список стандартных TransferkeyDTO
     */
    @Test
    void convertListResourceId() {

        Set<TransferkeyDTO> transferKeyDTOSet = converter.convert(new ArrayList<>(RESOURCE_ID_MAP.values()));

        transferKeyDTOSet.forEach(item -> {
            ResourceId expectedResourceId = RESOURCE_ID_MAP.get(item.getTransferkey());

            softly
                .assertThat(expectedResourceId)
                .as("Expected ResourceId not to be null")
                .isNotNull();

            softly
                .assertThat(item.getTransferkey())
                .as("Asserting transfer key (partnerId)")
                .isEqualTo(expectedResourceId.getPartnerId());
        });
    }

}
