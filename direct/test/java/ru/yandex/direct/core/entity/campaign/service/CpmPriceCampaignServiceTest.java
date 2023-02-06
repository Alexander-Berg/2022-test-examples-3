package ru.yandex.direct.core.entity.campaign.service;

import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpmPriceCampaignServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CpmPriceCampaignService cpmPriceCampaignService;

    private ClientInfo defaultClient;
    private PricePackage defaultPricePackage;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        defaultPricePackage =
                steps.pricePackageSteps().createApprovedPricePackageWithClients(defaultClient).getPricePackage();
    }

    @Test
    public void getCpmPriceCampaignsWaitingApproveByShard_Draft_NoGroups() {
        CpmPriceCampaign newCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                        .withStatusModerate(CampaignStatusModerate.NEW));

        Map<Integer, List<CpmPriceCampaign>> foundCampaigns =
                cpmPriceCampaignService.getCpmPriceCampaignsWaitingApproveByShard();
        Map<Integer, List<Long>> notexpectedCampaigns =
                Map.of(defaultClient.getShard(), List.of(newCampaign.getId()));
        assertNotContainCampaigns(foundCampaigns, notexpectedCampaigns);
    }

    @Test
    public void filterCpmPriceCampaignsWaitingApproveForValidation_Draft_NoGroups() {
        CpmPriceCampaign newCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                        .withStatusModerate(CampaignStatusModerate.NEW));

        List<CpmPriceCampaign> foundCampaigns =
                cpmPriceCampaignService.filterCpmPriceCampaignsEligibleForApprove(defaultClient.getShard(),
                        List.of(newCampaign));

        assertThat(foundCampaigns.stream()
                .anyMatch(foundCampaign -> foundCampaign.getId().equals(newCampaign.getId())))
                .isFalse();
    }

    @Test
    public void filterCpmPriceCampaignsWaitingApproveForValidation_ArchivedGroup() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW));

        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign,
                defaultClient);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(defaultClient, campaign);
        OldCpmBanner banner = activeCpmBanner(campaign.getId(), adGroup.getId(), creativeInfo.getCreativeId())
                .withStatusArchived(true);
        steps.bannerSteps().createActiveCpmBannerRaw(defaultClient.getShard(), banner, adGroup);

        List<CpmPriceCampaign> foundCampaigns =
                cpmPriceCampaignService.filterCpmPriceCampaignsEligibleForApprove(defaultClient.getShard(),
                        List.of(campaign));
        assertThat(foundCampaigns.stream()
                .anyMatch(foundCampaign -> foundCampaign.getId().equals(campaign.getId())))
                .isFalse();
    }


    @Test
    public void filterCpmPriceCampaignsWaitingApproveForValidation_NotArchivedGroup() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW));

        steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, defaultClient);

        List<CpmPriceCampaign> foundCampaigns =
                cpmPriceCampaignService.filterCpmPriceCampaignsEligibleForApprove(defaultClient.getShard(),
                        List.of(campaign));
        assertThat(foundCampaigns.stream()
                .anyMatch(foundCampaign -> foundCampaign.getId().equals(campaign.getId())))
                .isTrue();
    }

    @Test
    public void getCpmPriceCampaignsWaitingApproveByShard_differentStatusApprove() {
        CpmPriceCampaign newCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW));
        CpmPriceCampaign approvedCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.YES));
        CpmPriceCampaign rejectedCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NO));

        steps.adGroupSteps().createDefaultAdGroupForPriceSales(newCampaign, defaultClient);
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(approvedCampaign, defaultClient);
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(rejectedCampaign, defaultClient);

        Map<Integer, List<CpmPriceCampaign>> foundCampaigns =
                cpmPriceCampaignService.getCpmPriceCampaignsWaitingApproveByShard();
        Map<Integer, List<Long>> expectedCampaigns =
                Map.of(defaultClient.getShard(), List.of(newCampaign.getId()));
        Map<Integer, List<Long>> notexpectedCampaigns =
                Map.of(defaultClient.getShard(), List.of(approvedCampaign.getId(), rejectedCampaign.getId()));
        assertContainCampaigns(foundCampaigns, expectedCampaigns);
        assertNotContainCampaigns(foundCampaigns, notexpectedCampaigns);
    }

    @Test
    public void getCpmPriceCampaignsWaitingApproveByShard_differentStatusEmpty() {
        CpmPriceCampaign newCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW));
        CpmPriceCampaign deletedCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                        .withStatusEmpty(true));

        steps.adGroupSteps().createDefaultAdGroupForPriceSales(newCampaign, defaultClient);
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(deletedCampaign, defaultClient);

        Map<Integer, List<CpmPriceCampaign>> foundCampaigns =
                cpmPriceCampaignService.getCpmPriceCampaignsWaitingApproveByShard();
        Map<Integer, List<Long>> expectedCampaigns =
                Map.of(defaultClient.getShard(), List.of(newCampaign.getId()));
        Map<Integer, List<Long>> notexpectedCampaigns =
                Map.of(defaultClient.getShard(), List.of(deletedCampaign.getId()));
        assertContainCampaigns(foundCampaigns, expectedCampaigns);
        assertNotContainCampaigns(foundCampaigns, notexpectedCampaigns);
    }

    @Test
    public void getCpmPriceCampaignsWaitingApproveByShard_differentShard() {
        ClientInfo client1 = steps.clientSteps().createDefaultClient();
        ClientInfo client2 = steps.clientSteps().createDefaultClientAnotherShard();

        PricePackage pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(client1, client2)
                .getPricePackage();

        CpmPriceCampaign newCampaign1 = steps.campaignSteps().createActiveCpmPriceCampaign(client1,
                defaultCpmPriceCampaignWithSystemFields(client1, pricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW));
        CpmPriceCampaign newCampaign2 = steps.campaignSteps().createActiveCpmPriceCampaign(client2,
                defaultCpmPriceCampaignWithSystemFields(client2, pricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW));

        steps.adGroupSteps().createDefaultAdGroupForPriceSales(newCampaign1, client1);
        steps.adGroupSteps().createDefaultAdGroupForPriceSales(newCampaign2, client2);

        Map<Integer, List<CpmPriceCampaign>> foundCampaigns =
                cpmPriceCampaignService.getCpmPriceCampaignsWaitingApproveByShard();
        Map<Integer, List<Long>> expectedCampaigns = Map.of(
                client1.getShard(), List.of(newCampaign1.getId()),
                client2.getShard(), List.of(newCampaign2.getId())
        );
        assertContainCampaigns(foundCampaigns, expectedCampaigns);
    }

    @Test
    public void getCpmPriceCampaignsWaitingApproveByShard_DraftCampaigns() {
        CpmPriceCampaign draftCampaignNoDefaultAdgroup = steps.campaignSteps().createActiveCpmPriceCampaign(
                defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                        .withIsDraftApproveAllowed(true)
                        .withStatusModerate(CampaignStatusModerate.NEW));
        CpmPriceCampaign draftCampaignWithDefaultAdgroup = steps.campaignSteps().createActiveCpmPriceCampaign(
                defaultClient,
                defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                        .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                        .withIsDraftApproveAllowed(true)
                        .withStatusModerate(CampaignStatusModerate.NEW));
        CpmYndxFrontpageAdGroup defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(
                draftCampaignWithDefaultAdgroup, defaultClient);

        Map<Integer, List<CpmPriceCampaign>> foundCampaigns =
                cpmPriceCampaignService.getCpmPriceCampaignsWaitingApproveByShard();
        Map<Integer, List<Long>> expectedCampaigns =
                Map.of(defaultClient.getShard(), List.of(draftCampaignWithDefaultAdgroup.getId()));
        Map<Integer, List<Long>> notexpectedCampaigns =
                Map.of(defaultClient.getShard(), List.of(draftCampaignNoDefaultAdgroup.getId()));
        assertContainCampaigns(foundCampaigns, expectedCampaigns);
        assertNotContainCampaigns(foundCampaigns, notexpectedCampaigns);
    }

    private void assertContainCampaigns(Map<Integer, List<CpmPriceCampaign>> actualCampaigns,
                                        Map<Integer, List<Long>> expectedContainCampaignIdsByShard) {
        SoftAssertions softly = new SoftAssertions();
        EntryStream.of(expectedContainCampaignIdsByShard)
                .forKeyValue((shard, expectedContainCampaignIdsInShard) -> {
                    List<Long> actualCampaignIdsInShard = collectCampaignIdsInShard(actualCampaigns, shard);
                    softly.assertThat(actualCampaignIdsInShard).containsAll(expectedContainCampaignIdsInShard);
                });
        softly.assertAll();
    }

    private void assertNotContainCampaigns(Map<Integer, List<CpmPriceCampaign>> actualCampaigns,
                                           Map<Integer, List<Long>> expectedNotContainCampaignIdsByShard) {
        SoftAssertions softly = new SoftAssertions();
        EntryStream.of(expectedNotContainCampaignIdsByShard)
                .forKeyValue((shard, expectedNotContainCampaignIdsInShard) -> {
                    List<Long> actualCampaignIdsInShard = collectCampaignIdsInShard(actualCampaigns, shard);
                    softly.assertThat(actualCampaignIdsInShard)
                            .doesNotContainAnyElementsOf(expectedNotContainCampaignIdsInShard);
                });
        softly.assertAll();
    }

    private List<Long> collectCampaignIdsInShard(Map<Integer, List<CpmPriceCampaign>> campaignsByShard, int shard) {
        List<CpmPriceCampaign> campaignsInShard = campaignsByShard.getOrDefault(shard, emptyList());
        return mapList(campaignsInShard, CpmPriceCampaign::getId);
    }

}
