package ru.yandex.market.partner.mvc.controller.billing;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.netting.NettingTransitionStatus;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты {@link NettingTransitionController}
 */
@DbUnitDataSet(before = "csv/NettingTransitionInfoControllerTest.before.csv")
public class NettingTransitionControllerTest extends FunctionalTest {

    private static final long CAMPAIGN_ID = 200004444;

    @Test
    void getNettingTransitionInfoDefaultTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/info")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoDefaultTest.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingTransitionInfoControllerTest.getNettingTransitionInfoFullTest.csv")
    void getNettingTransitionInfoFullTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/info")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoFullTest.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingTransitionInfoControllerTest.statusDisabled.csv")
    void getNettingTransitionInfoWithoutBonusTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/info")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoWithoutBonusTest.json");
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/NettingTransitionInfoControllerTest.getNettingTransitionInfoNotIssuedBonusTest.csv",
            "csv/NettingTransitionInfoControllerTest.nettingBonus.csv",
    })
    void getNettingTransitionInfoNotIssuedBonusTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/info")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoNotIssuedBonusTest.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingTransitionInfoControllerTest.getNettingTransitionInfoRejectedTest.csv")
    void getNettingTransitionInfoRejectedTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/info")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoRejectedTest.json");
    }

    @Test
    @DbUnitDataSet(after = "csv/NettingTransitionInfoControllerTest.setEnabledStatus.after.csv")
    void setEnabledNewStatus() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/status")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .queryParam("status", NettingTransitionStatus.ENABLED.getId())
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.put(url, null), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoDefaultTest.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingTransitionInfoControllerTest.nettingBonus.csv",
            after = "csv/NettingTransitionInfoControllerTest.setEnabledStatus.after.csv")
    void changeEnabledNewStatus() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/status")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .queryParam("status", NettingTransitionStatus.ENABLED.getId())
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.put(url, null), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoNotIssuedBonusTest.json");
    }

    @Test
    @DbUnitDataSet(before = "csv/NettingTransitionInfoControllerTest.nettingBonus.csv",
            after = "csv/NettingTransitionInfoControllerTest.issueBonus.after.csv")
    void issueBonus() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl +
                "/netting/transition/bonus")
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build()
                .toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.post(url, null), this.getClass(),
                "json/NettingTransitionInfoControllerTest.getNettingTransitionInfoFullTest.json");
    }
}
