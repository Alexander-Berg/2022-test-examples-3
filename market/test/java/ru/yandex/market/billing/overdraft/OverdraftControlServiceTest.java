package ru.yandex.market.billing.overdraft;

import java.time.Instant;
import java.util.Optional;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.yt.YtUtilTest;

/**
 * Тесты для {@link OverdraftControlService}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class OverdraftControlServiceTest extends FunctionalTest {

    private static final Instant I_2019_06_10_232323 = DateTimes.toInstantAtDefaultTz(2019, 6, 10, 23, 23, 23);

    @Autowired
    private OverdraftControlService controlService;

    @Autowired
    private Yt yt;

    @Autowired
    private Cypress cypress;

    @Mock
    private YTreeNode ocTableNode;

    @DbUnitDataSet(
            before = {"db/calendar.before.csv", "db/OverdraftControlServiceTest.before.csv"},
            after = "db/OverdraftControlServiceTest.after.csv"
    )
    @DisplayName("Импорт и обработка информации по овердрафтным счетам")
    @Test
    void test_loadAndProcessOverdrafts() {
        Mockito.when(yt.cypress())
                .thenReturn(cypress);

        Mockito.when(cypress.get(ArgumentMatchers.any(), ArgumentMatchers.eq(Cf.set("creation_time"))))
                .thenReturn(ocTableNode);

        Mockito.when(ocTableNode.getAttribute("creation_time"))
                .thenReturn(
                        Optional.of(
                                YtUtilTest.stringNode("2019-06-10T06:30:09.223767Z")
                        )
                );

        controlService.loadAndProcessOverdrafts(I_2019_06_10_232323);
    }
}
