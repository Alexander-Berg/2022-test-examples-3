package ru.yandex.market.pers.notify.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class TestAddressControllerTest extends MarketUtilsMockedDbTest {
    @Autowired
    private MockMvc mvc;

    @Test
    public void testEmails() throws Exception {
        postEmail("foobar@example.com");
        postEmail("aa@bb.com");
        getAndCheckTestEmails("[\"aa@bb.com\", \"foobar@example.com\"]");
        postEmail("aa@bb.com");
        getAndCheckTestEmails("[\"aa@bb.com\", \"foobar@example.com\"]");
        deleteEmail("foobar@example.com");
        getAndCheckTestEmails("[\"aa@bb.com\"]");
        getAndCheckTestUUIDs("[]");
    }

    @Test
    public void testUUIDs() throws Exception {
        postUUID("qwerty123");
        postUUID("abcdef");
        getAndCheckTestUUIDs("[\"abcdef\", \"qwerty123\"]");
        postUUID("abcdef");
        getAndCheckTestUUIDs("[\"abcdef\", \"qwerty123\"]");
        deleteUUID("abcdef");
        getAndCheckTestUUIDs("[\"qwerty123\"]");
        getAndCheckTestEmails("[]");
    }

    @Test
    public void testInvalidUrl() throws Exception {
        mvc.perform(delete("/test-address/email"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json(toJson(new Error("INVALID_FORMAT", "Required String parameter 'email' is not present", 400))));
    }

    private void deleteEmail(String email) throws Exception {
        mvc.perform(delete("/test-address/email").param("email", email))
            .andExpect(status().isOk());
    }

    private void postEmail(String email) throws Exception {
        mvc.perform(post("/test-address/email").content(email))
            .andExpect(status().isOk());
    }

    private void getAndCheckTestEmails(String expectedResult) throws Exception {
        mvc.perform(get("/test-address/email"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(expectedResult));
    }

    private void deleteUUID(String uuid) throws Exception {
        mvc.perform(delete("/test-address/uuid").param("uuid", uuid))
            .andExpect(status().isOk());
    }

    private void postUUID(String uuid) throws Exception {
        mvc.perform(post("/test-address/uuid").content(uuid))
            .andExpect(status().isOk());
    }

    private void getAndCheckTestUUIDs(String expectedResult) throws Exception {
        mvc.perform(get("/test-address/uuid"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(expectedResult));
    }

}
