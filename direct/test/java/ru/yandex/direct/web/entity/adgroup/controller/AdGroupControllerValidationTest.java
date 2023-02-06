package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.data.TestWebAdGroupBuilder;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerValidationTest extends TextAdGroupControllerTestBase {

    private static final double LOW_INVALID_PRICE = 0.1;
    private static final double HIGH_INVALID_PRICE = 30_000;


    @Test
    public void addTextAdGroup_tooLowPrice() {
        WebTextAdGroup requestAdGroup = makeAdGroup();
        requestAdGroup.withGeneralPrice(LOW_INVALID_PRICE);

        addAndExpectError(requestAdGroup,
                "[0]." + WebTextAdGroup.Prop.GENERAL_PRICE,
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN.getCode()
        );
    }

    @Test
    public void addTextAdGroup_tooHighPrice() {
        WebTextAdGroup requestAdGroup = makeAdGroup();
        requestAdGroup.withGeneralPrice(HIGH_INVALID_PRICE);

        addAndExpectError(requestAdGroup,
                "[0]." + WebTextAdGroup.Prop.GENERAL_PRICE,
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX.getCode()
        );
    }

    @Test
    public void updateTextAdGroup_tooLowPrice() {
        WebTextAdGroup requestAdGroup = makeAdGroup();
        addAndCheckResult(Collections.singletonList(requestAdGroup));
        List<AdGroup> adGroups = findAdGroups();

        requestAdGroup.withId(adGroups.get(0).getId());
        requestAdGroup.withGeneralPrice(LOW_INVALID_PRICE);

        updateAndExpectError(requestAdGroup,
                "[0]." + WebTextAdGroup.Prop.GENERAL_PRICE,
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN.getCode()
        );
    }

    @Test
    public void updateTextAdGroup_tooHighPrice() {
        WebTextAdGroup requestAdGroup = makeAdGroup();
        addAndCheckResult(Collections.singletonList(requestAdGroup));
        List<AdGroup> adGroups = findAdGroups();

        requestAdGroup.withId(adGroups.get(0).getId());
        requestAdGroup.withGeneralPrice(HIGH_INVALID_PRICE);

        updateAndExpectError(requestAdGroup,
                "[0]." + WebTextAdGroup.Prop.GENERAL_PRICE,
                BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX.getCode()
        );
    }

    private void addAndCheckResult(List<WebTextAdGroup> requestAdGroups) {
        WebResponse response = controller.saveTextAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                true, false, false, null, null);
        checkResponse(response);
    }

    private WebTextAdGroup makeAdGroup() {
        return TestWebAdGroupBuilder.someWebAdGroup(campaignInfo.getCampaignId())
                .withSomeBanner()
                .withSomeKeyword()
                .build();
    }
}
