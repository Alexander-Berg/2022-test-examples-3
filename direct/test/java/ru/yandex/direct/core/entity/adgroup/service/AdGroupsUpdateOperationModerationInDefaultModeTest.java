package ru.yandex.direct.core.entity.adgroup.service;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusmoderate;

import static java.util.Arrays.asList;

@CoreTest
@RunWith(Parameterized.class)
public class AdGroupsUpdateOperationModerationInDefaultModeTest extends AdGroupsUpdateOperationModerationTestBase {

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // ModerationMode = DEFAULT, campaign is moderated, adGroup is draft
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                // ModerationMode = DEFAULT, campaign is moderated, adGroup is NOT draft
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NO, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.REJECTED,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.REJECTED
                },
                // ModerationMode = DEFAULT, campaign is rejected, adGroup is draft
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.No,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.No,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                // ModerationMode = DEFAULT, campaign is rejected, adGroup is NOT draft
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.No,
                        StatusModerate.YES, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.No,
                        StatusModerate.YES, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.No,
                        StatusModerate.YES, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Ready,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.No,
                        StatusModerate.NO, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Ready,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                // ModerationMode = DEFAULT, campaign is draft, adGroup is draft
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                // ModerationMode = DEFAULT, campaign is draft, adGroup is NOT draft (impossible case)
                {
                        ModerationMode.DEFAULT,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NO, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NO, StatusPostModerate.NO
                },
        });
    }

    @Test
    public void adGroupModerationStatusesAreUpdatedWell() {
        super.adGroupModerationStatusesAreUpdatedWell();
    }

    @Test
    public void campaignModerationStatusIsUpdatedWell() {
        super.campaignModerationStatusIsUpdatedWell();
    }
}
