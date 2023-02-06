package ru.yandex.market.core.cutoff;

import java.time.Duration;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.CutoffType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Date: 25.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "csv/DbCutoffServiceFunctionalTest.before.csv")
class DbCutoffServiceFunctionalTest extends FunctionalTest {

    @Autowired
    private CutoffService cutoffService;

    @DisplayName("Поиск магазинов, которые уже отключены")
    @Test
    void checkCutoffs_fourDatasourceId_twoDatasourceId() {
        Set<Long> shops = cutoffService.checkCutoffs(Set.of(772L, 773L, 774L, 775L, 776L, 777L, 778L),
                CutoffType.TECHNICAL_YML, Duration.ofHours(3));
        assertEquals(3, shops.size());
        Assertions.assertThat(shops)
                .containsExactlyInAnyOrder(774L, 776L, 778L);
    }

    @DisplayName("Поиск магазинов, которые уже отключены, без задержки на закрытие катоффа")
    @Test
    void checkCutoffs_fourDatasourceIdWithDurationZero_twoDatasourceId() {
        Set<Long> shops = cutoffService.checkCutoffs(Set.of(772L, 773L, 774L, 775L, 776L, 777L, 778L),
                CutoffType.TECHNICAL_YML, Duration.ofHours(0));
        assertEquals(2, shops.size());
        Assertions.assertThat(shops)
                .containsExactlyInAnyOrder(774L, 776L);
    }
}
