package ru.yandex.market.partner.message;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.agency.AgencyClient;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.doReturn;

/**
 * Unit тесты для {@link GetMessageFilterShopInfoServantletTest}.
 *
 * @author avetokhin 15/11/16.
 */
@DbUnitDataSet(before = "GetMessageHeadersServantletTest.before.csv")
public class GetMessageFilterShopInfoServantletTest extends AbstractClientBasedTest {

    @Autowired
    @Spy
    private AgencyService agencyService;

    private static final long CAMPAIGN_ID_1 = 103L;
    private static final long CAMPAIGN_ID_2 = 104L;

    private static final long AGENCY_ID = 1111L;

    private static final AgencyClient AGENCY_CLIENT = new AgencyClient(AGENCY_ID);

    static {
        AGENCY_CLIENT.addCampaign(CAMPAIGN_ID_1);
        AGENCY_CLIENT.addCampaign(CAMPAIGN_ID_2);
    }

    private static final Long USER_ID = 100500L;
    private static final Long AGENCY_USER_ID = 100900L;

    @Test
    @DisplayName("Проверить вызовы сервиса с корректными параметрами для обычного пользователя.")
    public void testForUserCall() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getMessageFilterShopInfo?format=json&euid={euid}", USER_ID);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/getMessageFilterShopInfo.superuser.json");
    }

    @Test
    @DisplayName("Проверить вызовы сервиса с корректными параметрами для агентства.")
    public void testForAgencyCall() {
        doReturn(Collections.singletonList(AGENCY_CLIENT)).when(agencyService).getAgencyClients(AGENCY_ID);
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getMessageFilterShopInfo?format=json&euid={euid}", AGENCY_USER_ID);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/getMessageFilterShopInfo.agency.json");
    }

    @Test
    @DisplayName("Проверить вызовы сервиса с корректными параметрами для агентства.")
    public void testForAgencyCallFiltered() {
        doReturn(Collections.singletonList(AGENCY_CLIENT)).when(agencyService).getAgencyClients(AGENCY_ID);
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getMessageFilterShopInfo?format=json&euid={euid}&client_id={client_id}",
                AGENCY_USER_ID, 102L);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/getMessageFilterShopInfo.agency.filtered.json");
    }
}
