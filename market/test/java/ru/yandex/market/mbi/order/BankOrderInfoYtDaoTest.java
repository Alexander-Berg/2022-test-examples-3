package ru.yandex.market.mbi.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.order.payment.BankOrderInfo;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.assertj.core.api.Assertions.assertThat;

public class BankOrderInfoYtDaoTest extends FunctionalTest {

    private static final long TEST_SUPPLIER = 1L;
    private static final LocalDate START_DATE = LocalDate.of(2022, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2023, 1, 1);

    @Autowired
    private BankOrderInfoYtDao bankOrderInfoYtDao;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mbi/reports/agg_payment_report"
            },
            csv = "BankOrderInfoYtDaoTest.getBankOrderInfoByDate.yql.csv",
            yqlMock = "BankOrderInfoYtDaoTest.getBankOrderInfoByDate.yql.mock"
    )
    public void getBankOrderInfoByDateTest() {
        Map<Long, List<BankOrderInfo>> bankOrderInfoMap = bankOrderInfoYtDao.getBankOrderInfoByDate(
                Set.of(TEST_SUPPLIER), START_DATE, END_DATE);

        assertThat(bankOrderInfoMap.keySet()).containsOnly(TEST_SUPPLIER);

        assertThat(bankOrderInfoMap.get(TEST_SUPPLIER)
                .stream()
                .map(BankOrderInfo::getTrustId))
                .containsOnly("abcd-abcd-abcd-abc3", "55684041");
    }
}
