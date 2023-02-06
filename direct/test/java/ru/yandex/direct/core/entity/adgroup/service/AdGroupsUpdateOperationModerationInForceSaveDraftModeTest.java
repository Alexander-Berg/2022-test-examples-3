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
public class AdGroupsUpdateOperationModerationInForceSaveDraftModeTest extends AdGroupsUpdateOperationModerationTestBase {

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // ModerationMode = FORCE_SAVE_DRAFT, campaign is moderated, adGroup is draft
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },

                // ModerationMode = FORCE_SAVE_DRAFT, campaign is moderated, adGroup is NOT draft
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NO, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.READY, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NO, StatusPostModerate.REJECTED,
                        false,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.YES, StatusPostModerate.NO,
                        true,
                        CampaignsStatusmoderate.Yes,
                        StatusModerate.NEW, StatusPostModerate.NO
                },

                // ModerationMode = FORCE_SAVE_DRAFT, campaign is draft, adGroup is draft
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO,
                        false,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.NO
                },
                {
                        ModerationMode.FORCE_SAVE_DRAFT,
                        CampaignsStatusmoderate.New,
                        StatusModerate.NEW, StatusPostModerate.REJECTED,
                        false,
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
