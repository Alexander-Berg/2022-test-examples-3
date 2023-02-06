package ru.yandex.autotests.direct.cmd.bssynced;

import java.util.Arrays;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BidsPerformanceStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.CampaignsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PhrasesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.jmock.Expectations.anything;
import static ru.yandex.autotests.direct.httpclient.TestEnvironment.newDbSteps;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public class BsSyncedHelper {

    public static void setCampaignBsSynced(DirectCmdRule cmdRule, Long campaignId, StatusBsSynced statusBsSynced) {
        cmdRule.apiSteps().campaignFakeSteps()
                .setBSSynced(campaignId, statusBsSynced == StatusBsSynced.YES);
    }

    public static void setGroupBsSynced(DirectCmdRule cmdRule, Long groupId, StatusBsSynced statusBsSynced) {
        cmdRule.apiSteps().groupFakeSteps()
                .setGroupFakeStatusBsSynced(groupId, statusBsSynced.toString());
    }

    public static void setBannerBsSynced(DirectCmdRule cmdRule, Long bannerId, StatusBsSynced statusBsSynced) {
        cmdRule.apiSteps().bannersFakeSteps()
                .setStatusBsSynced(bannerId, statusBsSynced.toString());
    }

    public static void setPerfFilterBsSynced(Long filterId, BidsPerformanceStatusbssynced statusBsSynced,
            String login)
    {
        BidsPerformanceRecord record = TestEnvironment.newDbSteps().useShardForLogin(login)
                .bidsPerformanceSteps().getBidsPerformance(filterId);
        record.setStatusbssynced(statusBsSynced);
        TestEnvironment.newDbSteps().bidsPerformanceSteps().updateBidsPerformance(record);
    }

    public static void setPhraseBsSynced(DirectCmdRule cmdRule, Long phraseId, StatusBsSynced statusBsSynced) {
        cmdRule.apiSteps().phrasesFakeSteps().setStatusBsSynced(phraseId, statusBsSynced.toString());
    }

    public static void checkCampaignBsSynced(String login, Long campaignId, StatusBsSynced... statuses) {
        CampaignsRecord campaign = newDbSteps().useShardForLogin(login)
                .campaignsSteps().getCampaignById(campaignId);

        assertThat("статус bsSynced кампании соответствует ожиданию", campaign.getStatusbssynced().getLiteral(),
                anyStatusOf(statuses));
    }

    public static void checkGroupBsSynced(Group group, StatusBsSynced... statuses) {
        assertThat("статус bsSynced группы соответствует ожиданию",
                Arrays.asList(group.getStatus_bs_synced(), group.getStatusBsSynced()),
                hasItem(anyStatusOf(statuses)));
    }

    public static void checkGroupBsSynced(String login, Long pid, PhrasesStatusbssynced... statuses) {
        newDbSteps().useShardForLogin(login);
        PhrasesRecord group = newDbSteps().adGroupsSteps().getPhrases(pid);
        assertThat("статус bsSynced группы соответствует ожиданиям",
                group.getStatusbssynced().getLiteral(),
                anyStatusOf(statuses));
    }

    public static void checkBannerBsSynced(String login, Long bid, BannersStatusbssynced... statuses) {
        newDbSteps().useShardForLogin(login);
        BannersRecord banner = newDbSteps().bannersSteps().getBanner(bid);
        assertThat("статус bsSynced баннера соответствует ожиданию",
                banner.getStatusbssynced().getLiteral(),
                anyStatusOf(statuses));
    }

    public static void moderateCamp(DirectCmdRule cmdRule, Long campaignId) {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(campaignId);
        String login = TestEnvironment.newDbSteps().shardingSteps().getOwnerForCid(campaignId);
        ShowCampResponse camp = cmdRule.cmdSteps().campaignSteps().getShowCamp(login, campaignId.toString());
        if (camp.getGroups() != null) {
            for (Banner banner : camp.getGroups()) {
                cmdRule.apiSteps().phrasesFakeSteps().setPhraseStatusModerateYes(banner.getAdGroupId());
                cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(banner.getAdGroupId());
                cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(banner.getBid());

                if (banner.getVideoResources() != null) {
                    TestEnvironment.newDbSteps(login).bannersPerformanceSteps()
                            .setCreativeStatusModerate(campaignId, banner.getAdGroupId(), banner.getBid(),
                                    BannersPerformanceStatusmoderate.Yes);
                }
            }
        }
    }

    public static void syncCamp(DirectCmdRule cmdRule, Long campaignId) {
        cmdRule.apiSteps().campaignFakeSteps().setRandomOrderID(campaignId);
        cmdRule.apiSteps().campaignFakeSteps().setBSSynced(campaignId.intValue(), true);

        String login = TestEnvironment.newDbSteps().shardingSteps().getOwnerForCid(campaignId);
        ShowCampResponse camp = cmdRule.cmdSteps().campaignSteps().getShowCamp(login, campaignId.toString());
        if (camp.getGroups() != null) {
            for (Banner banner : camp.getGroups()) {
                if (banner.getPhrases() != null) {
                    for (Phrase phrase : banner.getPhrases()) {
                        setPhraseBsSynced(cmdRule, phrase.getId(), StatusBsSynced.YES);
                    }
                }
                cmdRule.apiSteps().groupFakeSteps().setGroupFakeStatusBsSynced(banner.getAdGroupId(), Status.YES);
                cmdRule.apiSteps().bannersFakeSteps().setStatusBsSynced(banner.getBid(), Status.YES);
            }
        }

    }

    public static void makeCampSynced(DirectCmdRule cmdRule, Long campaignId) {
        moderateCamp(cmdRule, campaignId);
        syncCamp(cmdRule, campaignId);
    }

    @SafeVarargs
    private static <T> Matcher<String> anyStatusOf(T... statuses) {
        return Stream.of(statuses)
                .map(t -> equalTo(t.toString()))
                .reduce(Matchers::anyOf)
                .orElse(anything());
    }
}
