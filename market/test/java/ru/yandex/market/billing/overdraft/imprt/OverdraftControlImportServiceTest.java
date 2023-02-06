package ru.yandex.market.billing.overdraft.imprt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.yt.YtUtilTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.billing.overdraft.matchers.BalanceOverdraftInvoiceMatchers.hasExternalId;
import static ru.yandex.market.billing.overdraft.matchers.BalanceOverdraftInvoiceMatchers.hasInvoicePaymentTime;
import static ru.yandex.market.billing.overdraft.matchers.BalanceOverdraftInvoiceMatchers.hasStatus;
import static ru.yandex.market.billing.overdraft.matchers.BalanceOverdraftInvoiceMatchers.invoiceMatcher;

/**
 * Тесты для {@link OverdraftControlImportService}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class OverdraftControlImportServiceTest extends FunctionalTest {

    private static final Instant I_2019_5_1_010101 = toInstantAtUtcTz(2019, 5, 1, 1, 1, 1);

    @Autowired
    private OverdraftControlImportService overdraftControlImportService;

    @Autowired
    private Yt yt;

    @Value("${mbi.billing.overdraft-control.yt.path_raw}")
    private String ytOcPath;

    @Autowired
    private Cypress cypress;

    @Mock
    private YTreeNode ocTableNode;

    private static Instant toInstantAtUtcTz(int y, int m, int d, int hh, int mm, int ss) {
        final LocalDateTime dateTime = LocalDateTime.of(y, m, d, hh, mm, ss);
        return dateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    @BeforeEach
    void beforeEach() {
        Mockito.when(yt.cypress())
                .thenReturn(cypress);

        Mockito.when(cypress.get(YPath.simple(ytOcPath), Cf.set("creation_time")))
                .thenReturn(ocTableNode);
    }

    @DbUnitDataSet(before = "db/OverdraftControlImportServiceTest.before.csv")
    @Test
    void test_import() {

        Mockito.when(ocTableNode.getAttribute("creation_time"))
                .thenReturn(
                        Optional.of(
                                YtUtilTest.stringNode("2019-06-10T06:30:09.223767Z")
                        )
                );

        List<BalanceOverdraftInvoice> actual = overdraftControlImportService.loadExpiredInvoicesInfo(
                LocalDate.of(2019, 6, 3),
                LocalDate.of(2019, 6, 10)
        );
        assertThat(
                actual,
                contains(
                        ImmutableList.of(
                                invoiceMatcher(1L,
                                        "Б-111",
                                        I_2019_5_1_010101,
                                        LocalDate.of(2019, 6, 7),
                                        null,
                                        BalanceInvoiceStatus.OVERDUE_UNPAID
                                ),
                                allOf(
                                        hasExternalId("Б-222"),
                                        hasStatus(BalanceInvoiceStatus.OVERDUE_UNPAID)
                                ),
                                allOf(
                                        hasExternalId("Б-333"),
                                        hasStatus(BalanceInvoiceStatus.OVERDUE_UNPAID)
                                ),
                                allOf(
                                        hasExternalId("Б-444"),
                                        hasInvoicePaymentTime(toInstantAtUtcTz(2019, 6, 9, 5, 1, 1)),
                                        hasStatus(BalanceInvoiceStatus.OVERDUE_UNPAID)
                                ),
                                allOf(
                                        hasExternalId("Б-555"),
                                        hasInvoicePaymentTime(toInstantAtUtcTz(2019, 6, 8, 1, 1, 1)),
                                        hasStatus(BalanceInvoiceStatus.OVERDUE_UNPAID)
                                ),
                                allOf(
                                        hasExternalId("Б-155"),
                                        hasInvoicePaymentTime(toInstantAtUtcTz(2019, 6, 8, 1, 1, 1)),
                                        hasStatus(BalanceInvoiceStatus.OVERDUE_PAID)
                                ),
                                invoiceMatcher(16L,
                                        "Б-166",
                                        I_2019_5_1_010101,
                                        LocalDate.of(2019, 5, 10),
                                        toInstantAtUtcTz(2019, 6, 8, 1, 1, 1),
                                        BalanceInvoiceStatus.OVERDUE_PAID
                                ),
                                invoiceMatcher(17L,
                                        "Б-177",
                                        I_2019_5_1_010101,
                                        null,
                                        toInstantAtUtcTz(2019, 6, 8, 1, 1, 1),
                                        BalanceInvoiceStatus.OVERDUE_PAID
                                ),
                                invoiceMatcher(18L,
                                        "Б-188",
                                        I_2019_5_1_010101,
                                        null,
                                        toInstantAtUtcTz(2019, 6, 8, 1, 1, 1),
                                        BalanceInvoiceStatus.OVERDUE_PAID
                                ),
                                invoiceMatcher(26L,
                                        "Б-266",
                                        I_2019_5_1_010101,
                                        LocalDate.of(2019, 6, 7),
                                        toInstantAtUtcTz(2019, 6, 8, 1, 2, 3),
                                        BalanceInvoiceStatus.OVERDUE_PAID
                                ),
                                invoiceMatcher(27L,
                                        "Б-277",
                                        I_2019_5_1_010101,
                                        LocalDate.of(2019, 5, 10),
                                        toInstantAtUtcTz(2019, 6, 8, 1, 2, 3),
                                        BalanceInvoiceStatus.OVERDUE_PAID
                                ),
                                invoiceMatcher(28L,
                                        "Б-288",
                                        I_2019_5_1_010101,
                                        LocalDate.of(2019, 6, 7),
                                        toInstantAtUtcTz(2019, 6, 8, 1, 2, 3),
                                        BalanceInvoiceStatus.OVERDUE_UNPAID
                                )
                        )
                )
        );
    }

    @Test
    void test_import_whenOutdatedTable() {
        Mockito.when(ocTableNode.getAttribute("creation_time"))
                .thenReturn(
                        Optional.of(
                                YtUtilTest.stringNode("2019-05-10T06:30:09.223767Z")
                        )
                );

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () ->
                        overdraftControlImportService.loadExpiredInvoicesInfo(
                                LocalDate.of(2019, 6, 3),
                                LocalDate.of(2019, 6, 10)
                        )
        );

        assertThat(ex.getMessage(), is("//overdraft/some_table creation date must be >= then 2019-06-10 but is 2019-05-10"));
    }
}
