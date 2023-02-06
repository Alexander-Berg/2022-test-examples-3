package ru.yandex.direct.core.entity.banner.type.bigkingimage;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithBigKingImage;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperation;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithBigKingImageUpdateTest extends BannerClientInfoUpdateOperationTestBase {
    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    private CreativeInfo creativeInfo;
    private AdGroupInfo adGroupInfo;
    private String imageHash1;
    private String imageHash2;
    private String imageHash3;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(clientInfo,
                steps.creativeSteps().getNextCreativeId());
        imageHash1 = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo()).getImageHash();
        imageHash2 = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo()).getImageHash();
        imageHash3 = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo()).getImageHash();
    }

    @Test
    public void updateImageNotFound() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        BannersAddOperation operation = addOperationFactory.createAddOperation(Applicability.FULL,
                false, List.of(banner),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), false, false, false, false,
                featureService.getEnabledForClientId(adGroupInfo.getClientId()));

        Long bannerId = operation.prepareAndApply().get(0).getResult();

        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.BIG_KING_IMAGE_HASH, "123");

        var validationResult = prepareAndApplyInvalid(modelChanges);

        Assert.assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithBigKingImage.BIG_KING_IMAGE_HASH.name())),
                imageNotFound())));
    }

    @Test
    public void multipleUpdate() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner bannerWithoutBigKingImage = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        CpmBanner bannerWithBigKingImage = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash1);

        BannersAddOperation operation1 = addOperationFactory.createAddOperation(Applicability.FULL,
                false, List.of(bannerWithoutBigKingImage),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), false, false, false, false,
                featureService.getEnabledForClientId(adGroupInfo.getClientId()));
        BannersAddOperation operation2 = addOperationFactory.createAddOperation(Applicability.FULL,
                false, List.of(bannerWithBigKingImage),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), false, false, false, false,
                featureService.getEnabledForClientId(adGroupInfo.getClientId()));

        Long bannerId1 = operation1.prepareAndApply().get(0).getResult();
        Long bannerId2 = operation2.prepareAndApply().get(0).getResult();

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(bannerId1, CpmBanner.class,
                CpmBanner.BIG_KING_IMAGE_HASH, imageHash2);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(bannerId2, CpmBanner.class,
                CpmBanner.BIG_KING_IMAGE_HASH, imageHash3);

        prepareAndApplyValid(asList(modelChanges1, modelChanges2));

        CpmBanner actualBanner1 = getBanner(bannerId1);
        CpmBanner actualBanner2 = getBanner(bannerId2);

        assertThat(actualBanner1.getBigKingImageHash()).isEqualTo(imageHash2);
        assertThat(actualBanner2.getBigKingImageHash()).isEqualTo(imageHash3);
    }

    @Test
    public void updateNullToNull() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        BannersAddOperation operation = addOperationFactory.createAddOperation(Applicability.FULL,
                false, List.of(banner),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), false, false, false, false,
                featureService.getEnabledForClientId(adGroupInfo.getClientId()));

        Long bannerId = operation.prepareAndApply().get(0).getResult();

        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.BIG_KING_IMAGE_HASH, null);

        prepareAndApplyValid(List.of(modelChanges));

        CpmBanner actualBanner = getBanner(bannerId);

        assertThat(actualBanner.getBigKingImageHash()).isEqualTo(null);
    }

    @Test
    public void updateNotNullToNull() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash1);

        BannersAddOperation operation = addOperationFactory.createAddOperation(Applicability.FULL,
                false, List.of(banner),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), false, false, false, false,
                featureService.getEnabledForClientId(adGroupInfo.getClientId()));

        Long bannerId = operation.prepareAndApply().get(0).getResult();

        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.BIG_KING_IMAGE_HASH, null);

        prepareAndApplyValid(List.of(modelChanges));

        CpmBanner actualBanner = getBanner(bannerId);

        assertThat(actualBanner.getBigKingImageHash()).isEqualTo(null);
    }

    @Test
    public void multipleUpdateWithoutFeature() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner bannerWithBigKingImage = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash1);

        BannersAddOperation operation = addOperationFactory.createAddOperation(Applicability.FULL,
                false, List.of(bannerWithBigKingImage),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), false, false, false, false,
                featureService.getEnabledForClientId(adGroupInfo.getClientId()));

        Long bannerId = operation.prepareAndApply().get(0).getResult();

        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, false);

        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.BIG_KING_IMAGE_HASH, imageHash2);

        ValidationResult<?, Defect> validationResult = prepareAndApplyInvalid(modelChanges);

        Assert.assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field("bigKingImageHash")), isNull())));
    }


}

