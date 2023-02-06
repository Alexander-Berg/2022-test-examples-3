package ru.yandex.market.core.matchers.fulfillment.report.matchers;

import java.math.BigDecimal;

import org.hamcrest.Matcher;

import ru.yandex.market.core.fulfillment.report.excel.jxls.JxlsOrderReportTransactionModel;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.allOf;

/**
 * Матчеры для {@link JxlsOrderReportTransactionModel}.
 *
 * @author vbudnev
 */
public class JxlsOrderReportTransactionModelMatcher {

    public static Matcher<JxlsOrderReportTransactionModel> hasAmount(BigDecimal expectedValue) {
        return MbiMatchers.<JxlsOrderReportTransactionModel>newAllOfBuilder()
                .add(JxlsOrderReportTransactionModel::getAmount, expectedValue, "amount")
                .build();
    }

    public static Matcher<JxlsOrderReportTransactionModel> hasBankOrderId(String expectedValue) {
        return MbiMatchers.<JxlsOrderReportTransactionModel>newAllOfBuilder()
                .add(JxlsOrderReportTransactionModel::getBankOrderId, expectedValue, "bankOrderId")
                .build();
    }

    public static Matcher<JxlsOrderReportTransactionModel> hasBankOrderTime(String expectedValue) {
        return MbiMatchers.<JxlsOrderReportTransactionModel>newAllOfBuilder()
                .add(JxlsOrderReportTransactionModel::getBankOrderTime, expectedValue, "bankOrderTime")
                .build();
    }

    public static Matcher<JxlsOrderReportTransactionModel> hasPaymentIdentity(String expectedValue) {
        return MbiMatchers.<JxlsOrderReportTransactionModel>newAllOfBuilder()
                .add(JxlsOrderReportTransactionModel::getPaymentIdentity, expectedValue, "paymentIdentity")
                .build();
    }

    public static Matcher<JxlsOrderReportTransactionModel> hasDateHandlingTime(String expectedValue) {
        return MbiMatchers.<JxlsOrderReportTransactionModel>newAllOfBuilder()
                .add(JxlsOrderReportTransactionModel::getDateHandlingTime, expectedValue, "dateHandlingTime")
                .build();
    }


    /**
     * Проверка то строка с в колонке "... транзакция" пустая
     */
    public static Matcher<JxlsOrderReportTransactionModel> transactionNullMatcher() {
        return allOf(
                hasAmount(null),
                hasBankOrderId(null),
                hasBankOrderTime(null),
                hasPaymentIdentity(null),
                hasDateHandlingTime(null)
        );
    }

}
