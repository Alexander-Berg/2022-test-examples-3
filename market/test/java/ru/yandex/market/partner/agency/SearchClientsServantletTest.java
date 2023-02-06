package ru.yandex.market.partner.agency;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientSearchFieldType;
import ru.yandex.market.core.balance.model.FullClientInfo;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link SearchClientsServantlet}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class SearchClientsServantletTest extends FunctionalTest {

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private PassportService passportService;

    @Test
    void testSearchByLogin() {
        FullClientInfo fullClientInfo = new FullClientInfo(1L, ClientType.OOO, "AgencyClient1",
                "test@mail.com", "+79801111111", "+79801111111", "url.com",
                true, 1L);
        when(balanceService.searchClients(any(ClientSearchFieldType.class), anyString(), any()))
                .thenReturn(List.of(fullClientInfo));

        when(balanceContactService.getUidsByClient(eq(1L)))
                .thenReturn(List.of(111L));

        when(passportService.getUsers(any()))
                .thenReturn(List.of(new UserInfo(111L, "name111", "email111", "login111")));

        var response = FunctionalTestHelper.get(baseUrl + "/searchClients?fieldType=login&searchString=test&format=json");
        JsonTestUtil.assertEquals(response, getClass(), "SearchClientsServantletTest.testSearchByLogin.json");
    }
}
