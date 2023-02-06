package ru.yandex.direct.core.entity.banner.type.bigkingimage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithBigKingImage;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.entity.creative.model.CreativeType.CPM_VIDEO_CREATIVE;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithBigKingImageAddTest extends BannerAdGroupInfoAddOperationTestBase {
    private CreativeInfo creativeInfo;
    private String imageHash1;
    private String imageHash2;
    private String imageHash3;
    private String imageHash250;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
        BannerImageFormat bif1 = TestBanners.defaultImageBannerImageFormat(null)
                .withWidth(516L)
                .withHeight(272L)
                .withFormatsJson("{\"orig\":{\"height\":\"516\",\"width\":\"272\"}}");
        BannerImageFormat bif2 = TestBanners.defaultImageBannerImageFormat(null)
                .withWidth(516L)
                .withHeight(272L)
                .withFormatsJson("{\"orig\":{\"height\":\"516\",\"width\":\"272\"}}");
        BannerImageFormat bif250 = TestBanners.defaultImageBannerImageFormat(null)
                .withWidth(300L)
                .withHeight(250L)
                .withFormatsJson("{\"orig\":{\"height\":\"250\",\"width\":\"300\"}}");

        imageHash1 = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo(), bif1).getImageHash();
        imageHash2 = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo(), bif2).getImageHash();
        imageHash250 = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo(), bif250).getImageHash();
        imageHash3 = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo()).getImageHash();
    }

    private CpmVideoAdGroup setupPriceVideoFrontpageAdGroup(Boolean allowImage) {
        //Создаёт креатив, кампанию, основную группу для прайсового видео
        var clientInfo = adGroupInfo.getClientInfo();
        creativeInfo = steps.creativeSteps().addCpmVideoFrontpageCreative(clientInfo);
        PricePackage pricePackage = defaultPricePackage()
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed().withViewTypes(List.of(ViewType.DESKTOP));
        pricePackage.getCampaignOptions().setAllowImage(allowImage);
        pricePackage.setAllowedCreativeTemplates(Map.of(CPM_VIDEO_CREATIVE, List.of(406L)));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);
        return adGroup;
    }

    @Test
    public void withBigKingImageAdImage() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash1);
        Long id = prepareAndApplyValid(banner);

        BannerWithBigKingImage actualBanner = getBanner(id);
        assertThat(actualBanner.getBigKingImageHash()).isEqualTo(imageHash1);
    }

    @Test
    public void withBigKingImageRegular() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash3);
        Long id = prepareAndApplyValid(banner);

        BannerWithBigKingImage actualBanner = getBanner(id);
        assertThat(actualBanner.getBigKingImageHash()).isEqualTo(imageHash3);
    }

    @Test
    public void withBigKingImageWithoutFeature() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, false);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash1);
        ValidationResult<?, Defect> validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field("bigKingImageHash")), isNull())));
    }

    @Test
    public void withoutBigKingImage() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(null);

        Long id = prepareAndApplyValid(banner);

        BannerWithBigKingImage actualBanner = getBanner(id);
        assertThat(actualBanner.getBigKingImageHash()).isNull();
    }

    @Test
    public void bigKingImageNotFound() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash("123");

        var validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithBigKingImage.BIG_KING_IMAGE_HASH.name())),
                imageNotFound())));
    }

    @Test
    public void oneBannerWithBigKingImageAndOneWithout() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);
        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getBigKingImageHash()).isEqualTo(imageHash1);
        assertThat(actualBanner2.getBigKingImageHash()).isNull();
    }

    @Test
    public void severalBannersWithBigKingImage() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);
        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withBigKingImageHash(imageHash2);

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getBigKingImageHash()).isEqualTo(imageHash1);
        assertThat(actualBanner2.getBigKingImageHash()).isEqualTo(imageHash2);
    }

    @Test
    public void videoFrontpagePricePackage() {
        // Фичи нет, баннер разрешен пакетом прайсового видео на главной
        var adGroup = setupPriceVideoFrontpageAdGroup(true);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, false);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withTitle("title")
                .withBody("body")
                .withLogoImageHash(steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash())
                .withBigKingImageHash(imageHash1);
        Long id = prepareAndApplyValid(banner);

        BannerWithBigKingImage actualBanner = getBanner(id);
        assertThat(actualBanner.getBigKingImageHash()).isEqualTo(imageHash1);
    }

    @Test
    public void videoFrontpagePricePackageNotAllowed() {
        // баннер запрещён пакетом, нельзя добавить
        var adGroup = setupPriceVideoFrontpageAdGroup(false);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withBigKingImageHash(imageHash1);
        var validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field("bigKingImageHash")), isNull())));
    }

    @Test
    public void videoFrontpagePricePackageMandatory() {
        // баннер разрешен пакетом, картинка обязательна
        var adGroup = setupPriceVideoFrontpageAdGroup(true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withBigKingImageHash(null);
        var validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field("bigKingImageHash")), notNull())));
    }

    @Test
    public void videoFrontpagePricePackageInvalidSize() {
        //баннер разрешен, пробуем добавить размер 300х250, будет ошибка валидации
        var adGroup = setupPriceVideoFrontpageAdGroup(true);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BIG_KING_IMAGE, true);

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withBigKingImageHash(imageHash250);
        var validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field("bigKingImageHash")), BannerDefects.imageSizeInvalid())));
    }
}
