package ru.yandex.market.api.user.profile;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.util.httpclient.clients.LoyaltyTestClient;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by fettsery on 23.05.18.
 */
public class ProfileControllerTest extends BaseTest {

    @Inject
    LoyaltyTestClient loyaltyTestClient;

    @Inject
    ProfileController profileController;

    private static final long USER_ID = 123;
    private static final String UUID = "12345678901234567890123456789012";
    private static final int REGION_ID = 213;

    @Test
    public void testProfileWithYandexPlus() {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_1.json");

        Profile profile = doCall();

        assertTrue(profile.getHasYandexPlus());
    }

    @Test
    public void testProfileWithoutYandexPlus() {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_2.json");

        Profile profile = doCall();

        assertFalse(profile.getHasYandexPlus());
    }
    @Test
    public void testProfileWithSberPrime() {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_6.json");

        Profile profile = doCall();

        assertTrue(profile.getHasSberPrime());
    }

    @Test
    public void testProfileWithoutSberPrime() {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_5.json");

        Profile profile = doCall();

        assertFalse(profile.getHasSberPrime());
    }

    @Test
    public void testProfileIsYandexEmployee() {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_3.json");

        Profile profile = doCall();

        assertTrue(profile.isYandexEmployee());
    }

    @Test
    public void testProfileIsNotYandexEmployee() {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_4.json");

        Profile profile = doCall();

        assertFalse(profile.isYandexEmployee());
    }

    @Test
    public void testProfileIfNothingFound() {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_5.json");

        Profile profile = doCall();

        assertFalse(profile.isYandexEmployee());
        assertFalse(profile.getHasYandexPlus());
    }

    private Profile doCall() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("format", "json");

        ModelAndView profileModel = profileController.getProfile(request, new User(new OauthUser(USER_ID), null, new Uuid(UUID), null)).waitResult();

        Map modelMap = (Map) profileModel.getModel().get("json");
        assertNotNull(modelMap);
        assertEquals(Collections.singleton("profile"), modelMap.keySet());

        return (Profile)modelMap.get("profile");
    }
}
