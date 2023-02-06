package ru.yandex.market.checkout.checkouter.shop;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ShopControllerScheduleErrorTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 123456789L;

    @Autowired
    private TestSerializationService testSerializationService;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                Arrays.asList(
                        builder().withDay(1).withStart(10L).withEnd(1430L).build(),
                        builder().withDay(1).withStart(10L).withEnd(1430L).build()
                ),
                Arrays.asList(
                        builder().withDay(1).withStart(-1L).withEnd(1430L).build(),
                        builder().withDay(2).withStart(-10L).withEnd(1430L).build()
                ),
                Arrays.asList(
                        builder().withDay(1).withStart(0L).withEnd(-60L).build(),
                        builder().withDay(2).withStart(0L).withEnd(-1L).build()
                ),
                Collections.singletonList(
                        builder().withDay(1).withStart(1100L).withEnd(100L).build()
                ),
                Arrays.asList(
                        builder().withDay(1).withStart(0L).withEnd(1430L).build(),
                        builder().withDay(2).withStart(10L).withEnd(null).build()
                ),
                Arrays.asList(
                        builder().withDay(1).withStart(0L).withEnd(1430L).build(),
                        builder().withDay(null).withStart(10L).withEnd(1430L).build()
                )
        )
                .map(schedule -> new Object[]{schedule})
                .collect(Collectors.toList()).stream().map(Arguments::of);
    }

    private static Builder builder() {
        return new Builder();
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testErrorOnPushShopSchedule(List<Map<String, Object>> schedule) throws Exception {
        mockMvc.perform(
                put("/shop/{shopId}/schedule/", SHOP_ID)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(testSerializationService.serializeCheckouterObject(schedule))
        ).andExpect(status().isBadRequest());
    }

    static class Builder {

        private final Map<String, Object> scheduleLine;

        Builder() {
            this.scheduleLine = new HashMap<>();
        }

        public Builder withDay(Integer day) {
            if (day != null) {
                scheduleLine.put("day", day);
            }
            return this;
        }

        public Builder withStart(Long start) {
            if (start != null) {
                scheduleLine.put("start", start);
            }
            return this;
        }

        public Builder withEnd(Long end) {
            if (end != null) {
                scheduleLine.put("end", end);
            }
            return this;
        }

        public Map<String, Object> build() {
            return scheduleLine;
        }
    }
}
