package ru.yandex.direct.core.entity.tax.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.tax.model.TaxInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.Percent;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Ходит в yt, для ручного запуска")
@CoreTest
@RunWith(SpringRunner.class)
public class TaxHistoryRepositorySmokeTest {

    @Autowired
    TaxHistoryRepository taxHistoryRepository;

    @Test
    public void emptyResult() {
        Map<Long, List<TaxInfo>> taxes = taxHistoryRepository.selectTaxInfos(Set.of(9999L));
        assertThat(taxes).isEmpty();
    }

    @Test
    public void nonEmptyResult() {
        long taxId = 17L;
        Map<Long, List<TaxInfo>> taxInfo = taxHistoryRepository.selectTaxInfos(Set.of(taxId));
        List<TaxInfo> expected = Arrays.asList(
                new TaxInfo(taxId, LocalDate.of(2003, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20))),
                new TaxInfo(taxId, LocalDate.of(2004, 1, 1), Percent.fromPercent(BigDecimal.valueOf(18))),
                new TaxInfo(taxId, LocalDate.of(2019, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20)))
        );
        assertThat(taxInfo).hasSize(1);
        assertThat(taxInfo.get(taxId)).hasSameElementsAs(expected);
    }
}
