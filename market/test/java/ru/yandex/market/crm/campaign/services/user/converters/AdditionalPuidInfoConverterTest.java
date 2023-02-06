package ru.yandex.market.crm.campaign.services.user.converters;

import org.junit.Test;

import ru.yandex.market.crm.campaign.services.user.AdditionalPuidInfoDto;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class AdditionalPuidInfoConverterTest {
    @Test
    public void correctConvertNullRegDate() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(12345L);
        AdditionalPuidInfoConverter puidInfoConverter = new AdditionalPuidInfoConverter();
        AdditionalPuidInfoDto puidInfoDto = puidInfoConverter.convert(userInfo);
        assertNull(puidInfoDto.getRegistrationDate());
    }

    @Test
    public void correctConvertUserInfo() {
        UserInfo userInfo = new UserInfo();
        Long uid = 12345L;
        userInfo.setUid(uid);
        userInfo.setRegDate("1294999198");
        userInfo.setLogin("test");
        userInfo.setYandexPlusSub(false);
        AdditionalPuidInfoConverter puidInfoConverter = new AdditionalPuidInfoConverter();
        AdditionalPuidInfoDto puidInfoDto = puidInfoConverter.convert(userInfo);
        assertEquals("14.01.2011", puidInfoDto.getRegistrationDate());
        assertEquals("test", puidInfoDto.getLogin());
        assertEquals(uid.toString(), puidInfoDto.getUidValue());
        assertFalse(puidInfoDto.hasYandexPlus());
    }
}
