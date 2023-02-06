package ru.yandex.travel.commons.logging.masking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.Data;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static ru.yandex.travel.commons.logging.masking.MaskingJacksonAnnotationIntrospector.ConstantStringDataConverter.DEFAULT_STRING;

public class AnnotationMaskingTest {

    @Test
    public void thePersonalDataGetMasked() throws JsonProcessingException {
        String s = LogMaskingConverter.getObjectMapperForLogEvents().writeValueAsString(new PersonalDataDTO());
        PersonalDataDTO deserialized = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                .readValue(s, PersonalDataDTO.class);
        assertEquals(deserialized.name, DEFAULT_STRING);
        assertEquals(deserialized.phone, DEFAULT_STRING);

        assertArrayEquals(deserialized.binaryData, new byte[0]);
        assertEquals(deserialized.binaryData2, "");
    }


    @Data
    public static class PersonalDataDTO {
        @PersonalData
        private String name = "Alexandr Pushkin";
        @PersonalData
        private String phone = "212-85-06";
        private String registrationDate = "01-01-2020";
        @BinaryData
        private byte[] binaryData = new byte[10_000];
        @BinaryData
        private String binaryData2 = "PDF PDF PDF";
    }
}
