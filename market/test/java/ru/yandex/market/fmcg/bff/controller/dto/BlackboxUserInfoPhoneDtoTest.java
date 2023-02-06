package ru.yandex.market.fmcg.bff.controller.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class BlackboxUserInfoPhoneDtoTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testBlackboxUserInfoPhoneDtoHasPhonesDeserialization() throws IOException {
        BlackboxUserInfoPhoneDto dto = objectMapper.readValue(
            "{\"users\":[{\"id\":\"111\",\"uid\":{\"value\":\"111\",\"lite\":false,\"hosted\":false}," +
                "\"login\":\"test\",\"have_password\":true,\"have_hint\":true," +
                "\"karma\":{\"value\":0},\"karma_status\":{\"value\":0}," +
                "\"phones\":[{\"id\":\"2288\",\"attributes\":{\"102\":\"+71111111111\",\"107\":\"1\",\"108\":\"1\"}}" +
                ",{\"id\":\"3399\",\"attributes\":{\"102\":\"+72222222222\"}}]}]}",
            BlackboxUserInfoPhoneDto.class);
        Assertions.assertNotNull(dto.getUsers());
        Assertions.assertEquals(1, dto.getUsers().size());
        Assertions.assertNotNull(dto.getUsers().get(0).getPhones());
        Assertions.assertEquals(2, dto.getUsers().get(0).getPhones().size());
    }

    @Test
    public void testBlackboxUserInfoPhoneDtoEmptyPhonesDeserialization() throws IOException {
        BlackboxUserInfoPhoneDto dto = objectMapper.readValue(
            "{\"users\":[{\"id\":\"1111\",\"uid\":{\"value\":\"1111\",\"lite\":false,\"hosted\":false}," +
                "\"login\":\"mk-test22\",\"have_password\":true,\"have_hint\":true," +
                "\"karma\":{\"value\":0},\"karma_status\":{\"value\":0},\"phones\":[]}]}",
            BlackboxUserInfoPhoneDto.class
        );
        Assertions.assertNotNull(dto.getUsers());
        Assertions.assertEquals(1, dto.getUsers().size());
        Assertions.assertNotNull(dto.getUsers().get(0).getPhones());
        Assertions.assertEquals(0, dto.getUsers().get(0).getPhones().size());
    }

    @Test
    public void testBlackboxUserInfoPhoneDtoNullPhonesDeserialization() throws IOException {
        BlackboxUserInfoPhoneDto dto = objectMapper.readValue(
            "{\"users\":[{\"id\":\"111234\",\"uid\":{},\"karma\":{\"value\":0},\"karma_status\":{\"value\":0}}]}",
            BlackboxUserInfoPhoneDto.class
        );
        Assertions.assertNotNull(dto.getUsers());
        Assertions.assertEquals(1, dto.getUsers().size());
        Assertions.assertNull(dto.getUsers().get(0).getPhones());
    }
}
