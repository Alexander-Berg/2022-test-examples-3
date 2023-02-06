package ru.yandex.market.supportwizard;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.supportwizard.base.PartnerMoney;
import ru.yandex.market.supportwizard.base.PartnerType;
import ru.yandex.market.supportwizard.base.ProgramType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.storage.PartnerMoneyRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainControllerTest extends BaseFunctionalTest {

    private static final Long SHOP_A_ID = 3L;
    private static final Long SHOP_A_CAMPAIGN_ID = 245246L;
    private static final String SHOP_A_NAME = "testShopName1";
    private static final Long SHOP_A_MONEY = 1000L;
    private static final PartnerMoney SHOP_A =
            new PartnerMoney.Builder(SHOP_A_ID, PartnerType.SHOP)
                    .campaignId(SHOP_A_CAMPAIGN_ID)
                    .partnerName(SHOP_A_NAME)
                    .money(SHOP_A_MONEY)
                    .partnerPlacementProgramTypes(List.of(ProgramType.ADV))
                    .build();

    @Autowired
    private TestRestTemplate template;

    @Autowired
    PartnerMoneyRepository partnerMoneyRepository;

    @Test
    void getMoneyTest() {
        partnerMoneyRepository.save(SHOP_A.toPartnerMoney());
        ResponseEntity<String> response = template.getForEntity("/api/get-partner-money?id=3", String.class);
        assertEquals(response.getBody(), "1000");
    }

    @Test
    void updateTest() {
        ResponseEntity<String> response = template.postForEntity("/api/update_partners", null, String.class);
        assertEquals(response.getBody(), "Updating database");
    }
}
