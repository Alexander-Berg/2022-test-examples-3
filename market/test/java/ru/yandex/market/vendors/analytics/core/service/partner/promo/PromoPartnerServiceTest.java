package ru.yandex.market.vendors.analytics.core.service.partner.promo;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.model.partner.promo.PromoPartnerUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "PromoPartnerServiceTest.before.csv")
class PromoPartnerServiceTest extends FunctionalTest {

    @Autowired
    private PromoPartnerService promoPartnerService;

    @MockBean(name = "userInfoService")
    private UserInfoService userInfoService;

    @Test
    @DisplayName("Загрузить всех пользователей промо-партнёра")
    void loadPromoUsers() {
        List<PromoPartnerUser> promoPartnerUsers = promoPartnerService.loadPromoPartnerUsers();
        assertEquals(4, promoPartnerUsers.size());
    }

    @Test
    @DisplayName("Добавление пользователя к промо-партнёру")
    @DbUnitDataSet(after = "PromoPartnerServiceTest.addUser.after.csv")
    void addUser() {
        String login = "aaaa";
        mock(login, 1);
        promoPartnerService.grant(login, null);
    }

    @Test
    @DisplayName("Добавление несуществующего пользователя")
    void addUnknownUser() {
        String login = "aaaa";
        assertThrows(IllegalArgumentException.class, () -> promoPartnerService.grant(login, null));
    }

    @Test
    @DisplayName("Удаление старых пользователей")
    @DbUnitDataSet(after = "PromoPartnerServiceTest.removeOutdatedUsers.after.csv")
    void removeOutdatedUsers() {
        mock("yndx-1", 1001);
        mock("yndx1", 1002);
        mock("yandex", 1003);
        mock("google", 1004);
        promoPartnerService.removeOutdatedUsers();
    }

    private void mock(String userLogin, long userId) {
        UserInfo userInfo = mockUserInfo(userLogin, userId);
        when(userInfoService.getUserInfo(userLogin)).thenReturn(userInfo);
        when(userInfoService.getUserInfo(userId)).thenReturn(userInfo);
    }

    private static UserInfo mockUserInfo(String userLogin, long userId) {
        var userInfo = Mockito.mock(UserInfo.class);
        when(userInfo.getLogin()).thenReturn(userLogin);
        when(userInfo.getUserId()).thenReturn(userId);
        return userInfo;
    }
}