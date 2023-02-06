package ru.yandex.direct.core.entity.banner.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TextBannerUpdateServiceTest {
    private static final CompareStrategy STRATEGY = DefaultCompareStrategies.allFieldsExcept(newPath("lastChange"));

    @Autowired
    private Steps steps;
    @Autowired
    private BannerCreativeRepository newBannerCreativeRepository;
    @Autowired
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;
    @Autowired
    private BannerTypedRepository bannerRepository;

    private long operatorUid;
    private ClientId clientId;
    private int shard;

    private long creativeId1;
    private long creativeId2;


    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        BannerCreativeInfo<OldTextBanner> bannerCreativeInfo1 =
                steps.bannerCreativeSteps().createTextBannerCreative(clientInfo);
        creativeId1 = bannerCreativeInfo1.getCreativeId();

        BannerCreativeInfo<OldTextBanner> bannerCreativeInfo2 =
                steps.bannerCreativeSteps().createTextBannerCreative(clientInfo);
        creativeId2 = bannerCreativeInfo2.getCreativeId();
    }

    @Test
    public void createModelChangesToReplaceCreative_Valid() {
        List<ModelChanges<TextBanner>> modelChangesList =
                createModelChangesToReplaceCreative(creativeId1, creativeId2);

        ModelChanges<TextBanner> modelChanges = modelChangesList.get(0);
        assertThat("значение creative id должно измениться", modelChanges.getChangedProp(TextBanner.CREATIVE_ID),
                is(creativeId2));
    }

    @Test
    public void updateBannerPartial_Valid() {
        TextBanner bannerWithCreative = getBannerByCreativeId(creativeId1);

        TextBanner expectedBanner = bannerWithCreative
                .withStatusModerate(BannerStatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withCreativeId(creativeId2)
                .withCreativeStatusModerate(BannerCreativeStatusModerate.READY);

        List<ModelChanges<TextBanner>> textBannerModelChangesList =
                createModelChangesToReplaceCreative(creativeId1, creativeId2);

        List<ModelChanges<BannerWithSystemFields>> modelChangesList = mapList(textBannerModelChangesList,
                x -> x.castModelUp(BannerWithSystemFields.class));

        MassResult<Long> result =
                bannersUpdateOperationFactory.createPartialUpdateOperation(modelChangesList, operatorUid, clientId)
                        .prepareAndApply();
        assertThat(result, isFullySuccessful());

        TextBanner actualBanner = getBanner(bannerWithCreative.getId());
        assertThat(actualBanner, beanDiffer(expectedBanner).useCompareStrategy(STRATEGY));
    }


    private List<ModelChanges<TextBanner>> createModelChangesToReplaceCreative(Long creativeId, Long newCreativeId) {
        List<Long> bannerIds =
                newBannerCreativeRepository.getBannerPerformanceBannerIds(shard, singletonList(creativeId));

        return bannerIds.stream()
                .map(bid -> new ModelChanges<>(bid, TextBanner.class)
                        .process(newCreativeId, TextBanner.CREATIVE_ID))
                .collect(toList());
    }

    private TextBanner getBannerByCreativeId(Long creativeId) {
        Long bannerId =
                newBannerCreativeRepository.getBannerPerformanceBannerIds(shard, singletonList(creativeId)).get(0);

        return getBanner(bannerId);
    }

    private TextBanner getBanner(Long bannerId) {
        List<TextBanner> banners = bannerRepository.getSafely(shard, singletonList(bannerId), TextBanner.class);
        return banners.get(0);
    }
}
