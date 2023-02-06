package ru.yandex.market.pers.qa.controller.api;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.author.client.PersAuthorClient;
import ru.yandex.market.pers.qa.client.model.UserType;
import ru.yandex.market.pers.qa.controller.ControllerTest;
import ru.yandex.market.pers.qa.controller.dto.ResultDto;
import ru.yandex.market.pers.qa.model.QaEntityType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ComplaintControllerVideoTest extends ComplaintControllerTest {

    @Autowired
    private PersAuthorClient persAuthorClient;
    private long VIDEO_ID = 1344;

    @BeforeEach
    public void setUp() {
        qaJdbcTemplate.update("insert into qa.complaint_reason(id, name) values (?, ?)", REASON, REASON_NAME);
    }

    @Test
    public void testCreateVideoComplaintUid() throws Exception {
        final String text = UUID.randomUUID().toString();
        boolean isOk = createVideoComplaint(UserType.UID, VIDEO_ID, REASON, text);

        assertFalse(isOk);
        checkComplaint(UserType.UID, ControllerTest.UID_STR, QaEntityType.VIDEO, VIDEO_ID);
        verify(persAuthorClient, times(1)).sendComplaint(
            eq(VIDEO_ID), eq(UserType.UID.getDescription()), eq(UID_STR), eq(REASON_NAME + " " + text),
            any(Instant.class));
    }

    @Test
    public void testCreateVideoComplaintYandexUid() throws Exception {
        final String text = UUID.randomUUID().toString();

        boolean isOk = createVideoComplaint(UserType.YANDEXUID, VIDEO_ID, REASON, text);

        assertFalse(isOk);
        checkComplaint(UserType.YANDEXUID, ControllerTest.YANDEXUID, QaEntityType.VIDEO, VIDEO_ID);
        verify(persAuthorClient, times(1)).sendComplaint(
            eq(VIDEO_ID), eq(UserType.YANDEXUID.getDescription()), eq(YANDEXUID), eq(REASON_NAME + " " + text),
            any(Instant.class));
    }

    private boolean createVideoComplaint(UserType userType,
                                        long videoId,
                                        int reasonId,
                                        String text) throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = post(String
            .format("/complaint/%s/%s/video/%s",
                userType.getDescription(),
                UserType.UID == userType ? UID : YANDEXUID,
                videoId
            ))
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format(COMPLAIN_BODY, reasonId, text))
            .accept(MediaType.APPLICATION_JSON);

        return objectMapper.readValue(invokeAndRetrieveResponse(requestBuilder, status().is2xxSuccessful()),
            ResultDto.class).getResult();
    }

}
