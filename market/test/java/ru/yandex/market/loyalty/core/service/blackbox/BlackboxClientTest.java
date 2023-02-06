package ru.yandex.market.loyalty.core.service.blackbox;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class BlackboxClientTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final String IP_STUB = "8.8.8.8";
    private static final long MUID = (1L << 60) | 1;
    private static final long SBER_ID = (1L << 61) - 1L;

    @Autowired
    private BlackboxClient blackboxClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldReturnEmptyStringForSberIdAndMuid() {
        String result = blackboxClient.getProfileInfo(IP_STUB, MUID);
        assertEquals("", result);

        result = blackboxClient.getProfileInfo(IP_STUB, SBER_ID);
        assertEquals("", result);
    }

    @Test
    public void shouldReturnNotNullUserStatForSberIdAndMuid() {
        BlackboxClient.UserStat result = blackboxClient.getUserStat(IP_STUB, MUID);
        assertNotNull(result);
        assertFalse(result.isEmployee());
        assertFalse(result.isYandexPlus());

        result = blackboxClient.getUserStat(IP_STUB, SBER_ID);
        assertNotNull(result);
        assertFalse(result.isEmployee());
        assertFalse(result.isYandexPlus());
    }

    @Test
    public void testBlackboxResponseMapping() throws Exception {
        String jsonValue = "{\n" +
                "   \"users\" : [\n" +
                "      {\n" +
                "         \"karma\" : {\n" +
                "            \"value\" : 0\n" +
                "         },\n" +
                "         \"login\" : \"Junit-Test\",\n" +
                "         \"have_hint\" : true,\n" +
                "         \"uid\" : {\n" +
                "            \"lite\" : false,\n" +
                "            \"value\" : \"70500\",\n" +
                "            \"hosted\" : false\n" +
                "         },\n" +
                "         \"id\" : \"70500\",\n" +
                "         \"have_password\" : true,\n" +
                "         \"karma_status\" : {\n" +
                "            \"value\" : 0\n" +
                "         }\n" +
                "      }\n" +
                "   ]\n" +
                "}";

        UserInfoResponse blackBoxResponse = objectMapper.readValue(jsonValue, UserInfoResponse.class);

        assertNotNull(blackBoxResponse);
    }

    @Test
    public void getProfileByLogin() {
        BlackboxUtils.mockBlackbox("test", PerkType.YANDEX_EMPLOYEE, false, blackboxRestTemplate);

        Collection<String> strings = List.of("test");
        Collection<StaffInfo> profileByLogin = blackboxClient.getProfileByLogin(strings);

        assertNotNull(profileByLogin);

    }
}
