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
public class AdGroupsUpdateOperationModerationInForceModerateModeTest extends AdGroupsUpdateOperationModerationTestBase {

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // ModerationMode = FORCE_MODERATE, campaign is moderated, adGroup is draft
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO
                },

                // ModerationMode = FORCE_MODERATE, campaign is moderated, adGroup is NOT draft
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.REJECTED,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.REJECTED
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.REJECTED,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.REJECTED
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NO, StatusPostModerate.REJECTED,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NO, StatusPostModerate.REJECTED
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NO, StatusPostModerate.REJECTED,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.REJECTED
                },

                // ModerationMode = FORCE_MODERATE, campaign is rejected, adGroup is draft
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.No,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Ready,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.No,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Ready,
                        StatusModerate.READY, StatusPostModerate.NO
                },

                // ModerationMode = FORCE_MODERATE, campaign is rejected, adGroup is NOT draft
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.No,
                        StatusModerate.YES, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.No,
                        StatusModerate.YES, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.No,
                        StatusModerate.YES, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Ready,
                        StatusModerate.READY, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.No,
                        StatusModerate.NO, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Ready,
                        StatusModerate.READY, StatusPostModerate.NO
                },

                // ModerationMode = FORCE_MODERATE, campaign is draft, adGroup is draft
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_MODERATE,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        true,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO
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
