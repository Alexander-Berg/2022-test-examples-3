package ru.yandex.market.ff4shops.order.change.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.factory.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("Синхронизация разрешений на изменение заказа")
public class PossibleOrderChangeServiceTest extends FunctionalTest {

    private static final Set<PossibleOrderChangeType> TYPES_TO_SYNC = EnumSet.of(
        PossibleOrderChangeType.ORDER_ITEMS
    );

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private PossibleOrderChangeService possibleOrderChangeService;

    @BeforeEach
    void init() {
        when(lmsClient.getPartnerPossibleOrderChanges(eq(TYPES_TO_SYNC)))
            .thenReturn(List.of(
                LmsFactory.possibleOrderChangeGroup(107L, true),
                LmsFactory.possibleOrderChangeGroup(9L, true),
                LmsFactory.possibleOrderChangeGroup(1003939L, false)
            ));
    }

    @Test
    @DisplayName("Создание новых")
    @DbUnitDataSet(after = "PossibleOrderChangeServiceTest.after.csv")
    void saveNew() {
        possibleOrderChangeService.sync();
    }

    @Test
    @DisplayName("Создание новых обновление старых")
    @DbUnitDataSet(
        before = "PossibleOrderChangeServiceTest.before.csv",
        after = "PossibleOrderChangeServiceTest.after.csv"
    )
    void saveNewUpdateOld() {
        possibleOrderChangeService.sync();
    }

    @Test
    @DisplayName("Создание новых обновление и удаление старых")
    @DbUnitDataSet(
        before = "PossibleOrderChangeServiceTest.before.csv",
        after = "PossibleOrderChangeServiceTest.saveNewUpdateAndDeleteOld.after.csv"
    )
    void saveNewUpdateAndDeleteOld() {
        when(lmsClient.getPartnerPossibleOrderChanges(eq(TYPES_TO_SYNC)))
            .thenReturn(List.of(
                LmsFactory.possibleOrderChangeGroup(9L, true),
                LmsFactory.possibleOrderChangeGroup(1003939L, false)
            ));

        possibleOrderChangeService.sync();
    }

    @Test
    @DisplayName("Получение возможности удаления товаров")
    @DbUnitDataSet(before = "PossibleOrderChangeServiceTest.canChangeItems.before.csv")
    void canChangeItems() {
        assertCanChangeItems(1, false);
        assertCanChangeItems(2, false);
        assertCanChangeItems(3, false);
        assertCanChangeItems(4, true);
        assertCanChangeItems(5, true);
    }

    private void assertCanChangeItems(long partnerId, boolean canChange) {
        assertEquals(
            possibleOrderChangeService.canChangeOrderItems(partnerId),
            canChange
        );
    }
}
