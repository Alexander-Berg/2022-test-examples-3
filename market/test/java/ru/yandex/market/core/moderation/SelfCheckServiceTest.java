package ru.yandex.market.core.moderation;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.moderation.self.SelfCheckResult;
import ru.yandex.market.core.moderation.self.SelfCheckService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod.API;
import static ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod.PI;

@DbUnitDataSet(before = "selfCheckServiceTest.before.csv")
public class SelfCheckServiceTest extends FunctionalTest {

    @Autowired
    private SelfCheckService tested;
    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    /**
     * Тестирование статуса сампроверки ДСБС.
     * 4 сценария из них один - в процессе, 2 - зафейлено, один - успешный.
     */
    @Test
    void testDsbsModeration() {
        when(aboPublicRestClient.getSelfCheckScenarios(1L, PlacementType.DSBS, API))
                .thenReturn(List.of(
                        new SelfCheckDTO(1L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.NEW)
                                        .build()
                        ),
                        new SelfCheckDTO(1L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.FAIL)
                                        .build()
                        ),
                        new SelfCheckDTO(1L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.FAIL)
                                        .build()
                        ),
                        new SelfCheckDTO(1L,
                                CheckOrderScenarioDTO.builder(1L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()
                        )));

        SelfCheckResult actual = tested.getSelfCheckResults(1L);

        assertNotNull(actual);
        assertEquals(4, actual.getTotalCount());
        assertEquals(2, actual.getFailedCount());
        assertEquals(1, actual.getSuccessCount());
        assertFalse(actual.isSelfcheckPassed());
    }

    /**
     * Тестирование статуса сампроверки Дропшипа.
     * 2 сценария из них 2 успешных.
     */
    @Test
    void testDropshipModeration() {
        when(aboPublicRestClient.getSelfCheckScenarios(2L, PlacementType.DSBB, PI))
                .thenReturn(List.of(
                        new SelfCheckDTO(2L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()
                        ),
                        new SelfCheckDTO(2L,
                                CheckOrderScenarioDTO.builder(2L)
                                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                                        .build()
                        )));

        SelfCheckResult actual = tested.getSelfCheckResults(2L);

        assertNotNull(actual);
        assertEquals(2, actual.getTotalCount());
        assertEquals(0, actual.getFailedCount());
        assertEquals(2, actual.getSuccessCount());
        assertTrue(actual.isSelfcheckPassed());
    }

    /**
     * Тестирование статуса сампроверки обычного белого магазина.
     * Возвращаются нули, так как самопроверки для белых нет.
     */
    @Test
    void testWhiteShopModeration() {
        SelfCheckResult actual = tested.getSelfCheckResults(3L);

        assertNotNull(actual);
        assertEquals(0, actual.getTotalCount());
        assertEquals(0, actual.getFailedCount());
        assertEquals(0, actual.getSuccessCount());
        assertFalse(actual.isSelfcheckPassed());
    }
}
