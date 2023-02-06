package ru.yandex.market.core.billing.fulfillment.storage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.FfStorageBillingMultiplierTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DbUnitDataSet(before = "db/StorageMultiplierServiceTest.common.before.csv")
public class StorageMultiplierServiceTest extends FunctionalTest {
    @Autowired
    private StorageMultiplierService storageMultiplierService;

    @Autowired
    private TariffsService tariffsService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");
            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }
                return getMultiplierTariffs().stream()
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
    }

    @Test
    @DisplayName("Тест на возвращение двух мультипликаторов по дочерней категории - столовые приборы.")
    void testStandard() {
        innerTest(90693L, 45L, 76L, 12, 36);
    }

    @Test
    @DisplayName("Тест на возвращение только основного мультипликатора для приборов для ухода за телом.")
    void testTrimmed() {
        var multiplier = storageMultiplierService.getStorageMultiplier(null, 1001393L,
                StorageMultiplierService.MultiplierType.STANDARD);
        assertNotNull(multiplier, "Cannot find multiplier.");
        assertThat(multiplier.getDayFrom(), is(76L));
        assertThat(multiplier.getDayTo(), nullValue());
        assertThat(multiplier.getMultiplier(), is(BigDecimal.valueOf(36)));

        multiplier = storageMultiplierService.getStorageMultiplier(null, 1001393L,
                StorageMultiplierService.MultiplierType.EXTRA);
        assertNull(multiplier, "Wrong extra multiplier was found...");
    }

    @Test
    @DisplayName("Тест на возвращение двух мультипликаторов по несуществующей категории.")
    void testWrongCategory() {
        innerTest(1L, 45L, 76L, 12, 36);
    }

    @Test
    @DisplayName("Тест на получение мультипликаторов для автокресел.")
    void testAutoChar() {
        innerTest(512743, 91, 121, 12, 36);
    }

    private void innerTest(long categoryId, long fromFirst, long fromSecond, int costFirst, int costSecond) {
        var multiplier = storageMultiplierService.getStorageMultiplier(null, categoryId,
                StorageMultiplierService.MultiplierType.STANDARD);
        assertNotNull(multiplier, "Cannot find multiplier.");
        assertThat(multiplier.getDayFrom(), is(fromFirst));
        assertThat(multiplier.getDayTo(), is(fromSecond));
        assertThat(multiplier.getMultiplier(), is(BigDecimal.valueOf(costFirst)));

        multiplier = storageMultiplierService.getStorageMultiplier(null, categoryId,
                StorageMultiplierService.MultiplierType.EXTRA);
        assertNotNull(multiplier, "Cannot find extra multiplier.");
        assertThat(multiplier.getDayFrom(), is(fromSecond));
        assertThat(multiplier.getDayTo(), nullValue());
        assertThat(multiplier.getMultiplier(), is(BigDecimal.valueOf(costSecond)));
    }

    private List<TariffDTO> getMultiplierTariffs() {
        return List.of(
                createTariff(2286, null,
                        LocalDate.of(2018, 1, 1),
                        null,
                        List.of(
                                createMeta(1001393, 76, null, 36),
                                createMeta(1001393, 0, 76L, 0),
                                createMeta(13203592, 76, null, 36),
                                createMeta(13203592, 0, 76L, 0),
                                createMeta(91157, 76, null, 36),
                                createMeta(91157, 0, 76L, 0),

                                createMeta(512743, 121, null, 36),
                                createMeta(512743, 91, 121L, 12),
                                createMeta(512743, 0, 91L, 0),

                                createMeta(90796, 121, null, 36),
                                createMeta(90796, 91, 121L, 12),
                                createMeta(90796, 0, 91L, 0),

                                createMeta(7877999, 120, null, 50),
                                createMeta(7877999, 90, 120L, 25),
                                createMeta(7877999, 0, 90L, 0),

                                createMeta(90401, 76, null, 36),
                                createMeta(90401, 45, 76L, 12),
                                createMeta(90401, 0, 45L, 0)
                        ))
        );
    }

    private TariffDTO createTariff(
            long id,
            Long partnerId,
            LocalDate from,
            LocalDate to,
            List<Object> meta
    ) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setIsActive(true);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        if (partnerId != null) {
            tariff.setPartner(new Partner().id(partnerId));
        }
        tariff.setMeta(meta);
        return tariff;
    }

    private CommonJsonSchema createMeta(long categoryId, long from, Long to, int mult) {
        return new FfStorageBillingMultiplierTariffJsonSchema()
                .categoryId(categoryId)
                .daysOnStockFrom(from)
                .daysOnStockTo(to)
                .multiplier(BigDecimal.valueOf(mult));
    }
}
