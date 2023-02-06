package ru.yandex.direct.core.entity.tax.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.tax.model.TaxInfo;
import ru.yandex.direct.core.entity.tax.repository.TaxHistoryRepository;
import ru.yandex.direct.currency.Percent;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TaxHistoryServiceTest {

    @Parameterized.Parameter
    public LocalDate date;

    @Parameterized.Parameter(1)
    public TaxInfo expectedResult;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        LocalDate.of(2003, 1, 1),
                        new TaxInfo(17L, LocalDate.of(2003, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20)))
                },
                {
                        LocalDate.of(2003, 11, 5),
                        new TaxInfo(17L, LocalDate.of(2003, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20)))
                },
                {
                        LocalDate.of(2004, 1, 1),
                        new TaxInfo(17L, LocalDate.of(2004, 1, 1), Percent.fromPercent(BigDecimal.valueOf(18)))
                },
                {
                        LocalDate.of(2010, 8, 6),
                        new TaxInfo(17L, LocalDate.of(2004, 1, 1), Percent.fromPercent(BigDecimal.valueOf(18)))
                },
                {
                        LocalDate.of(2019, 1, 1),
                        new TaxInfo(17L, LocalDate.of(2019, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20)))
                },
                {
                        LocalDate.of(2019, 7, 8),
                        new TaxInfo(17L, LocalDate.of(2019, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20)))
                }
        });
    }

    private TaxHistoryService taxHistoryService;

    @Before
    public void before() {
        TaxHistoryRepository taxHistoryRepository = mock(TaxHistoryRepository.class);
        long taxId = 17L;
        when(taxHistoryRepository.selectTaxInfos(Set.of(taxId))).thenReturn(Map.of(taxId, Arrays.asList(
                new TaxInfo(taxId, LocalDate.of(2003, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20))),
                new TaxInfo(taxId, LocalDate.of(2004, 1, 1), Percent.fromPercent(BigDecimal.valueOf(18))),
                new TaxInfo(taxId, LocalDate.of(2019, 1, 1), Percent.fromPercent(BigDecimal.valueOf(20)))
        )));
        taxHistoryService = new TaxHistoryService(taxHistoryRepository);
    }

    @Test
    public void test() {
        TaxInfo taxInfo = taxHistoryService.getTaxInfo(17L, date);
        assertThat(taxInfo).isEqualTo(expectedResult);
    }
}
