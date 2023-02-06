package ru.yandex.market.billing.report.payment.dao;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class PaymentReportYtDaoTest extends FunctionalTest {

    private static final LocalDate START_DATE = LocalDate.of(2022, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2023, 1, 1);

    @Autowired
    private PaymentReportYtDao paymentReportYtDao;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/billing/dictionaries/accrual/2022-01-01",
                    "//home/market/production/billing/dictionaries/accrual_correction/latest",
                    "//home/market/production/billing/dictionaries/payout_group_payment_order/latest",
                    "//home/market/production/billing/dictionaries/payout/latest",
                    "//home/market/production/billing/dictionaries/payout_correction/latest",
                    "//home/market/production/mstat/dictionaries/mbi_bank_order/latest",
                    "//home/market/production/mstat/dictionaries/mbi_bank_order_item/latest",
                    "//home/market/production/mstat/dwh/ods/mbi/bank_order/bank_order",
                    "//home/market/production/mstat/dwh/ods/mbi/bank_order_item/bank_order_item",
                    "//home/market/production/mbi/billing/oracle-export-tm/market_billing_cpa_order",
                    "//home/market/production/mbi/billing/oracle-export-tm/market_billing_cpa_order_item",
                    "//home/market/production/mbi/dictionaries/partner_contract/latest",
                    "//home/market/production/mstat/oebs/sales_daily_market",
                    "//home/market/production/mbi/reports/old_trust_transaction",
                    "//home/market/production/mbi/reports/trust_transaction",
                    "//home/market/production/billing/tlog/payouts/expenses/2022-01-01",
                    "//home/market/production/billing/reports/act_data"
            },
            csv = "PaymentReportYtDaoTest.preparePaymentReportTableQuery.yql.csv",
            expectedCsv = "PaymentReportYtDaoTest.preparePaymentReportTableQuery.result.yql.csv",
            yqlMock = "PaymentReportYtDaoTest.preparePaymentReportTableQuery.yql.mock"
    )
    public void preparePaymentReportTableQueryTest() {
        paymentReportYtDao.preparePaymentReportTable(START_DATE, END_DATE, true);
    }
}
