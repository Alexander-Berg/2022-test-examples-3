package ru.yandex.market.logistics.cs.dbqueue.servicecounter;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.cs.AbstractTest;

class ServiceDeliveryDescriptorTest extends AbstractTest {

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @SneakyThrows
    void testCompatibleSerializationDeserialization() {
        var serviceDeliveryDescriptor = new ServiceDeliveryDescriptor(123L, LocalDate.EPOCH, 999, 100500);
        String json = mapper.writeValueAsString(serviceDeliveryDescriptor);
        softly.assertThat(mapper.readValue(json, ServiceDeliveryDescriptor.class))
            .isEqualTo(serviceDeliveryDescriptor);
    }
}
