package ru.yandex.direct.grid.processing.service.campaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignsPromotion;
import ru.yandex.direct.core.entity.campaign.repository.CampaignsPromotionsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsPromotion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsPromotions;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationServiceUpdateCampaignsPromotionsTest {

    private static final LocalDate START_1 = LocalDate.of(2021, 11, 1);
    private static final LocalDate FINISH_1 = LocalDate.of(2021, 11, 2);
    private static final Long PERCENT_1 = 10L;

    private static final LocalDate START_2 = LocalDate.of(2021, 11, 3);
    private static final LocalDate FINISH_2 = LocalDate.of(2021, 11, 4);
    private static final Long PERCENT_2 = 20L;

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignMutationService campaignMutationService;
    @Autowired
    private CampaignsPromotionsRepository campaignsPromotionsRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Long uid;
    private ClientId clientId;
    private int shard;

    private Long campaignId;
    private Long campaignIdElse;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
        shard = clientInfo.getShard();

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru"),
                clientInfo);
        campaignId = campaignInfo.getCampaignId();

        CampaignInfo campaignInfoElse = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru"),
                clientInfo);
        campaignIdElse = campaignInfoElse.getCampaignId();
        campaignsPromotionsRepository.updateCampaignsPromotions(shard,
                Map.of(campaignId, Collections.emptyList(), campaignIdElse, Collections.emptyList()));
    }

    @Test
    public void createOnePromotionInOneCampaign() {
        Long promotionId = (long) RandomUtils.nextInt();
        List<GdCampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new GdCampaignsPromotion()
                .withCid(campaignId)
                .withPromotionId(promotionId)
                .withPercent(PERCENT_1)
                .withStart(START_1)
                .withFinish(FINISH_1));
        updateAndAssertResult(List.of(campaignId), campaignsPromotions);
        Map<Long, List<CampaignsPromotion>> campaignsPromotionsAfterUpdate = Map.of(
                campaignId, Collections.singletonList(new CampaignsPromotion()
                        .withCid(campaignId)
                        .withPromotionId(promotionId)
                        .withStart(START_1)
                        .withFinish(FINISH_1)
                        .withPercent(PERCENT_1))
        );
        Map<Long, List<CampaignsPromotion>> dbCampaignsPromotions =
                campaignsPromotionsRepository.getCampaignsPromotionsByCid(shard, Collections.singletonList(campaignId));

        Assert.assertThat("проверяем, что в базе сохранились корректные промоакции",
                dbCampaignsPromotions, beanDiffer(campaignsPromotionsAfterUpdate));
    }

    @Test
    public void createTwoPromotionsInOneCampaign() {
        Long promotionId1 = (long) RandomUtils.nextInt();
        Long promotionId2 = promotionId1 + 1;
        List<GdCampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new GdCampaignsPromotion()
                .withCid(campaignId)
                .withPromotionId(promotionId1)
                .withPercent(PERCENT_1)
                .withStart(START_1)
                .withFinish(FINISH_1));
        campaignsPromotions.add(new GdCampaignsPromotion()
                .withCid(campaignId)
                .withPromotionId(promotionId2)
                .withPercent(PERCENT_2)
                .withStart(START_2)
                .withFinish(FINISH_2));
        updateAndAssertResult(List.of(campaignId), campaignsPromotions);
        Map<Long, List<CampaignsPromotion>> campaignsPromotionsAfterUpdate = Map.of(
                campaignId, Arrays.asList(new CampaignsPromotion()
                                .withCid(campaignId)
                                .withPromotionId(promotionId1)
                                .withStart(START_1)
                                .withFinish(FINISH_1)
                                .withPercent(PERCENT_1),
                        new CampaignsPromotion()
                                .withCid(campaignId)
                                .withPromotionId(promotionId2)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2))
        );
        Map<Long, List<CampaignsPromotion>> dbCampaignsPromotions =
                campaignsPromotionsRepository.getCampaignsPromotionsByCid(shard, Collections.singletonList(campaignId));

        Assert.assertThat("проверяем, что в базе сохранились корректные промоакции",
                dbCampaignsPromotions, beanDiffer(campaignsPromotionsAfterUpdate));
    }

    @Test
    public void twoPromotionsInTwoCampaigns() {
        Long promotionId1 = (long) RandomUtils.nextInt();
        Long promotionId2 = promotionId1 + 1;

        //Просто создаем две промоакции в двух кампаниях

        List<GdCampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new GdCampaignsPromotion()
                .withCid(campaignId)
                .withPromotionId(promotionId1)
                .withPercent(PERCENT_1)
                .withStart(START_1)
                .withFinish(FINISH_1));
        campaignsPromotions.add(new GdCampaignsPromotion()
                .withCid(campaignIdElse)
                .withPromotionId(promotionId2)
                .withPercent(PERCENT_1)
                .withStart(START_1)
                .withFinish(FINISH_1));

        updateAndAssertResult(List.of(campaignId, campaignIdElse), campaignsPromotions);
        Map<Long, List<CampaignsPromotion>> campaignsPromotionsAfterUpdate = Map.of(
                campaignId, Collections.singletonList(new CampaignsPromotion()
                        .withCid(campaignId)
                        .withPromotionId(promotionId1)
                        .withStart(START_1)
                        .withFinish(FINISH_1)
                        .withPercent(PERCENT_1)),
                campaignIdElse, Collections.singletonList(new CampaignsPromotion()
                        .withCid(campaignIdElse)
                        .withPromotionId(promotionId2)
                        .withStart(START_1)
                        .withFinish(FINISH_1)
                        .withPercent(PERCENT_1))
        );
        Map<Long, List<CampaignsPromotion>> dbCampaignsPromotions =
                campaignsPromotionsRepository.getCampaignsPromotionsByCid(shard, Arrays.asList(campaignId,
                        campaignIdElse));

        Assert.assertThat("проверяем, что в базе сохранились корректные промоакции",
                dbCampaignsPromotions, beanDiffer(campaignsPromotionsAfterUpdate));

        //Далее обновляем промоакцию только в одной кампании, в campaignsIds тоже только одна кампания.
        //Промоакция во второй кампании должна остаться в базе

        campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new GdCampaignsPromotion()
                .withCid(campaignId)
                .withPromotionId(promotionId1)
                .withPercent(PERCENT_1)
                .withStart(START_1)
                .withFinish(FINISH_1));

        updateAndAssertResult(List.of(campaignId), campaignsPromotions);
        campaignsPromotionsAfterUpdate = Map.of(
                campaignId, Collections.singletonList(new CampaignsPromotion()
                        .withCid(campaignId)
                        .withPromotionId(promotionId1)
                        .withStart(START_1)
                        .withFinish(FINISH_1)
                        .withPercent(PERCENT_1)),
                campaignIdElse, Collections.singletonList(new CampaignsPromotion()
                        .withCid(campaignIdElse)
                        .withPromotionId(promotionId2)
                        .withStart(START_1)
                        .withFinish(FINISH_1)
                        .withPercent(PERCENT_1))
        );
        dbCampaignsPromotions =
                campaignsPromotionsRepository.getCampaignsPromotionsByCid(shard, Arrays.asList(campaignId,
                        campaignIdElse));

        Assert.assertThat("проверяем, что в базе сохранились корректные промоакции",
                dbCampaignsPromotions, beanDiffer(campaignsPromotionsAfterUpdate));

        //Далее обновляем промоакцию только в одной кампании, но в campaignsIds будут обе кампании.
        //Промоакция во второй кампании удалиться

        updateAndAssertResult(List.of(campaignId, campaignIdElse), campaignsPromotions);
        campaignsPromotionsAfterUpdate = Map.of(
                campaignId, Collections.singletonList(new CampaignsPromotion()
                        .withCid(campaignId)
                        .withPromotionId(promotionId1)
                        .withStart(START_1)
                        .withFinish(FINISH_1)
                        .withPercent(PERCENT_1))
        );
        dbCampaignsPromotions =
                campaignsPromotionsRepository.getCampaignsPromotionsByCid(shard, Arrays.asList(campaignId,
                        campaignIdElse));

        Assert.assertThat("проверяем, что в базе сохранились корректные промоакции",
                dbCampaignsPromotions, beanDiffer(campaignsPromotionsAfterUpdate));
    }


    private void updateAndAssertResult(List<Long> campaignIds, List<GdCampaignsPromotion> campaignsPromotions) {
        var result = updateCampaignsPromotions(campaignIds, campaignsPromotions);
        assertCopyResult(result, campaignIds);
    }

    private GdUpdateCampaignPayload updateCampaignsPromotions(List<Long> campaignIds,
                                                              List<GdCampaignsPromotion> campaignsPromotions) {
        return campaignMutationService.updateCampaignsPromotions(
                new GridGraphQLContext(new User().withUid(uid), new User().withClientId(clientId).withUid(uid)),
                new GdUpdateCampaignsPromotions()
                        .withCampaignIds(campaignIds)
                        .withCampaignsPromotions(campaignsPromotions));
    }

    private void assertCopyResult(GdUpdateCampaignPayload result, List<Long> campaignIds) {
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(result.getValidationResult()).isNull();
            softAssertions.assertThat(mapList(result.getUpdatedCampaigns(), GdUpdateCampaignPayloadItem::getId))
                    .isEqualTo(campaignIds);
        });
    }
}
