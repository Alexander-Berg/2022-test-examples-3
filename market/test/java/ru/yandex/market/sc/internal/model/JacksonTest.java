package ru.yandex.market.sc.internal.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
class JacksonTest {

    ObjectMapper objectMapper = ScIntModelConfiguration.OBJECT_MAPPER;

    @Test
    void courierDto() {
        testMapping(
                new CourierDto(1L, "2", "3", "4", "5", "6", null, false),
                "{" +
                        "\"id\":1," +
                        "\"name\":\"2\"," +
                        "\"carNumber\":\"3\"," +
                        "\"carDescription\":\"4\"," +
                        "\"phone\":\"5\"," +
                        "\"companyName\":\"6\"" +
                        "}"
        );
    }

    @Test
    void internalSortingCenterDto() {
        testMapping(
                new InternalSortingCenterDto(1L, "2", "3", "4", "5", "6", "7", "8", false),
                "{" +
                        "\"id\":1," +
                        "\"address\":\"2\"," +
                        "\"partnerId\":\"3\"," +
                        "\"logisticPointId\":\"4\"," +
                        "\"partnerName\":\"5\"," +
                        "\"scName\":\"6\"," +
                        "\"regionTagSuffix\":\"7\"," +
                        "\"token\":\"8\"," +
                        "\"thirdParty\":false" +
                        "}"
        );
    }

    @SneakyThrows
    private void testMapping(Object o, String expected) {
        var json = objectMapper.writeValueAsString(o);
        assertThat(json).isEqualTo(expected);
        assertThat(objectMapper.readValue(expected, o.getClass())).isEqualTo(o);
    }

}
