package ru.yandex.market.tpl.api.controller.api;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.avatarnica.AvatarnicaClient;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_={@Autowired})
public class PhotoControllerTest extends BaseApiIntTest {
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final AvatarnicaClient avatarnicaClient;

    @BeforeEach
    void setUpThis() {
        testUserHelper.findOrCreateUser(UID);
        Mockito.when(avatarnicaClient.uploadImageData(Mockito.any()).getMeta().getOrigSizeBytes()).thenReturn(3L);
    }

    @Test
    void shouldSavePhotos() throws Exception {
        byte[] data = new byte[3];

        String content = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/photo/upload")
                .part(new MockPart("file1", "a.jpg", data))
                .part(new MockPart("file2", "b.jpg", data))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> result = tplObjectMapper.readValue(content, new TypeReference<Map<String, String>>() {});

        Assertions.assertThat(result)
                .hasSize(2)
                .hasEntrySatisfying("file1", url -> Assertions.assertThat(url).isNotEmpty())
                .hasEntrySatisfying("file2", url -> Assertions.assertThat(url).isNotEmpty());
    }
}
