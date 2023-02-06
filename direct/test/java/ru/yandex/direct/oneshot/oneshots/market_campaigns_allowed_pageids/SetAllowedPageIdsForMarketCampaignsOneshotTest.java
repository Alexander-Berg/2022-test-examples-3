package ru.yandex.direct.oneshot.oneshots.market_campaigns_allowed_pageids;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.bs.resync.queue.model.BsResyncQueueInfo;
import ru.yandex.direct.core.entity.bs.resync.queue.service.BsResyncService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.placements.model.Placement;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@OneshotTest
@RunWith(SpringRunner.class)
public class SetAllowedPageIdsForMarketCampaignsOneshotTest {

    private static final long FIRST_PAGE_ID = 1309141;
    private static final long SECOND_PAGE_ID = 213088;
    private static final long EXISTING_PAGE_ID = 1020;
    private static final String SEARCHAPP_DOMAIN = "ru.yandex.searchplugin";
    private static final Placement ALLOWED_PAGE = new Placement()
            .withIsYandexPage(1L)
            .withPageId(FIRST_PAGE_ID)
            .withDomain(SEARCHAPP_DOMAIN);
    private static final Placement DISALLOWED_PAGE = new Placement()
            .withIsYandexPage(1L)
            .withPageId(SECOND_PAGE_ID)
            .withDomain(SEARCHAPP_DOMAIN);


    @Autowired
    private Steps steps;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private PlacementsRepository placementsRepository;
    @Autowired
    private BsResyncService bsResyncService;
    @Autowired
    private SetAllowedPageIdsForMarketCampaignsOneshot oneshot;

    private List<Long> clientIds;
    private CampaignInfo textCampaignInfo1;
    private CampaignInfo performanceCampaignInfo1;
    private CampaignInfo textCampaignInfo2;
    private CampaignInfo performanceCampaignInfo2;
    private CampaignInfo campaignInfoWithExistingAllowedPageIds;
    private CampaignInfo campaignInfoWithExistingDisallowedPageIds;

    @Before
    public void before() {
        var firstClientInfo = steps.clientSteps().createDefaultClient();
        var secondClientInfo = steps.clientSteps().createDefaultClientAnotherShard();
        clientIds = List.of(firstClientInfo.getClientId().asLong(), secondClientInfo.getClientId().asLong());

        textCampaignInfo1 = steps.campaignSteps().createActiveTextCampaign(firstClientInfo);
        performanceCampaignInfo1 = steps.campaignSteps().createActivePerformanceCampaign(firstClientInfo);
        textCampaignInfo2 = steps.campaignSteps().createActiveTextCampaign(secondClientInfo);
        performanceCampaignInfo2 = steps.campaignSteps().createActivePerformanceCampaign(secondClientInfo);

        campaignInfoWithExistingAllowedPageIds = steps.campaignSteps().createActiveTextCampaign(firstClientInfo);
        campaignRepository.updateAllowedPageIds(
                firstClientInfo.getShard(),
                campaignInfoWithExistingAllowedPageIds.getCampaignId(),
                List.of(EXISTING_PAGE_ID)
        );

        campaignInfoWithExistingDisallowedPageIds = steps
                .campaignSteps()
                .createActivePerformanceCampaign(secondClientInfo);
        campaignRepository.updateDisallowedPageIds(
                secondClientInfo.getShard(),
                campaignInfoWithExistingDisallowedPageIds.getCampaignId(),
                List.of(EXISTING_PAGE_ID)
        );

        placementsRepository.insertPlacements(List.of(ALLOWED_PAGE, DISALLOWED_PAGE));
    }

    @Test
    public void test() {
        //на вход подаем две текстовые и одну смарт кампанию
        var campaignIdsForInput = List.of(
                textCampaignInfo1.getCampaignId(),
                performanceCampaignInfo2.getCampaignId(),
                campaignInfoWithExistingAllowedPageIds.getCampaignId());
        //тогда две кампании содержат только новую страницу
        var campaignIdsWithNewAllowedPages = List.of(textCampaignInfo1.getCampaignId(),
                performanceCampaignInfo2.getCampaignId());
        //запрещенные страницы проставляются для двух смарт кампаний (первого клиента и той, что уже имеет запрещенные страницы в дб)
        var campaignIdsWithNewDisallowedPages = List.of(performanceCampaignInfo1.getCampaignId());
        var campaignIdsWithAllDisallowedPages =  List.of(
                performanceCampaignInfo1.getCampaignId(),
                campaignInfoWithExistingDisallowedPageIds.getCampaignId()
        );
        var unaffectedCampaigns = List.of(textCampaignInfo2.getCampaignId());

        oneshot.execute(new InputData(
                        clientIds,
                        campaignIdsForInput,
                        List.of(ALLOWED_PAGE.getPageId()),
                        List.of(DISALLOWED_PAGE.getPageId())),
                null);

        SoftAssertions soft = new SoftAssertions();
        campaignIdsWithNewAllowedPages.forEach(cid -> checkCampaignWithAllowedPages(cid, soft));
        campaignIdsWithNewDisallowedPages.forEach(cid -> checkCampaignWithDisallowedPages(cid, soft));
        unaffectedCampaigns.forEach(cid -> checkCampaignWithNoChanges(cid, soft));

        checkCampaignWithExistingAllowedPages(campaignInfoWithExistingAllowedPageIds.getCampaignId(), soft);
        checkCampaignWithExistingDisallowedPages(campaignInfoWithExistingDisallowedPageIds.getCampaignId(), soft);

        List<Long> addedToBsCids = new ArrayList<>();
        addedToBsCids.addAll(campaignIdsForInput);
        addedToBsCids.addAll(campaignIdsWithAllDisallowedPages);
        checkAddedToBsResyncQueue(addedToBsCids, soft);
        soft.assertAll();
    }

    void checkCampaignWithAllowedPages(Long campaignId, SoftAssertions soft) {
        var allowedAndDisallowedPages = getAllowedAndDisallowedPageIds(campaignId);
        soft.assertThat(allowedAndDisallowedPages.getLeft())
                .as("Campaign " + campaignId + " contains all allowed page ids")
                .containsExactly(ALLOWED_PAGE.getPageId());

        soft.assertThat(allowedAndDisallowedPages.getRight())
                .as("Campaign " + campaignId + " has no disallowed page ids")
                .isEmpty();
    }

    void checkCampaignWithDisallowedPages(Long campaignId, SoftAssertions soft) {
        var allowedAndDisallowedPages = getAllowedAndDisallowedPageIds(campaignId);
        soft.assertThat(allowedAndDisallowedPages.getLeft())
                .as("Campaign " + campaignId + " contains no allowed page ids")
                .isEmpty();

        soft.assertThat(allowedAndDisallowedPages.getRight())
                .as("Campaign " + campaignId + " contains all disallowed page ids")
                .containsExactly(DISALLOWED_PAGE.getPageId());
    }

    void checkCampaignWithNoChanges(Long campaignId, SoftAssertions soft) {
        var allowedAndDisallowedPages = getAllowedAndDisallowedPageIds(campaignId);
        soft.assertThat(allowedAndDisallowedPages.getLeft())
                .as("Campaign " + campaignId + " contains no allowed page ids")
                .isEmpty();

        soft.assertThat(allowedAndDisallowedPages.getRight())
                .as("Campaign " + campaignId + " has no disallowed page ids")
                .isEmpty();
    }

    void checkCampaignWithExistingAllowedPages(Long campaignId, SoftAssertions soft) {
        var allowedAndDisallowedPages = getAllowedAndDisallowedPageIds(campaignId);
        soft.assertThat(allowedAndDisallowedPages.getLeft())
                .as("Campaign " + campaignId + " contains new and previous allowed page ids")
                .containsOnly(EXISTING_PAGE_ID, ALLOWED_PAGE.getPageId());
    }

    void checkCampaignWithExistingDisallowedPages(Long campaignId, SoftAssertions soft) {
        var allowedAndDisallowedPages = getAllowedAndDisallowedPageIds(campaignId);
        soft.assertThat(allowedAndDisallowedPages.getRight())
                .as("Campaign " + campaignId + " contains new and previous disallowed page ids")
                .containsOnly(EXISTING_PAGE_ID, DISALLOWED_PAGE.getPageId());
    }

    Pair<List<Long>, List<Long>> getAllowedAndDisallowedPageIds(long campaignId) {
        return campaignRepository.getCampaignsAllowedAndDisallowedPageIdsMap(
                        shardHelper.getShardByCampaignId(campaignId),
                        List.of(campaignId))
                .get(campaignId);
    }

    void checkAddedToBsResyncQueue(List<Long> campaignIds, SoftAssertions soft) {
        var resyncInfo = bsResyncService.getBsResyncItemsByCampaignIds(campaignIds);
        soft.assertThat(mapList(resyncInfo, BsResyncQueueInfo::getCampaignId))
                .containsOnlyElementsOf(campaignIds);
    }
}
