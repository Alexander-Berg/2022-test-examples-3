package ru.yandex.market.logistics.management.entity.serialization;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.logbroker.BusinessWarehouseSnapshotDto;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.type.PhoneType;

public class BusinessWarehouseSnapshotDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializationTest() throws IOException {
        BusinessWarehouseSnapshotDto dto = new BusinessWarehouseSnapshotDto();
        dto.setPhones(Set.of(getPhoneDto()));

        String dtoAsString = objectMapper.writeValueAsString(dto);
        BusinessWarehouseSnapshotDto dtoNew = objectMapper.readValue(dtoAsString, BusinessWarehouseSnapshotDto.class);

        Assertions.assertThat(dtoNew)
            .usingRecursiveComparison()
            .isEqualTo(dtoNew);
    }

    @Nonnull
    private Phone getPhoneDto() {
        return Phone.newBuilder()
            .number("+78005553535")
            .internalNumber("")
            .comment("number")
            .type(PhoneType.PRIMARY)
            .build();
    }
}
