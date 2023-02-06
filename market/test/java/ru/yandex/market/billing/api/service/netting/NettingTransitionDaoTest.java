package ru.yandex.market.billing.api.service.netting;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.api.FunctionalTest;
import ru.yandex.market.billing.core.netting.model.NettingTransition;
import ru.yandex.market.billing.core.netting.model.NettingTransitionStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class NettingTransitionDaoTest extends FunctionalTest {
    @Autowired
    private NettingTransitionDao nettingTransitionDao;

    @Test
    @DbUnitDataSet(after = "NettingTransition.save.after.csv")
    void testSave() {
        var nettingTransitions = List.of(
                new NettingTransition(1L, NettingTransitionStatus.ENABLED),
                new NettingTransition(2L, NettingTransitionStatus.DISABLED),
                new NettingTransition(3L, NettingTransitionStatus.REJECTED)
        );

        nettingTransitionDao.upsertNettingTransition(nettingTransitions);
    }
}
