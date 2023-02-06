package ru.yandex.market.volva.converter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import ru.yandex.market.volva.domain.PaymentEvent;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class PaymentConverterTest {

    @DataPoints("currency")
    public static List<String> currencies = List.of("RUR", "KZT", "USD", "EUR");
    @DataPoints
    public static int[] amounts = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};

    private static final String PUID = "123456";
    private static final String CARD_ID = "16ab876cd";
    private static final Map<String, Integer> MINIMAL_AMOUNTS = Map.of(
        "RUR", 4,
        "KZT", 8
    );
    private static final int DEFAULT_MINIMAL_AMOUNT = 5;
    private static final PaymentConverter PAYMENT_CONVERTER = new PaymentConverter(MINIMAL_AMOUNTS, DEFAULT_MINIMAL_AMOUNT);

    @Theory
    public void testSumLessMinimal(String currency, int amount) {
        assumeThat(currency, isIn(MINIMAL_AMOUNTS.keySet()));
        assumeThat(amount, lessThan(MINIMAL_AMOUNTS.get(currency)));

        var paymentEvent = new PaymentEvent(PUID, CARD_ID, BigDecimal.valueOf(amount), currency);
        assertThat(PAYMENT_CONVERTER.extractEdges(paymentEvent))
            .isEmpty();
    }

    @Theory
    public void testSumGreaterMinimal(String currency, int amount) {
        assumeThat(currency, isIn(MINIMAL_AMOUNTS.keySet()));
        assumeThat(amount, greaterThanOrEqualTo(MINIMAL_AMOUNTS.get(currency)));

        var paymentEvent = new PaymentEvent(PUID, CARD_ID, BigDecimal.valueOf(amount), currency);
        assertThat(PAYMENT_CONVERTER.extractEdges(paymentEvent))
            .flatExtracting(x -> x.getAffectedNodesS().collect(toList()))
            .containsExactlyInAnyOrder(new Node(PUID, IdType.PUID), new Node(CARD_ID, IdType.CARD));
    }

    @Theory
    public void testSumLessDefaultMinimal(String currency, int amount) {
        assumeThat(currency, not(isIn(MINIMAL_AMOUNTS.keySet())));
        assumeThat(amount, lessThan(DEFAULT_MINIMAL_AMOUNT));

        var paymentEvent = new PaymentEvent(PUID, CARD_ID, BigDecimal.valueOf(amount), currency);
        assertThat(PAYMENT_CONVERTER.extractEdges(paymentEvent))
            .isEmpty();
    }

    @Theory
    public void testSumGreaterDefaultMinimal(String currency, int amount) {
        assumeThat(currency, not(isIn(MINIMAL_AMOUNTS.keySet())));
        assumeThat(amount, greaterThanOrEqualTo(DEFAULT_MINIMAL_AMOUNT));

        var paymentEvent = new PaymentEvent(PUID, CARD_ID, BigDecimal.valueOf(amount), currency);
        assertThat(PAYMENT_CONVERTER.extractEdges(paymentEvent))
            .flatExtracting(x -> x.getAffectedNodesS().collect(toList()))
            .containsExactlyInAnyOrder(new Node(PUID, IdType.PUID), new Node(CARD_ID, IdType.CARD));
    }
}