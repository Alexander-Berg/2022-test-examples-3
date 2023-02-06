package ru.yandex.direct.core.entity.banner.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.stringShouldNotBeBlank;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersUpdateOperationTest {
    @Autowired
    public ShardHelper shardHelper;

    @Autowired
    private BannerTypedRepository repository;

    @Autowired
    private BannersUpdateOperationFactory updateOperationFactory;

    @Autowired
    public FeatureService featureService;

    @Autowired
    public TestContentPromotionBanners contentPromotionBanners;

    @Autowired
    private Steps steps;

    private long clientUid;
    private ClientId clientId;
    private long adGroupId;
    private int shard;
    private Long contentPromotionId;
    private ContentPromotionAdGroupInfo adGroupInfo;
    private ContentPromotionBannerInfo contentPromotion;

    @Before
    public void before() {
        adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        clientUid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId();
        adGroupId = adGroupInfo.getAdGroupId();
        shard = adGroupInfo.getShard();

        var contentPromotionContent = steps.contentPromotionSteps()
                .createContentPromotionContent(clientId, ContentPromotionContentType.VIDEO);
        contentPromotionId = contentPromotionContent.getId();
        contentPromotion = steps.contentPromotionBannerSteps().createBanner(
                new ContentPromotionBannerInfo()
                        .withBanner(contentPromotionBanners.fullContentPromoBanner(contentPromotionId,
                                "https://www.youtube.com"))
                        .withContent(contentPromotionContent)
                        .withAdGroupInfo(adGroupInfo));
    }

    // правильность результата

    @Test
    public void prepareAndApply_PartialYes_OneInvalidBannerOperation_ItemResultIsFailed() {
        ModelChanges<ContentPromotionBanner> change = getContentPromotionModelChanges(contentPromotion.getBannerId())
                .process("", ContentPromotionBanner.TITLE);

        List<ModelChanges<BannerWithSystemFields>> changes = singletonList(castUp(change));

        MassResult<Long> massResult = createPartialUpdateOperation(changes).prepareAndApply();
        assertThat(massResult, isSuccessful(false));

        Result<Long> itemResult = massResult.getResult().get(0);
        assertThat(itemResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(field("title")), stringShouldNotBeBlank())));
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidBannerOperation_OneOfItemsResultsIsFailed() {
        Long contentPromotionId2 = steps.contentPromotionBannerSteps().createDefaultBanner(adGroupInfo).getBannerId();
        ModelChanges<ContentPromotionBanner> change1 =
                getContentPromotionModelChanges(contentPromotion.getBannerId())
                        .process("change1", ContentPromotionBanner.TITLE);
        ModelChanges<ContentPromotionBanner> change2 = getContentPromotionModelChanges(contentPromotionId2)
                .process("", ContentPromotionBanner.TITLE);

        List<ModelChanges<BannerWithSystemFields>> changes = asList(castUp(change1), castUp(change2));

        MassResult<Long> massResult = createPartialUpdateOperation(changes).prepareAndApply();
        assertThat(massResult, isSuccessful(true, false));
        assertThat(massResult.getResult().get(1).getValidationResult(),
                hasDefectDefinitionWith(validationError(path(field("title")), stringShouldNotBeBlank())));
    }

    //save
    @Test
    public void prepareAndApply_PartialYes_ValidBanner_BannerInDb() {
        String newTitle = "change1";
        ModelChanges<ContentPromotionBanner> change1 =
                getContentPromotionModelChanges(contentPromotion.getBannerId())
                        .process(newTitle, ContentPromotionBanner.TITLE);

        List<ModelChanges<BannerWithSystemFields>> changes = singletonList(castUp(change1));

        MassResult<Long> massResult = createPartialUpdateOperation(changes).prepareAndApply();
        assumeThat(massResult, isSuccessful(true));

        List<ContentPromotionBanner> actualBanners = repository
                .getStrictly(shard, singletonList(contentPromotion.getBannerId()), ContentPromotionBanner.class);
        assumeThat(actualBanners, hasSize(1));

        ContentPromotionBanner expectedBanner = contentPromotion.getBanner();
        expectedBanner
                .withTitle(newTitle)
                .withStatusModerate(BannerStatusModerate.READY)
                .withStatusPostModerate(BannerStatusPostModerate.NO)
                .withStatusBsSynced(StatusBsSynced.NO);
        DefaultCompareStrategy compareStrategy = allFieldsExcept(
                newPath(ContentPromotionBanner.LANGUAGE.name()),
                newPath(ContentPromotionBanner.LAST_CHANGE.name()));
        assertThat(actualBanners.get(0), beanDiffer(expectedBanner).useCompareStrategy(compareStrategy));
    }

    private BannersUpdateOperation<BannerWithSystemFields> createPartialUpdateOperation(
            List<ModelChanges<BannerWithSystemFields>> changes) {
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(clientId);

        return updateOperationFactory.createUpdateOperation(
                Applicability.PARTIAL,
                false,
                ModerationMode.DEFAULT,
                changes,
                shard,
                clientId,
                clientUid,
                clientEnabledFeatures,
                false);
    }

    private ModelChanges<BannerWithSystemFields> castUp(ModelChanges<ContentPromotionBanner> modelChanges) {
        return modelChanges.castModelUp(BannerWithSystemFields.class);
    }

    private ModelChanges<ContentPromotionBanner> getContentPromotionModelChanges(Long bannerId) {
        return new ModelChanges<>(bannerId, ContentPromotionBanner.class);
    }
}
