package ru.yandex.direct.core.entity.campaign.repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignsPromotion;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignsPromotionsRepositoryTest {

    private static final int SHARD = 1;

    private static final Long CID_1 = 11L;
    private static final Long PROMOTION_ID_1 = 1L;
    private static final LocalDate START_1 = LocalDate.of(2021, 11, 1);
    private static final LocalDate FINISH_1 = LocalDate.of(2021, 11, 2);
    private static final Long PERCENT_1 = 10L;

    private static final Long CID_2 = 12L;
    private static final Long PROMOTION_ID_2 = 2L;
    private static final LocalDate START_2 = LocalDate.of(2021, 11, 3);
    private static final LocalDate FINISH_2 = LocalDate.of(2021, 11, 4);
    private static final Long PERCENT_2 = 20L;

    private static final Long PROMOTION_ID_3 = 3L;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignsPromotionsRepository campaignsPromotionsRepository;

    @Parameterized.Parameter(0)
    public Map<Long, List<CampaignsPromotion>> campaignsPromotionsBeforeUpdate;

    @Parameterized.Parameter(1)
    public Map<Long, List<CampaignsPromotion>> campaignsPromotionsForUpdate;

    @Parameterized.Parameter(2)
    public Map<Long, List<CampaignsPromotion>> campaignsPromotionsAfterUpdate;

    @Parameterized.Parameters()
    public static Collection<Object[]> typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_1)
                                .withPromotionId(PROMOTION_ID_1)
                                .withStart(START_1)
                                .withFinish(FINISH_1)
                                .withPercent(PERCENT_1))),
                        Map.of(CID_2, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_2)
                                .withPromotionId(PROMOTION_ID_2)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2))),
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1)),
                                CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)))
                },
                {
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_1)
                                .withPromotionId(PROMOTION_ID_1)
                                .withStart(START_1)
                                .withFinish(FINISH_1)
                                .withPercent(PERCENT_1))),
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_1)
                                .withPromotionId(PROMOTION_ID_1)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2))),
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_1)
                                .withPromotionId(PROMOTION_ID_1)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2)))
                },
                {
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_1)
                                .withPromotionId(PROMOTION_ID_1)
                                .withStart(START_1)
                                .withFinish(FINISH_1)
                                .withPercent(PERCENT_1))),
                        Map.of(CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)),
                                CID_1, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2))),
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)),
                                CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)))
                },
                {
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_1)
                                .withPromotionId(PROMOTION_ID_1)
                                .withStart(START_1)
                                .withFinish(FINISH_1)
                                .withPercent(PERCENT_1))),
                        Map.of(CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)),
                                CID_1, Collections.emptyList()),
                        Map.of(CID_2, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_2)
                                .withPromotionId(PROMOTION_ID_2)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2)))
                },
                {
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_1)
                                .withPromotionId(PROMOTION_ID_1)
                                .withStart(START_1)
                                .withFinish(FINISH_1)
                                .withPercent(PERCENT_1))),
                        Map.of(CID_1, Collections.emptyList()),
                        emptyMap()
                },
                {
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1)),
                                CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1))),
                        Map.of(CID_2, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_2)
                                .withPromotionId(PROMOTION_ID_2)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2))),
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1)),
                                CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)))
                },
                {
                        Map.of(CID_1, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1)),
                                CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1))),
                        Map.of(CID_1, Collections.emptyList(),
                                CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2))),
                        Map.of(CID_2, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_2)
                                .withPromotionId(PROMOTION_ID_2)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2)))
                },
                {
                        Map.of(CID_1, Arrays.asList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1),
                                new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_3)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2))),
                        Map.of(CID_2, Collections.singletonList(new CampaignsPromotion()
                                .withCid(CID_2)
                                .withPromotionId(PROMOTION_ID_2)
                                .withStart(START_2)
                                .withFinish(FINISH_2)
                                .withPercent(PERCENT_2))),
                        Map.of(CID_1, Arrays.asList(new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_1)
                                        .withStart(START_1)
                                        .withFinish(FINISH_1)
                                        .withPercent(PERCENT_1),
                                new CampaignsPromotion()
                                        .withCid(CID_1)
                                        .withPromotionId(PROMOTION_ID_3)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)),
                                CID_2, Collections.singletonList(new CampaignsPromotion()
                                        .withCid(CID_2)
                                        .withPromotionId(PROMOTION_ID_2)
                                        .withStart(START_2)
                                        .withFinish(FINISH_2)
                                        .withPercent(PERCENT_2)))
                },
        });
    }

    @Before
    public void setup() {
        campaignsPromotionsRepository.updateCampaignsPromotions(SHARD,
                Map.of(CID_1, Collections.emptyList(), CID_2, Collections.emptyList()));
    }

    @Test
    public void checkUpdateAndGetCampaignsPromotions() {
        campaignsPromotionsRepository.updateCampaignsPromotions(SHARD, campaignsPromotionsBeforeUpdate);

        Map<Long, List<CampaignsPromotion>> campaignsPromotions =
                campaignsPromotionsRepository.getCampaignsPromotionsByCid(SHARD, Arrays.asList(CID_1, CID_2));

        assertThat("проверяем, что в базе сохранились корректные промоакции",
                campaignsPromotions, beanDiffer(campaignsPromotionsBeforeUpdate));

        campaignsPromotionsRepository.updateCampaignsPromotions(SHARD, campaignsPromotionsForUpdate);

        campaignsPromotions =
                campaignsPromotionsRepository.getCampaignsPromotionsByCid(SHARD, Arrays.asList(CID_1, CID_2));

        assertThat("проверяем, что в базе сохранились корректные промоакции",
                campaignsPromotions, beanDiffer(campaignsPromotionsAfterUpdate));
    }
}
