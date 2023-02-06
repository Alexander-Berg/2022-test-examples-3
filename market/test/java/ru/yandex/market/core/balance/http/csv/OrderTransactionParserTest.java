package ru.yandex.market.core.balance.http.csv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.order.payment.MarketPaymentsStat;
import ru.yandex.market.mbi.util.FileUtils;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.common.util.csv.CSVReader.SEMICOLON;
import static ru.yandex.market.core.balance.http.BalanceCsvService.BALANCE_TIMESTAMP_FORMAT;

/**
 * Unit тесты для {@link OrderTransactionParser}.
 *
 * @author avetokhin 29/06/17.
 */
class OrderTransactionParserTest {
    private static final String NO_REFUND_ID = null;
    private static final String NO_PAYMENT_ID = null;

    @DisplayName("Парсер ответа баланса")
    @Test
    void testParse() throws IOException, ParseException {
        final OrderTransactionParser orderTransactionParser = new OrderTransactionParser();

        final List<MarketPaymentsStat> actual
                = orderTransactionParser.readCsv(excludeComments("/ru/yandex/market/core/balance/http/csv/transactions.csv"), SEMICOLON);
        assertThat(
                actual,
                contains(
                        ImmutableList.of(
                                //прямые транзакции платежи картой
                                transaction("20170617021240",
                                        "59401bc8b5a2d42b0aa92210",
                                        NO_REFUND_ID,
                                        "62741",
                                        "20170614000000",
                                        "20170614000000",
                                        286191,
                                        BigDecimal.valueOf(5)
                                ),
                                transaction("20170617021241",
                                        "59401bc8b5a2d42b0aa92211",
                                        NO_REFUND_ID,
                                        "62742",
                                        null,
                                        "20170614000000",
                                        286191,
                                        null
                                ),
                                //прямая транзакция платеж наличными
                                transaction("20181225105709",
                                        "5c1f4f19fbacea2831dca3dd",
                                        NO_REFUND_ID,
                                        "145767",
                                        "20181224000000",
                                        "20181223120218",
                                        370078,
                                        BigDecimal.valueOf(55.8)
                                ),
                                //прямая транзакция субсидии
                                transaction("20181225105740",
                                        "5c1fb52a03c378d4c75493bc",
                                        NO_REFUND_ID,
                                        "145753",
                                        "20181224000000",
                                        "20181223191746",
                                        407087,
                                        BigDecimal.ZERO
                                ),
                                //обратная транзакция для субсидий
                                transaction("20181225105738",
                                        NO_PAYMENT_ID,
                                        "5c208aa1dff13b5334e50aae",
                                        "145798",
                                        "20181224000000",
                                        "20181224102833",
                                        395489,
                                        BigDecimal.ZERO
                                ),
                                //обратная транзакция - компенсационный платеж на удержание суммы со счета магазина
                                transaction("20181228111428",
                                        NO_PAYMENT_ID,
                                        "5c238c22f988016691fee41b",
                                        "150951",
                                        "20181227000000",
                                        "20181226171146",
                                        451510,
                                        BigDecimal.ZERO
                                )
                        )
                )
        );
    }

    private Matcher<MarketPaymentsStat> transaction(
            String tranTime,
            @Nullable String paymentId,
            @Nullable String refundId,
            String bankOrderId,
            @Nullable String bankOrderTime,
            String handlingTime,
            long contractId,
            @Nullable BigDecimal agencyCommissionValue
    ) {
        return MbiMatchers.<MarketPaymentsStat>newAllOfBuilder()
                .add(MarketPaymentsStat::getTrantime, LocalDateTime.parse(tranTime, BALANCE_TIMESTAMP_FORMAT), "trantime")
                .add(MarketPaymentsStat::getTrustRefundId, refundId, "refundId")
                .add(MarketPaymentsStat::getTrustPaymentId, paymentId, "paymentId")
                .add(MarketPaymentsStat::getBankOrderId, bankOrderId, "bankOrderId")
                .add(MarketPaymentsStat::getContractId, contractId, "contractId")
                .add(MarketPaymentsStat::getBankOrderTime, bankOrderTime == null ?
                        null :
                        LocalDateTime.parse(bankOrderTime, BALANCE_TIMESTAMP_FORMAT), "bankOrderTime"
                )
                .add(MarketPaymentsStat::getHandlingTime, LocalDateTime.parse(handlingTime, BALANCE_TIMESTAMP_FORMAT), "hanldingTime")
                .add(MarketPaymentsStat::getAgencyCommissionValue, agencyCommissionValue, "agencyCommission")
                .build();
    }

    private InputStream excludeComments(String filename) throws IOException {
        return new ByteArrayInputStream(
                FileUtils.readFilteredLinesFromFile(
                        filename,
                        line -> !line.startsWith("#"))
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}

