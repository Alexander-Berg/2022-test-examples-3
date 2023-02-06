package ru.yandex.direct.api.v5.entity.changes.converter;

import java.util.List;

import com.yandex.direct.api.v5.changes.CampaignChangesItem;
import com.yandex.direct.api.v5.changes.CheckCampaignsResponse;
import com.yandex.direct.api.v5.changes.CheckResponse;
import com.yandex.direct.api.v5.changes.CheckResponseIds;
import com.yandex.direct.api.v5.changes.CheckResponseModified;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.changes.model.CheckCampaignsIntResp;
import ru.yandex.direct.core.entity.changes.model.CheckIntResp;

import static com.yandex.direct.api.v5.changes.CampaignChangesInEnum.CHILDREN;
import static com.yandex.direct.api.v5.changes.CampaignChangesInEnum.SELF;
import static com.yandex.direct.api.v5.changes.CampaignChangesInEnum.STAT;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.changes.converter.ChangesCommonConvertUtil.convertCheckCampaignsResult;
import static ru.yandex.direct.api.v5.entity.changes.converter.ChangesCommonConvertUtil.convertCheckResult;
import static ru.yandex.direct.core.entity.changes.model.CheckIntResp.ParamBlock.ModifiedCampaignIds;
import static ru.yandex.direct.core.entity.changes.model.CheckIntResp.ParamBlock.NotFoundAdGroupIds;
import static ru.yandex.direct.core.entity.changes.model.CheckIntResp.ParamBlock.NotFoundCampaignIds;
import static ru.yandex.direct.core.entity.changes.model.CheckIntResp.ParamBlock.UnprocessedAdGroupIds;
import static ru.yandex.direct.core.entity.changes.model.CheckIntResp.ParamBlock.UnprocessedAdIds;

public class ChangesCommonConvertUtilTest {

    @Test
    public void testConvertCheckCampaignsResult() {
        CheckCampaignsIntResp item1 = new CheckCampaignsIntResp(1L, true, false, false);
        CheckCampaignsIntResp item2 = new CheckCampaignsIntResp(2L, false, true, false);
        CheckCampaignsIntResp item3 = new CheckCampaignsIntResp(3L, false, false, true);
        CheckCampaignsIntResp item4 = new CheckCampaignsIntResp(4L, true, true, true);

        CheckCampaignsResponse externalResult =
                convertCheckCampaignsResult(List.of(item1, item2, item3, item4), null);

        List<CampaignChangesItem> gotResults = externalResult.getCampaigns();

        assertThat(gotResults)
                .usingElementComparatorOnFields("campaignId", "changesIn")
                .containsExactly(
                        new CampaignChangesItem().withCampaignId(1L).withChangesIn(SELF),
                        new CampaignChangesItem().withCampaignId(2L).withChangesIn(CHILDREN),
                        new CampaignChangesItem().withCampaignId(3L).withChangesIn(STAT),
                        new CampaignChangesItem().withCampaignId(4L).withChangesIn(SELF, CHILDREN, STAT));
    }

    @Test
    public void testConvertEmptyCheckCampaignsResult() {
        CheckCampaignsResponse externalResult = convertCheckCampaignsResult(List.of(), null);
        assertThat(CheckCampaignsResponse.PropInfo.CAMPAIGNS.get(externalResult)).isNull();
    }

    @Test
    public void testConvertEmptyCheckResult() {
        var internalResponse = new CheckIntResp();
        internalResponse.setCampaignIdToBorderDateMap(emptyMap());
        internalResponse.setIds(ModifiedCampaignIds, List.of());
        internalResponse.setIds(NotFoundCampaignIds, List.of(123L));
        internalResponse.setIds(NotFoundAdGroupIds, List.of());
        internalResponse.setIds(UnprocessedAdGroupIds, List.of(456L));
        internalResponse.setIds(UnprocessedAdIds, List.of());

        CheckResponse checkResponse = convertCheckResult(internalResponse);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(CheckResponseModified.PropInfo.CAMPAIGNS_STAT.get(checkResponse.getModified())).isNull();
            softly.assertThat(CheckResponseModified.PropInfo.CAMPAIGN_IDS.get(checkResponse.getModified())).isNull();
            softly.assertThat(CheckResponseIds.PropInfo.AD_GROUP_IDS.get(checkResponse.getNotFound())).isNull();
            softly.assertThat(CheckResponseIds.PropInfo.AD_IDS.get(checkResponse.getUnprocessed())).isNull();
        });
    }
}
