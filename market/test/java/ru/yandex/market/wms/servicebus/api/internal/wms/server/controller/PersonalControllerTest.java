package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.personal.PersonalClient;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.enums.PersonalDataType;
import ru.yandex.market.personal.model.PersonalResponseItem;
import ru.yandex.market.wms.servicebus.IntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@Slf4j
public class PersonalControllerTest extends IntegrationTest {

    @MockBean
    @Autowired
    private PersonalClient personalClient;

    @Autowired
    protected ObjectMapper objectMapper;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(personalClient);
    }

    @Test
    public void shouldDecodeId() throws Exception {
        List<Pair<String, PersonalDataType>> personalClientRequest = Arrays.asList(
                Pair.of("mock1", PersonalDataType.ADDRESS),
                Pair.of("ca7fe7f322ea45479c660dea3fd2b886", PersonalDataType.FULL_NAME),
                Pair.of("mock2", PersonalDataType.ADDRESS),
                Pair.of("7d0b4b1bd3b2461d9103a99ff88b47e5", PersonalDataType.FULL_NAME)
                );

        FullName fullName1 = new FullName();
        fullName1.setSurname("Пупкин");
        fullName1.setForename("Василий");
        FullName fullName2 = new FullName();
        fullName2.setSurname("Кузнецов");
        fullName2.setForename("Алексей");

        List<PersonalResponseItem> personalClientResponse = Arrays.asList(
                new PersonalResponseItem("ca7fe7f322ea45479c660dea3fd2b886", PersonalDataType.FULL_NAME, fullName1),
                new PersonalResponseItem("7d0b4b1bd3b2461d9103a99ff88b47e5", PersonalDataType.FULL_NAME, fullName2)
        );

        ArgumentCaptor<List<Pair<String, PersonalDataType>>> request = ArgumentCaptor.forClass(List.class);

        Mockito.when(personalClient.multiTypesRetrieve(any())).thenReturn(personalClientResponse);

        mockMvc.perform(post("/api/wms/personal/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/personal/happy/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/personal/happy/response.json")))
                .andReturn();
        Mockito.verify(personalClient).multiTypesRetrieve(request.capture());

        Assertions.assertEquals(personalClientRequest, request.getValue(), "Request to WMS API is mismatched");
    }
}
