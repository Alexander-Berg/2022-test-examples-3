package ru.yandex.market.vendors.analytics.platform.controller.partner.beru;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder;

@ClickhouseDbUnitDataSet(before = "BeruPlacementControllerTestClickhouse.before.csv")
@Import(BeruPlacementControllerTest.BeruPlacementTestConfig.class)
public class BeruPlacementControllerTest extends FunctionalTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    UserInfoService userInfoService;

    //Тесты по выдаче доступа
    @DbUnitDataSet(
            before = "BeruPlacementControllerTest.before.csv",
            after = "BeruPlacementControllerTestUnknownUser.after.csv"
    )
    @Test
    public void requestNewUserTestUnknownUser() throws JsonProcessingException {
        requestAccessAndAssertResult(
                0L,
                null,
                false,
                null,
                "Access request for user 0 resolved: DENIED. User invalid"
        );
    }

    @DbUnitDataSet(
            before = "BeruPlacementControllerTest.before.csv",
            after = "BeruPlacementControllerTest.after.csv"
    )
    @Test
    public void requestNewUserTestNotMatched() throws JsonProcessingException {
        requestAccessAndAssertResult(10L, null, true, null, "");
    }

    @DbUnitDataSet(
            before = "BeruPlacementControllerTest.before.csv",
            after = "BeruPlacementControllerTestNonAdmin.after.csv"
    )
    @Test
    public void requestNewUserTestNotMatchedNotAdmin() throws JsonProcessingException {
        BlackBoxUserInfo user1 = mock(BlackBoxUserInfo.class);
        when(user1.getLogin()).thenReturn("login1");
        BlackBoxUserInfo user2 = mock(BlackBoxUserInfo.class);
        when(user2.getLogin()).thenReturn("login2");
        when(userInfoService.getUserInfo(10L)).thenReturn(user1);
        when(userInfoService.getUserInfo(11L)).thenReturn(user2);
        requestAccessAndAssertResult(
                12L,
                null,
                false,
                List.of("login1", "login2"),
                "Access request for user 12 resolved: ADMIN_APPROVAL_REQUIRED"
        );
    }

    @DbUnitDataSet(
            before = "BeruPlacementControllerTestMatched.before.csv",
            after = "BeruPlacementControllerTestMatched.after.csv"
    )
    @Test
    public void requestNewUserTestAlreadyHasAccess() throws JsonProcessingException {
        BlackBoxUserInfo user1 = mock(BlackBoxUserInfo.class);
        when(user1.getLogin()).thenReturn("login1");
        when(userInfoService.getUserInfo(21L)).thenReturn(user1);
        requestAccessAndAssertResult(
                21L,
                202L,
                false,
                null,
                "Access request for user 21 resolved: DENIED. Some admins have access already"
        );
    }

    @DbUnitDataSet(
            after = "BeruPlacementControllerTestMatched.after.csv",
            before = "BeruPlacementControllerTestMatched.before.csv"
    )
    @Test
    public void requestNewUserTestMatched() throws JsonProcessingException {
        BlackBoxUserInfo user1 = mock(BlackBoxUserInfo.class);
        when(user1.getLogin()).thenReturn("login1");
        when(userInfoService.getUserInfo(10L)).thenReturn(user1);
        requestAccessAndAssertResult(
                11L,
                null,
                false,
                List.of("login1"), "Access request for user 11 resolved: ADMIN_APPROVAL_REQUIRED"
        );
    }

    @DbUnitDataSet(
            after = "BeruPlacementControllerTestMatched.after.csv",
            before = "BeruPlacementControllerTestMatched.before.csv"
    )
    @Test
    public void requestNewUserTestMatchedNonAdmin() throws JsonProcessingException {
        BlackBoxUserInfo user1 = mock(BlackBoxUserInfo.class);
        when(user1.getLogin()).thenReturn("login1");
        when(userInfoService.getUserInfo(10L)).thenReturn(user1);
        requestAccessAndAssertResult(
                12L,
                null,
                false,
                List.of("login1"),
                "Access request for user 12 resolved: ADMIN_APPROVAL_REQUIRED"
        );
    }

    @DbUnitDataSet(
            before = "BeruPlacementControllerTest.before.csv",
            after = "BeruPlacementControllerTest.before.csv"
    )
    @Test
    public void requestNewUserTestMultiBusinessNullBusiness() throws JsonProcessingException {
        requestAccessAndAssertResult(
                23L,
                null,
                false,
                null,
                "Access request for user 23 resolved: DENIED. User invalid"
        );
    }

    @DbUnitDataSet(
            before = "BeruPlacementControllerTest.before.csv",
            after = "BeruPlacementControllerMultibusinessFirstTest.after.csv"
    )
    @Test
    public void requestNewUserTestMultiBusinessFirst() throws JsonProcessingException {
        requestAccessAndAssertResult(23L, 2L, true, null, "");
    }

    @DbUnitDataSet(
            before = "BeruPlacementControllerTest.before.csv",
            after = "BeruPlacementControllerMultibusinessSecondTest.after.csv"
    )
    @Test
    public void requestNewUserTestMultiBusinessSecond() throws JsonProcessingException {
        requestAccessAndAssertResult(23L, 2L, true, null, "");
        requestAccessAndAssertResult(
                26L,
                3L,
                false,
                null,
                "Access request for user 26 resolved: DENIED. Business blocked by others: [2]"
        );
    }

    //Тесты по проверке доступа по новому контракту
    @DbUnitDataSet(before = "BeruPlacementControllerCheckAccessTest.before.csv")
    @Test
    public void checkUserHasAccessPrimaryMultibusinessWithAccess() throws JsonProcessingException {
        checkUserAccessAndAssertResult(27L, 4L, true, false);
        checkUserAccessAndAssertResult(28L, 4L, true, false);
    }

    @DbUnitDataSet(before = "BeruPlacementControllerCheckAccessTest.before.csv")
    @Test
    public void checkUserHasAccessSecondaryMultibusiness() throws JsonProcessingException {
        checkUserAccessAndAssertResult(27L, 5L, false, true);
        checkUserAccessAndAssertResult(29L, 5L, false, true);
    }

    @DbUnitDataSet(before = "BeruPlacementControllerCheckAccessTest.before.csv")
    @Test
    public void checkUserHasAccessPrimaryMultibusinessNoAccess() throws JsonProcessingException {
        checkUserAccessAndAssertResult(30L, 7L, false, false);
        checkUserAccessAndAssertResult(30L, 6L, false, false);
    }

    //Тесты по проверке валидности пользователя
    @Test
    public void checkValidUserTest() {
        checkUserAndAssertResult(12L, null, true);
    }

    @Test
    public void checkValidMultibusinessUserTest() {
        checkUserAndAssertResult(23L, 2L, true);
        checkUserAndAssertResult(23L, 3L, true);
    }

    @DbUnitDataSet(before = "CheckUserAccessTest.before.csv")
    @Test
    public void checkInvalidMultibusinessUserTest() {
        checkUserAndAssertResult(23L, 2L, false);
        checkUserAndAssertResult(26L, 2L, false);
        checkUserAndAssertResult(26L, 3L, true);
    }

    @DbUnitDataSet(before = "CheckUserAccessTest.before.csv")
    @Test
    public void checkRegisteredValidUserTest() {
        checkUserAndAssertResult(255L, null, true);
    }

    @Test
    public void checkInvalidUserTest() {
        checkUserAndAssertResult(255L, null, false);
    }

    //Тесты по перерассчету заявок
    @DbUnitDataSet(
            before = "RecalculateApplicationsTest.before.csv",
            after = "RecalculateApplicationsTest.after.csv"
    )
    @Test
    public void recalculateApplicationsTest() {
        requestRecalculateApplications(500);
    }

    //Тесты по удалению заявок
    @DbUnitDataSet(
            before = "DeleteApplicationsTest.before.csv",
            after = "DeleteApplicationsSoftTest.after.csv"
    )
    @Test
    public void deleteBeruApplicationsSoftTest() {
        requestRevokeApplications(500, false, false);
    }

    @DbUnitDataSet(
            before = "DeleteApplicationsTest.before.csv",
            after = "DeleteApplicationsHardTest.after.csv"
    )
    @Test
    public void deleteBeruApplicationsHardTest() {
        requestRevokeApplications(500, true, true);
    }

    @DbUnitDataSet(
            before = "ForceGrantBusinessUserAccess.before.csv",
            after = "ForceGrantBusinessUserAccess.after.csv"

    )
    @Test
    public void forceGrantBusinessUserAccessTest() {
        requestForceGrantBusinessUserAccess(14, 102, "testUser14", "testEmail14");
    }

    @DbUnitDataSet(
            before = "ForceAddBusinessToShop.before.csv",
            after = "ForceAddBusinessToShop.after.csv"

    )
    @Test
    public void forceAddBusinessToShopTest() {
        requestAddBusinessToShopTest(14, 103, 1000000, "testUser14", "testEmail14");
    }

    public void requestAddBusinessToShopTest(long userId, long businessId, long shopId, String userName, String email) {
        FunctionalTestHelper.get(
                UriComponentsBuilder.fromUriString(baseUrl())
                        .path("/beru/placement/forceAddBusinessToShop")
                        .queryParam("userId", userId)
                        .queryParam("businessId", businessId)
                        .queryParam("shopId", shopId)
                        .queryParam("userName", userName)
                        .queryParam("email", email)
                        .toUriString()
        );
    }

    public void requestForceGrantBusinessUserAccess(long userId, long businessId, String userName, String email) {
        FunctionalTestHelper.get(
                UriComponentsBuilder.fromUriString(baseUrl())
                        .path("/beru/placement/forceGrantAccessForUserAndBusiness")
                        .queryParam("userId", userId)
                        .queryParam("businessId", businessId)
                        .queryParam("userName", userName)
                        .queryParam("email", email)
                        .toUriString()

        );
    }

    public void requestRevokeApplications(long partnerId, boolean hard, boolean revokeBusinessLink) {
        FunctionalTestHelper.get(
                UriComponentsBuilder.fromUriString(baseUrl())
                        .path("/beru/placement/revokeBeruApplications")
                        .queryParam("hard", hard)
                        .queryParam("partnerId", partnerId)
                        .queryParam("revokeBusinessLink", revokeBusinessLink)
                        .toUriString()

        );
    }

    public void requestRecalculateApplications(long partnerId) {
        FunctionalTestHelper.get(
                UriComponentsBuilder.fromUriString(baseUrl())
                        .path("/beru/placement/recalculateBeruApplications")
                        .queryParam("partnerIds", partnerId)
                        .toUriString()
        );
    }

    public void checkUserAccessAndAssertResult(long uid, Long businessId, boolean hasAccess, boolean needForm)
            throws JsonProcessingException {
        String response = FunctionalTestHelper.get(
                UriComponentsBuilder.fromUriString(baseUrl())
                        .path("/beru/placement/checkUserHasAccess/" + uid)
                        .queryParam("business_id", businessId)
                        .toUriString()
        ).getBody();
        assertJsonEqualsIgnoreArrayOrder(makeExpected(hasAccess, needForm), response);
    }

    public void checkUserAndAssertResult(long uid, Long businessId, boolean isValid) {
        boolean response = Boolean.parseBoolean(
                FunctionalTestHelper.get(
                        UriComponentsBuilder.fromUriString(baseUrl())
                                .path("/beru/placement/canUserGetAccess/" + uid)
                                .queryParam("business_id", businessId)
                                .toUriString()
                ).getBody()
        );
        Assertions.assertEquals(isValid, response);
    }

    public void requestAccessAndAssertResult(
            long uid,
            Long businessId,
            boolean hasAccess,
            List<String> adminContacts,
            String message
    ) throws JsonProcessingException {
        String response = FunctionalTestHelper.get(
                UriComponentsBuilder.fromUriString(baseUrl())
                        .path("/beru/placement/attemptGrantUserAccess/" + uid)
                        .queryParam("business_id", businessId)
                        .toUriString()
        ).getBody();
        assertJsonEqualsIgnoreArrayOrder(
                makeExpected(hasAccess, adminContacts, message),
                response
        );
    }

    private String makeExpected(boolean hasAccess, List<String> adminContacts, String message) throws JsonProcessingException {
        return MAPPER.writeValueAsString(new BeruGrantAccessDTO(hasAccess, adminContacts, message));
    }

    private String makeExpected(boolean hasAccess, boolean needForm) throws JsonProcessingException {
        return MAPPER.writeValueAsString(new BeruAccessCheckDTO(hasAccess, needForm));
    }

    @TestConfiguration
    static class BeruPlacementTestConfig {

        @Bean(name = "userInfoService")
        @Primary
        public UserInfoService userInfoService() {
            return mock(UserInfoService.class);
        }

        @Bean(name = "testBlackboxService")
        @Primary
        public UserInfoService yandexTeamUserInfoService() {
            return mock(UserInfoService.class);
        }
    }
}
