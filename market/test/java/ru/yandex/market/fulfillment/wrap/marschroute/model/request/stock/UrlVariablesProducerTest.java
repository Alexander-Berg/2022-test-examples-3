package ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.fulfillment.wrap.marschroute.model.request.UrlQueryParametersProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

public abstract class UrlVariablesProducerTest {

    @MethodSource("data")
    @ParameterizedTest(name = " [" + INDEX_PLACEHOLDER + "] {0}")
    void testPaginationToUrlVariablesTransformation(
        String name,
        UrlQueryParametersProducer producer,
        MultiValueMap<String, String> expectedUrlVariables
    ) {
        MultiValueMap<String, String> actualUrlVariables = producer.produce();

        assertThat(actualUrlVariables)
            .as("Comparing actual url variables with expected url variables")
            .isEqualTo(expectedUrlVariables);
    }

    public static <K, V> MultiValueMap<K, V> wrapInMultiValueMap(Map<K, V> map) {
        LinkedMultiValueMap<K, V> multiValueMap = new LinkedMultiValueMap<>();
        map.forEach(multiValueMap::add);

        return multiValueMap;
    }
}
