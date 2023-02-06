package ru.yandex.direct.web.entity.adgroup.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.keyword.model.AutoBudgetPriority;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.SaveAdGroupsResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class AdGroupControllerCopyTestBase extends TextAdGroupControllerTestBase {

    protected static final Double GENERAL_PRICE_DOUBLE = 315.4d;
    protected static final BigDecimal GENERAL_PRICE =
            BigDecimal.valueOf(GENERAL_PRICE_DOUBLE).setScale(2, RoundingMode.DOWN);

    protected static final BigDecimal PRICE_SEARCH =
            BigDecimal.valueOf(197.9).setScale(2, RoundingMode.DOWN);
    protected static final BigDecimal PRICE_CONTEXT =
            BigDecimal.valueOf(231.3).setScale(2, RoundingMode.DOWN);
    protected static final int PRIORITY = AutoBudgetPriority.HIGH.getTypedValue();
    protected static final int DEFAULT_PRIORITY = AutoBudgetPriority.MEDIUM.getTypedValue();

    protected AdGroupInfo adGroupInfo;
    protected Long adGroupId;

    protected void createAdGroup() {
        adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    protected List<Long> copyAndCheckResult(List<WebTextAdGroup> requestAdGroups) {
        WebResponse response = controller.saveTextAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                true, false, true, null, null);
        checkResponse(response);

        List<AdGroup> adGroupCopies =
                adGroupRepository.getAdGroups(shard, ((SaveAdGroupsResponse) response).getResult());
        assumeThat("количество скопированных групп не соответствует ожидаемому",
                adGroupCopies, hasSize(requestAdGroups.size()));
        return mapList(adGroupCopies, AdGroup::getId);
    }
}
