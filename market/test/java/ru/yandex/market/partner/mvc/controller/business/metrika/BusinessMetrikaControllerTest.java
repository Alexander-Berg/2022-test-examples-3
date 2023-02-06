package ru.yandex.market.partner.mvc.controller.business.metrika;

import java.util.UUID;
import java.util.function.Supplier;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тесты {@link BusinessMetrikaController}
 */
@DbUnitDataSet(before = "csv/BusinessMetrikaControllerTest.before.csv")
public class BusinessMetrikaControllerTest extends FunctionalTest {

    private static final long BUSINESS_ID = 4444;
    private static final String LOGIN = "cherdakov";
    private static final long UID = 123456789;
    private static final String GOOD_LOGIN = "yndx-cherdakov";
    private static final long GOOD_UID = 887308675;
    private static final String INVITATION_ID = "9c7a68cc-7bd8-4ea1-bfb1-bf8b395ab78c";

    @Autowired
    PassportService passportService;
    @Autowired
    Supplier<UUID> uuidSupplier;
    @Autowired
    private MockWebServer metrikaIntApiMockWebServer;

    @BeforeEach
    void beforeEach() {
        UserInfo goodUser = new UserInfo(GOOD_UID, GOOD_LOGIN, GOOD_LOGIN + "@yandex.ru", GOOD_LOGIN);
        when(passportService.getUserInfo(eq(GOOD_LOGIN))).thenReturn(goodUser);
        when(passportService.getUserInfo(eq(GOOD_UID))).thenReturn(goodUser);
        UserInfo user = new UserInfo(UID, LOGIN, LOGIN + "@yandex.ru", LOGIN);
        when(passportService.getUserInfo(eq(LOGIN))).thenReturn(user);
        when(passportService.getUserInfo(eq(UID))).thenReturn(user);
        when(uuidSupplier.get()).thenReturn(UUID.fromString("9c7a68cc-7bd8-4ea1-bfb1-bf8b395ab78c"));
    }


    @Test
    void getBusinessMetrikaInfoTest() {
        var url = UriComponentsBuilder.fromUriString(baseUrl + "/businesses/{business_id}/metrika")
                .buildAndExpand(BUSINESS_ID)
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), getClass(),
                "json/BusinessMetrikaControllerTest.getBusinessMetrikaInfoTest.json");
    }

    @Test
    @DbUnitDataSet(after = "csv/BusinessMetrikaControllerTest.bindDirectTest.after.csv")
    void bindDirectTest() {
        var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/{business_id}/metrika/direct")
                .queryParam("_user_id", GOOD_UID)
                .queryParam("login", LOGIN)
                .buildAndExpand(BUSINESS_ID)
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.post(url), getClass(),
                "json/BusinessMetrikaControllerTest.bindDirectTest.json");

        verifySentNotificationType(partnerNotificationClient, 1, 1618813719L);
    }


    @Test
    @DbUnitDataSet(after = "csv/BusinessMetrikaControllerTest.bindGoodDirectTest.after.csv")
    void bindGoodDirectTest() {
        var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/{business_id}/metrika/direct")
                .queryParam("_user_id", GOOD_UID)
                .queryParam("login", GOOD_LOGIN)
                .buildAndExpand(BUSINESS_ID)
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.post(url), getClass(),
                "json/BusinessMetrikaControllerTest.bindGoodDirectTest.json");

        verifyNoInteractions(partnerNotificationClient);
    }

    /**
     * Проверяется, что права на счетчик не будут повторно выданы, так как аккаунт в директе является админом в бизнесе
     */
    @Test
    @DbUnitDataSet(before = "csv/BusinessMetrikaControllerTest.businessAdmin.csv")
    void bindGoodDirectTestForAdmin() {
        var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/{business_id}/metrika/direct")
                .queryParam("_user_id", GOOD_UID)
                .queryParam("login", GOOD_LOGIN)
                .buildAndExpand(BUSINESS_ID)
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.post(url), getClass(),
                "json/BusinessMetrikaControllerTest.bindGoodDirectTest.json");
    }

    @Test
    @DbUnitDataSet(
            before = "csv/BusinessMetrikaControllerTest.unbindDirectTest.before.csv",
            after = "csv/BusinessMetrikaControllerTest.unbindDirectTest.after.csv")
    void unbindDirectTest() {
        var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/{business_id}/metrika/direct")
                .queryParam("_user_id", UID)
                .queryParam("", LOGIN)
                .buildAndExpand(BUSINESS_ID)
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.post(url), getClass(),
                "json/BusinessMetrikaControllerTest.unbindDirectTest.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/BusinessMetrikaControllerTest.getBindingTest.before.csv")
    void getBindingTest() {
        var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/{business_id}/metrika/direct")
                .buildAndExpand(BUSINESS_ID)
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), getClass(),
                "json/BusinessMetrikaControllerTest.getBindingTest.json");
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/BusinessMetrikaControllerTest.getBindingTest.before.csv",
            "csv/BusinessMetrikaControllerTest.businessAdmin.csv"
    },
            after = "csv/BusinessMetrikaControllerTest.approveBindingTest.after.csv")
    void approveBindingTest() {
        metrikaIntApiMockWebServer.enqueue(new MockResponse());
        var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/metrika/direct/approve")
                .queryParam("_user_id", UID)
                .queryParam("invitation_id", INVITATION_ID)
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.post(url), getClass(),
                "json/BusinessMetrikaControllerTest.approveBindingTest.json");

        verifySentNotificationType(partnerNotificationClient, 1, 1621411473L);
    }

    @Test
    void getBindingNotFoundTest() {
        var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/businesses/{business_id}/metrika/direct")
                .buildAndExpand(BUSINESS_ID)
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), getClass(),
                "json/BusinessMetrikaControllerTest.getBindingNotFoundTest.json");
    }
}
