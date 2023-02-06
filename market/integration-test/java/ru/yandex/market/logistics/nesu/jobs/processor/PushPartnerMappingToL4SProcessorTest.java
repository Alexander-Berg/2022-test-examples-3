package ru.yandex.market.logistics.nesu.jobs.processor;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPartnerIdPayload;
import ru.yandex.market.logistics4shops.client.api.PartnerMappingApi;
import ru.yandex.market.logistics4shops.client.model.CreatePartnerMappingRequest;
import ru.yandex.market.logistics4shops.client.model.PartnerType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Пуш соответствия идентификаторов партнера mbi и lms в L4S")
@DatabaseSetup("/jobs/processor/push_partner_mapping_to_l4s/prepare.xml")
class PushPartnerMappingToL4SProcessorTest extends AbstractContextualTest {
    @Autowired
    private PushPartnerMappingToL4SProcessor pushPartnerMappingToL4SProcessor;
    @Autowired
    private PartnerMappingApi partnerMappingApi;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(partnerMappingApi);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка создания")
    void fail(String name, Long partnerId, Long shopId, String errorMessage) {
        softly.assertThatCode(() -> pushPartnerMappingToL4SProcessor.processPayload(
                new ShopIdPartnerIdPayload("1", partnerId, shopId)
            ))
            .hasMessage(errorMessage);
    }

    @Nonnull
    private static Stream<Arguments> fail() {
        return Stream.of(
            Arguments.of("Магазин не найден", 200L, 100L, "Failed to find [SHOP] with ids [100]"),
            Arguments.of("Настройка не найдена", 200L, 1L, "There is no relation between partner 200 and shop 1"),
            Arguments.of(
                "Неверный shopRole",
                3L,
                2L,
                "No enum constant ru.yandex.market.logistics.nesu.model.entity.type.ShopPartnerType.DAAS"
            )
        );
    }

    @Test
    @DisplayName("Успешное создание")
    void success() {
        pushPartnerMappingToL4SProcessor.processPayload(new ShopIdPartnerIdPayload("1", 2, 1));
        verify(partnerMappingApi).createPartnerMapping(createPartnerMappingRequest(1L, 2L, PartnerType.DROPSHIP));
    }

    @Nonnull
    private CreatePartnerMappingRequest createPartnerMappingRequest(
        Long mbiPartnerId,
        Long lmsPartnerId,
        PartnerType partnerType
    ) {
        return new CreatePartnerMappingRequest()
            .mbiPartnerId(mbiPartnerId)
            .lmsPartnerId(lmsPartnerId)
            .partnerType(partnerType);
    }
}
