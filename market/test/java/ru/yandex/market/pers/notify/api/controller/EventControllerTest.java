package ru.yandex.market.pers.notify.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.push.MobileAppInfoDAO;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EventControllerTest extends MarketUtilsMockedDbTest {
    private static final long USER_ID = 12345;
    private static final String EMAIL = "foo@bar.buzz";

    private static final String MAIL_EVENT_JSON = "{\n" +
        "  \"uid\": 12345,\n" +
        "  \"email\": \"foo@bar.buzz\",\n" +
        "  \"notificationSubtype\": \"SUCCESSFUL_MODEL_GRADE\"\n" +
        "}";

    private static final String BAD_MAIL_EVENT_JSON = "{\n" +
        "  \"uid\": 12345,\n" +
        "  \"email\": \"foo\",\n" +
        "  \"notificationSubtype\": \"SUCCESSFUL_MODEL_GRADE\"\n" +
        "}";


    private static final String PUSH_EVENT_JSON = "{\n" +
        "  \"uid\": 12345,\n" +
        "  \"notificationSubtype\": \"PUSH_ORDER_CANCELLED\"\n" +
        "}";


    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;


    @Test
    public void mailEventForExistingUserMustBeSuccess() throws Exception {
        createUser();
        mockMvc.perform(post("/event/add").content(MAIL_EVENT_JSON).contentType(MediaType.APPLICATION_JSON)
        ).andDo(print()).andExpect(status().is2xxSuccessful());
    }

    @Test
    public void mailEventWithBadEmailMustBeBadRequest() throws Exception {
        createUser();
        mockMvc.perform(post("/event/add").content(BAD_MAIL_EVENT_JSON).contentType(MediaType.APPLICATION_JSON)
        ).andDo(print()).andExpect(status().isBadRequest())
            .andExpect(content().json(
                toJson(new Error("INVALID_FORMAT", "source.email is invalid: foo", 400)))
            );
    }

    @Test
    public void pushForExistingUserWithNoMobileAppMustBeSuccess() throws Exception {
        createUser();
        mockMvc.perform(post("/event/add").content(PUSH_EVENT_JSON).contentType(MediaType.APPLICATION_JSON)
        ).andDo(print()).andExpect(status().is2xxSuccessful());
    }


    private void createUser() {
        blackBoxPassportService.doReturn(USER_ID, EMAIL);

        mobileAppInfoDAO.add(new MobileAppInfo(USER_ID, "uuid", "app_name", "yandexUid", "pushToken",
            MobilePlatform.IPHONE, false, new Date(), 213L, 1L, true));
    }


}
