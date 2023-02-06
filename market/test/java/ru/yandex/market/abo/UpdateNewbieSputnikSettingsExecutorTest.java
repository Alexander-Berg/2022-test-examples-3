package ru.yandex.market.abo;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для {@link UpdateNewbieSputnikSettingsExecutor}.
 */
@DbUnitDataSet(before = "UpdateNewbieSputnikSettingsExecutorTest.csv")
public class UpdateNewbieSputnikSettingsExecutorTest extends FunctionalTest {
    @Autowired
    private AboAPI aboAPI;

    @Autowired
    private UpdateNewbieSputnikSettingsExecutor updateNewbieSputnikSettingsExecutor;

    @Test
    void test() {
        updateNewbieSputnikSettingsExecutor.doJob(null);
        Mockito.verify(aboAPI).createNewbieOrderLimit(100L, RatingPartnerType.DROPSHIP_BY_SELLER);
        updateNewbieSputnikSettingsExecutor.doJob(null);
        Mockito.verifyNoMoreInteractions(aboAPI);
    }
}
