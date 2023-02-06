package ru.yandex.market.delivery.transport_manager.model.dto.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CargoUnitIdWithDirectionTest {
    public static final String WITH_TARGET_POINT = "\"100000172/100000145/DRP0001\"";
    public static final String WITHOUT_TARGET_POINT = "\"100000172/DRP0001\"";
    public static final CargoUnitIdWithDirection DTO_WITH_TARGET_POINT = new CargoUnitIdWithDirection(
        "DRP0001",
        100000172L,
        100000145L
    );
    public static final CargoUnitIdWithDirection DTO_WITHOUT_TARGET_POINT = new CargoUnitIdWithDirection(
        "DRP0001",
        100000172L,
        null
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SneakyThrows
    void testToJson() {
        assertThat(objectMapper.writeValueAsString(DTO_WITH_TARGET_POINT))
            .isEqualTo(WITH_TARGET_POINT);
    }

    @Test
    @SneakyThrows
    void testToJsonNullTargetPoint() {
        assertThat(objectMapper.writeValueAsString(DTO_WITHOUT_TARGET_POINT))
            .isEqualTo(WITHOUT_TARGET_POINT);
    }

    @Test
    @SneakyThrows
    void testParseJson() {
        assertThat(objectMapper.readValue(WITH_TARGET_POINT, CargoUnitIdWithDirection.class))
            .isEqualTo(DTO_WITH_TARGET_POINT);
    }

    @Test
    @SneakyThrows
    void testParseJsonNullTargetPoint() {
        assertThat(objectMapper.readValue(WITHOUT_TARGET_POINT, CargoUnitIdWithDirection.class))
            .isEqualTo(DTO_WITHOUT_TARGET_POINT);
    }
}
