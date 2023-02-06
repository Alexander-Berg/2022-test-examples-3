package ru.yandex.market.billing.pg.validation.goe;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.pg.validation.ComparableRow;
import ru.yandex.market.billing.pg.validation.ComparableTableMeta;
import ru.yandex.market.billing.pg.validation.OraPgValidationPgDAO;
import ru.yandex.market.billing.pg.validation.OraPgValidationService;
import ru.yandex.market.billing.pg.validation.ValidationGetterDAO;
import ru.yandex.market.common.test.db.DbUnitDataSet;


@ExtendWith(MockitoExtension.class)
class GoeOraPgValidationServiceTest extends FunctionalTest {

    private static final LocalDate FROM_DATE = LocalDate.of(2021, Month.SEPTEMBER, 20);
    private static final LocalDate TO_DATE = LocalDate.of(2021, Month.OCTOBER, 1);

    private static final ComparableTableMeta TABLE_META = new ComparableTableMeta(
            "market_billing.cpa_order",
            "market_billing.cpa_order",
            List.of("status", "order_id", "items_total", "shop_id"),
            "status",
            "creation_date");

    @Mock
    private GoeOraPgValidationDAO goeOraValidationDAO;

    @Mock
    private GoeOraPgValidationDAO goePgValidationDAO;

    private GoeOraPgValidationService goeOraPgValidationService;

    @Autowired
    private ValidationGetterDAO validationOraGetterDAO;

    @Autowired
    private ValidationGetterDAO validationPgGetterDAO;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Qualifier("namedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2021, 10, 12, 0, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC
    );

    @BeforeEach
    void setup() {
        OraPgValidationService oraPgValidationService = new OraPgValidationService(
                validationOraGetterDAO,
                validationPgGetterDAO,
                new OraPgValidationPgDAO(oraJdbcTemplate, FIXED_CLOCK),
                transactionTemplate);

        this.goeOraPgValidationService = new GoeOraPgValidationService(
                goeOraValidationDAO,
                goePgValidationDAO,
                oraPgValidationService);
    }

    @Test
    @DisplayName("Сверка количества записей GOE между Oracle и PG. Количество данных совпадает.")
    public void correctCountDataTest() {
        setupDaoMockForCountRows(buildComparableRowsMock(), buildComparableRowsMock());
        Assertions.assertTrue(goeOraPgValidationService.validate(TABLE_META, FROM_DATE, TO_DATE));
    }

    @Test
    @DisplayName("Сверка количества записей GOE между Oracle и PG. Не хватает одной строки в PG")
    public void oneRowInconsistentDataTest() {
        setupDaoMockForCountRows(buildOraComparableRowsMock(), buildPgComparableRowsMock());
        Assertions.assertFalse(goeOraPgValidationService.validate(TABLE_META, FROM_DATE, TO_DATE));
    }

    @Test
    @DisplayName("Сверка количества записей GOE между Oracle и PG. Не хватает больше одной строки в PG")
    public void multipleRowsInconsistentDataTest() {
        setupDaoMockForCountRows(buildOraComparableRowsMockMultiple(), buildPgComparableRowsMock());
        Assertions.assertFalse(goeOraPgValidationService.validate(TABLE_META, FROM_DATE, TO_DATE));
    }

    @Test
    @DisplayName("Сверка количества записей GOE между Oracle и PG. Не хватает одной строки в PG, проверяем " +
            "логирование ошибки в таблицу сверки.")
    @DbUnitDataSet(
            before = "GoeOraPgValidationServiceTest.pgValidationLogInvalidRowTest.before.csv",
            after = "GoeOraPgValidationServiceTest.pgValidationLogInvalidRowTest.after.csv"
    )
    public void pgValidationLogInvalidRowTest() {
        setupDaoMockForCountRows(buildOraComparableRowsMock(), buildPgComparableRowsMock());
        goeOraPgValidationService.validate(TABLE_META, FROM_DATE, TO_DATE);
    }

    private List<ComparableRow> buildOraComparableRowsMockMultiple() {
        return List.of(
                new ComparableRow(2, Map.of(
                        "status", "1")),
                new ComparableRow(1, Map.of(
                        "status", "2")
                )
        );
    }

    private List<ComparableRow> buildOraComparableRowsMock() {
        return List.of(
                new ComparableRow(2, Map.of(
                        "status", "1"))
        );
    }

    private List<ComparableRow> buildComparableRowsMock() {
        return List.of(
                new ComparableRow(2, Map.of(
                        "status", "1"))
        );
    }

    private List<ComparableRow> buildPgComparableRowsMock() {
        return List.of(
                new ComparableRow(1, Map.of(
                        "status", "1"))
        );
    }

    private void setupDaoMockForCountRows(List<ComparableRow> oraCount, List<ComparableRow> pgCount) {
        Mockito.when(goeOraValidationDAO.getComparableCountRows(
                TABLE_META.getOraCountSqlQuery(), TABLE_META.getOraTableName(), TABLE_META, FROM_DATE, TO_DATE
        )).thenReturn(oraCount);
        Mockito.when(goePgValidationDAO.getComparableCountRows(
                TABLE_META.getPgCountSqlQuery(), TABLE_META.getPgTableName(), TABLE_META, FROM_DATE, TO_DATE
        )).thenReturn(pgCount);
    }

}
