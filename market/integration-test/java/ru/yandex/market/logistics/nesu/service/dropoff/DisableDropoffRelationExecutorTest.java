package ru.yandex.market.logistics.nesu.service.dropoff;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Тест на выполнение подзадачи отключения дропоффа. Отключить связки, сервисы в графе, удалить эджи.")
class DisableDropoffRelationExecutorTest extends AbstractDisablingSubtaskTest {

    private static final long NOT_DISABLED_DROPSHIP_PARTNER_5 = 5;
    private boolean syncBannersSave;

    @BeforeEach
    void before() {
        syncBannersSave = partnerBannersProperties.isDropoffSync();
        partnerBannersProperties.setDropoffSync(false);
    }

    @AfterEach
    void after() {
        partnerBannersProperties.setDropoffSync(syncBannersSave);
    }

    @Test
    @DatabaseSetup("/service/dropoff/before/disable_dropoff_relation.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/disable_dropoff_relation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное отключение дропоффа с подключенными магазинами")
    void successDisablingDropoff() {
        when(lmsClient.disableDropoff(DROPOFF_LOGISTIC_POINT_ID_321)).thenReturn(
            List.of(
                PartnerResponse.newBuilder().id(NOT_DISABLED_DROPSHIP_PARTNER_5).build()
            )
        );

        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(lmsClient).disableDropoff(DROPOFF_LOGISTIC_POINT_ID_321);
    }

    @Test
    @DatabaseSetup("/service/dropoff/before/disable_dropoff_relation.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/disable_dropoff_relation_no_affected_shops.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное отключение дропоффа без подключенных магазинов")
    void successDisablingDropoffNoAffectedShops() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(lmsClient).disableDropoff(DROPOFF_LOGISTIC_POINT_ID_321);
    }
}
