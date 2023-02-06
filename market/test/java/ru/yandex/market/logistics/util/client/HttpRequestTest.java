package ru.yandex.market.logistics.util.client;

import java.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestTest {

    @Test
    void addMixedParameters() {
        HttpRequest<?> request = new HttpRequest<>(HttpMethod.GET)
            .addMixedParameters(ImmutableMap.of(
                "single", 20L,
                "collection", ImmutableList.of(30L, LocalDate.of(2021, 1, 1))
            ));

        assertThat(request.getParameters())
            .containsEntry("single", ImmutableList.of("20"))
            .containsEntry("collection", ImmutableList.of("30", "2021-01-01"));
    }

}
