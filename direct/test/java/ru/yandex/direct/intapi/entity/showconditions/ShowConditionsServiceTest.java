package ru.yandex.direct.intapi.entity.showconditions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilter;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.showconditions.model.request.KeywordModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.ShowConditionsRequest;
import ru.yandex.direct.intapi.entity.showconditions.service.ShowConditionsService;
import ru.yandex.direct.web.core.model.WebResponse;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class ShowConditionsServiceTest {

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private ShowConditionsService showConditionsService;

    private AdGroupInfo adGroupInfo;
    private long adGroupId;
    private Long uid;
    private ClientId clientId;
    private int shard;

    private KeywordInfo keywordInfo;
    private AdGroupBidModifierInfo bidModifierInfo;

    private static final String DEFAULT_PHRASE = "default phrase";
    private static final BigDecimal INITIAL_PRICE = BigDecimal.valueOf(12).setScale(2, BigDecimal.ROUND_UNNECESSARY);

    @Before
    public void before() {
        Keyword keyword = keywordWithText(DEFAULT_PHRASE)
                .withPrice(INITIAL_PRICE)
                .withPriceContext(INITIAL_PRICE);
        keywordInfo = steps.keywordSteps().createKeyword(keyword);

        adGroupInfo = keywordInfo.getAdGroupInfo();
        adGroupId = adGroupInfo.getAdGroupId();

        var retCondInfo = steps.retConditionSteps().createDefaultRetCondition();
        bidModifierInfo = steps.bidModifierSteps()
                .createAdGroupBidModifierRetargetingFilterWithRetCondIds(
                        adGroupInfo,
                        List.of(retCondInfo.getRetConditionId())
                );

        // для успешного ответа из торгов, должен быть задан баннер в группе, иначе результат будет пустой.
        steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);

        shard = keywordInfo.getShard();
        uid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId();

        adGroupRepository.updateStatusBsSynced(shard, singletonList(adGroupId), StatusBsSynced.YES);
    }

    @Test
    public void doOperations_EmptyRequest_AdGroupStatusBsSyncedNotReset() {
        ShowConditionsRequest request = new ShowConditionsRequest();

        WebResponse response = showConditionsService.doOperations(uid, clientId, request);
        assumeTrue(response.isSuccessful());

        adGroupInfo.getAdGroupId();
        AdGroup savedAdGoup = adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);

        assertThat(savedAdGoup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    // при изменении условий показа должен быть сброс statusBsSynced группы.
    // В перле на это было отдельное условие, в java реализация обновления статуса зашита в каждую операцию,
    // проверка остается для общей валидации сервиса showConditions.
    @Test
    public void doOperations_DeleteKeyword_LastCondiion_AdGroupStatusBsSyncedReset() {
        ShowConditionsRequest request = new ShowConditionsRequest()
                .withKeywords(singletonMap(adGroupId,
                        new KeywordModificationContainer().withDeleted(singletonList(keywordInfo.getId()))));

        WebResponse response = showConditionsService.doOperations(uid, clientId, request);
        assumeTrue(response.isSuccessful());

        adGroupInfo.getAdGroupId();
        AdGroup savedAdGoup = adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);

        assertThat(savedAdGoup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void doOperations_DeleteSearchRetargetings_AdGroupStatusBsSyncedReset() {
        BidModifierRetargetingFilter modifier = (BidModifierRetargetingFilter) bidModifierInfo.getBidModifier();
        ShowConditionsRequest request = new ShowConditionsRequest()
                .withSearchRetargetings(singletonMap(adGroupId,
                        new RetargetingModificationContainer().withDeleted(
                                mapList(modifier.getRetargetingAdjustments(),
                                        AbstractBidModifierRetargetingAdjustment::getId)
                        )));
       WebResponse response = showConditionsService.doOperations(uid, clientId, request);
       assumeTrue(response.isSuccessful());

        adGroupInfo.getAdGroupId();
        AdGroup savedAdGoup = adGroupRepository.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);

        assertThat(savedAdGoup.getStatusBsSynced(), is(StatusBsSynced.NO));
    }
}
