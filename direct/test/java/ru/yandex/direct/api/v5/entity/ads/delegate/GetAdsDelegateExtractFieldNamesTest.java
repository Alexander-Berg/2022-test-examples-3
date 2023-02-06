package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdFieldEnum;
import com.yandex.direct.api.v5.ads.ContentPromotionCollectionAdFieldEnum;
import com.yandex.direct.api.v5.ads.ContentPromotionEdaAdFieldEnum;
import com.yandex.direct.api.v5.ads.ContentPromotionServiceAdFieldEnum;
import com.yandex.direct.api.v5.ads.ContentPromotionVideoAdFieldEnum;
import com.yandex.direct.api.v5.ads.CpcVideoAdBuilderAdFieldEnum;
import com.yandex.direct.api.v5.ads.CpmBannerAdBuilderAdFieldEnum;
import com.yandex.direct.api.v5.ads.CpmVideoAdBuilderAdFieldEnum;
import com.yandex.direct.api.v5.ads.DynamicTextAdFieldEnum;
import com.yandex.direct.api.v5.ads.GetRequest;
import com.yandex.direct.api.v5.ads.MobileAppAdBuilderAdFieldEnum;
import com.yandex.direct.api.v5.ads.MobileAppAdFieldEnum;
import com.yandex.direct.api.v5.ads.MobileAppCpcVideoAdBuilderAdFieldEnum;
import com.yandex.direct.api.v5.ads.MobileAppImageAdFieldEnum;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdFieldEnum;
import com.yandex.direct.api.v5.ads.TextAdBuilderAdFieldEnum;
import com.yandex.direct.api.v5.ads.TextAdFieldEnum;
import com.yandex.direct.api.v5.ads.TextAdPriceExtensionFieldEnum;
import com.yandex.direct.api.v5.ads.TextImageAdFieldEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetAdsDelegateExtractFieldNamesTest {

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Set<AdAnyFieldEnum> expectedFields;

    private GetAdsDelegate delegate;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        List<AdFieldEnum> adFieldNames = asList(AdFieldEnum.values());
        List<AdFieldEnum> adFieldNamesWithDuplicates = new ArrayList<>(adFieldNames);
        adFieldNamesWithDuplicates.addAll(adFieldNames);
        Set<AdAnyFieldEnum> expectedAdFieldNames =
                Arrays.stream(AdFieldEnum.values()).map(AdAnyFieldEnum::fromAdFieldEnum).collect(toSet());

        List<TextAdFieldEnum> textAdFieldNames = asList(TextAdFieldEnum.values());
        List<TextAdFieldEnum> textAdFieldNamesWithDuplicates = new ArrayList<>(textAdFieldNames);
        textAdFieldNamesWithDuplicates.addAll(textAdFieldNames);
        Set<AdAnyFieldEnum> expectedTextAdFieldNames =
                Arrays.stream(TextAdFieldEnum.values()).map(AdAnyFieldEnum::fromTextAdFieldEnum).collect(toSet());

        List<TextAdPriceExtensionFieldEnum> textAdPriceExtensionFieldNames =
                asList(TextAdPriceExtensionFieldEnum.values());
        List<TextAdPriceExtensionFieldEnum> textAdFieldPriceExtensionNamesWithDuplicates =
                new ArrayList<>(textAdPriceExtensionFieldNames);
        textAdFieldPriceExtensionNamesWithDuplicates.addAll(textAdPriceExtensionFieldNames);
        Set<AdAnyFieldEnum> expectedTextAdPriceExtensionFieldNames =
                Arrays.stream(TextAdPriceExtensionFieldEnum.values())
                        .map(AdAnyFieldEnum::fromTextAdPriceExtensionFieldEnum).collect(toSet());

        List<MobileAppAdFieldEnum> mobileAppAdFieldNames = asList(MobileAppAdFieldEnum.values());
        List<MobileAppAdFieldEnum> mobileAppAdFieldNamesWithDuplicates = new ArrayList<>(mobileAppAdFieldNames);
        mobileAppAdFieldNamesWithDuplicates.addAll(mobileAppAdFieldNames);
        Set<AdAnyFieldEnum> expectedMobileAppAdFieldNames = Arrays.stream(MobileAppAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromMobileAppAdFieldEnum).collect(toSet());

        List<DynamicTextAdFieldEnum> dynamicTextAdFieldNames = asList(DynamicTextAdFieldEnum.values());
        List<DynamicTextAdFieldEnum> dynamicTextAdFieldNamesWithDuplicates = new ArrayList<>(dynamicTextAdFieldNames);
        dynamicTextAdFieldNamesWithDuplicates.addAll(dynamicTextAdFieldNames);
        Set<AdAnyFieldEnum> expectedDynamicTextAdFieldNames = Arrays.stream(DynamicTextAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromDynamicTextAdFieldEnum).collect(toSet());

        List<TextImageAdFieldEnum> textImageAdFieldNames = asList(TextImageAdFieldEnum.values());
        List<TextImageAdFieldEnum> textImageAdFieldNamesWithDuplicates = new ArrayList<>(textImageAdFieldNames);
        textImageAdFieldNamesWithDuplicates.addAll(textImageAdFieldNames);
        Set<AdAnyFieldEnum> expectedTextImageAdFieldNames = Arrays.stream(TextImageAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromTextImageAdFieldEnum).collect(toSet());

        List<MobileAppImageAdFieldEnum> mobileAppImageAdFieldNames = asList(MobileAppImageAdFieldEnum.values());
        List<MobileAppImageAdFieldEnum> mobileAppImageAdFieldNamesWithDuplicates =
                new ArrayList<>(mobileAppImageAdFieldNames);
        mobileAppImageAdFieldNamesWithDuplicates.addAll(mobileAppImageAdFieldNames);
        Set<AdAnyFieldEnum> expectedMobileAppImageAdFieldNames = Arrays.stream(MobileAppImageAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromMobileAppImageAdFieldEnum).collect(toSet());

        List<MobileAppCpcVideoAdBuilderAdFieldEnum> mobileAppCpcVideoAdBuilderAdFieldNames =
                asList(MobileAppCpcVideoAdBuilderAdFieldEnum.values());
        List<MobileAppCpcVideoAdBuilderAdFieldEnum> mobileAppCpcVideoAdBuilderAdFieldNamesWithDuplicates =
                new ArrayList<>(mobileAppCpcVideoAdBuilderAdFieldNames);
        mobileAppCpcVideoAdBuilderAdFieldNamesWithDuplicates.addAll(mobileAppCpcVideoAdBuilderAdFieldNames);
        Set<AdAnyFieldEnum> expectedMobileAppCpcVideoAdBuilderAdFieldNames =
                Arrays.stream(MobileAppCpcVideoAdBuilderAdFieldEnum.values())
                        .map(AdAnyFieldEnum::fromMobileAppCpcVideoAdBuilderAdFieldEnum).collect(toSet());

        List<TextAdBuilderAdFieldEnum> textAdBuilderAdFieldNames = asList(TextAdBuilderAdFieldEnum.values());
        List<TextAdBuilderAdFieldEnum> textAdBuilderAdFieldNamesWithDuplicates =
                new ArrayList<>(textAdBuilderAdFieldNames);
        textAdBuilderAdFieldNamesWithDuplicates.addAll(textAdBuilderAdFieldNames);
        Set<AdAnyFieldEnum> expectedTextAdBuilderAdFieldNames = Arrays.stream(TextAdBuilderAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromTextAdBuilderAdFieldEnum).collect(toSet());

        List<MobileAppAdBuilderAdFieldEnum> mobileAppAdBuilderAdFieldNames =
                asList(MobileAppAdBuilderAdFieldEnum.values());
        List<MobileAppAdBuilderAdFieldEnum> mobileAppAdBuilderAdFieldNamesWithDuplicates =
                new ArrayList<>(mobileAppAdBuilderAdFieldNames);
        mobileAppAdBuilderAdFieldNamesWithDuplicates.addAll(mobileAppAdBuilderAdFieldNames);
        Set<AdAnyFieldEnum> expectedMobileAppAdBuilderAdFieldNames =
                Arrays.stream(MobileAppAdBuilderAdFieldEnum.values())
                        .map(AdAnyFieldEnum::fromMobileAppAdBuilderAdFieldEnum).collect(toSet());

        List<CpmBannerAdBuilderAdFieldEnum> cpmBannerAdBuilderAdFieldNames =
                asList(CpmBannerAdBuilderAdFieldEnum.values());
        List<CpmBannerAdBuilderAdFieldEnum> cpmBannerAdBuilderAdFieldNamesWithDuplicates =
                new ArrayList<>(cpmBannerAdBuilderAdFieldNames);
        cpmBannerAdBuilderAdFieldNamesWithDuplicates.addAll(cpmBannerAdBuilderAdFieldNames);
        Set<AdAnyFieldEnum> expectedCpmBannerAdBuilderAdFieldNames =
                Arrays.stream(CpmBannerAdBuilderAdFieldEnum.values())
                        .map(AdAnyFieldEnum::fromCpmBannerAdBuilderAdFieldEnum).collect(toSet());

        List<CpcVideoAdBuilderAdFieldEnum> cpcVideoAdBuilderAdFieldNames =
                asList(CpcVideoAdBuilderAdFieldEnum.values());
        List<CpcVideoAdBuilderAdFieldEnum> cpcVideoAdBuilderAdFieldNamesWithDuplicates =
                new ArrayList<>(cpcVideoAdBuilderAdFieldNames);
        cpcVideoAdBuilderAdFieldNamesWithDuplicates.addAll(cpcVideoAdBuilderAdFieldNames);
        Set<AdAnyFieldEnum> expectedCpcVideoAdBuilderAdFieldNames = Arrays.stream(CpcVideoAdBuilderAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromCpcVideoAdBuilderAdFieldEnum).collect(toSet());

        Set<AdAnyFieldEnum> expectedAllFieldNames = Arrays.stream(AdAnyFieldEnum.values()).collect(toSet());

        List<CpmVideoAdBuilderAdFieldEnum> cpmVideoAdBuilderAdFieldNames =
                asList(CpmVideoAdBuilderAdFieldEnum.values());
        List<CpmVideoAdBuilderAdFieldEnum> cpmVideoAdBuilderAdFieldNamesWithDuplicates =
                new ArrayList<>(cpmVideoAdBuilderAdFieldNames);
        cpmVideoAdBuilderAdFieldNamesWithDuplicates.addAll(cpmVideoAdBuilderAdFieldNames);
        Set<AdAnyFieldEnum> expectedCpmVideoAdBuilderAdFieldNames = Arrays.stream(CpmVideoAdBuilderAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromCpmVideoAdBuilderAdFieldEnum).collect(toSet());

        List<SmartAdBuilderAdFieldEnum> smartAdBuilderAdFieldNames =
                asList(SmartAdBuilderAdFieldEnum.values());
        List<SmartAdBuilderAdFieldEnum> smartAdBuilderAdFieldNamesWithDuplicates =
                new ArrayList<>(smartAdBuilderAdFieldNames);
        smartAdBuilderAdFieldNamesWithDuplicates.addAll(smartAdBuilderAdFieldNames);
        Set<AdAnyFieldEnum> expectedSmartAdBuilderAdFieldNames = Arrays.stream(SmartAdBuilderAdFieldEnum.values())
                .map(AdAnyFieldEnum::fromSmartAdBuilderAdFieldEnum).collect(toSet());
        List<ContentPromotionVideoAdFieldEnum> contentPromotionVideoAdFieldNames =
                asList(ContentPromotionVideoAdFieldEnum.values());
        List<ContentPromotionVideoAdFieldEnum> contentPromotionVideoAdFieldNamesWithDuplicates =
                new ArrayList<>(contentPromotionVideoAdFieldNames);
        contentPromotionVideoAdFieldNamesWithDuplicates.addAll(contentPromotionVideoAdFieldNames);
        Set<AdAnyFieldEnum> expectedContentPromotionVideoAdFieldNames =
                Arrays.stream(ContentPromotionVideoAdFieldEnum.values())
                        .map(AdAnyFieldEnum::fromContentPromotionVideoAdFieldEnum)
                        .collect(toSet());
        List<ContentPromotionCollectionAdFieldEnum> contentPromotionCollectionAdFieldNames =
                asList(ContentPromotionCollectionAdFieldEnum.values());
        List<ContentPromotionCollectionAdFieldEnum> contentPromotionCollectionAdFieldNamesWithDuplicates =
                new ArrayList<>(contentPromotionCollectionAdFieldNames);
        contentPromotionCollectionAdFieldNamesWithDuplicates.addAll(contentPromotionCollectionAdFieldNames);
        Set<AdAnyFieldEnum> expectedContentPromotionCollectionAdFieldNames =
                Arrays.stream(ContentPromotionCollectionAdFieldEnum.values())
                        .map(AdAnyFieldEnum::fromContentPromotionCollectionAdFieldEnum)
                        .collect(toSet());
        List<ContentPromotionServiceAdFieldEnum> contentPromotionServiceAdFieldNames =
                asList(ContentPromotionServiceAdFieldEnum.values());
        List<ContentPromotionServiceAdFieldEnum> contentPromotionServiceAdFieldNamesWithDuplicates =
                new ArrayList<>(contentPromotionServiceAdFieldNames);
        contentPromotionServiceAdFieldNamesWithDuplicates.addAll(contentPromotionServiceAdFieldNames);
        Set<AdAnyFieldEnum> expectedContentPromotionServiceAdFieldNames =
                Arrays.stream(ContentPromotionServiceAdFieldEnum.values())
                        .map(AdAnyFieldEnum::fromContentPromotionServiceAdFieldEnum)
                        .collect(toSet());

        List<ContentPromotionEdaAdFieldEnum> contentPromotionEdaAdFieldNames =
                asList(ContentPromotionEdaAdFieldEnum.values());
        List<ContentPromotionEdaAdFieldEnum> contentPromotionEdaAdFieldNamesWithDuplicates =
                new ArrayList<>(contentPromotionEdaAdFieldNames);
        contentPromotionEdaAdFieldNamesWithDuplicates.addAll(contentPromotionEdaAdFieldNames);
        Set<AdAnyFieldEnum> expectedContentPromotionEdaAdFieldNames =
                Arrays.stream(ContentPromotionEdaAdFieldEnum.values())
                        .map(AdAnyFieldEnum::fromContentPromotionEdaAdFieldEnum)
                        .collect(toSet());


        return new Object[][]{
                {"with field names", new GetRequest().withFieldNames(adFieldNames), expectedAdFieldNames},
                {"with field names with duplicates", new GetRequest().withFieldNames(adFieldNamesWithDuplicates),
                        expectedAdFieldNames},
                {"with text ad field names", new GetRequest().withTextAdFieldNames(textAdFieldNames),
                        expectedTextAdFieldNames},
                {"with text ad field names with duplicates",
                        new GetRequest().withTextAdFieldNames(textAdFieldNamesWithDuplicates),
                        expectedTextAdFieldNames},
                {"with text ad price extension field names",
                        new GetRequest().withTextAdPriceExtensionFieldNames(textAdPriceExtensionFieldNames),
                        expectedTextAdPriceExtensionFieldNames},
                {"with text ad price extension field names with duplicates",
                        new GetRequest()
                                .withTextAdPriceExtensionFieldNames(textAdFieldPriceExtensionNamesWithDuplicates),
                        expectedTextAdPriceExtensionFieldNames},
                {"with mobile app ad field names", new GetRequest().withMobileAppAdFieldNames(mobileAppAdFieldNames),
                        expectedMobileAppAdFieldNames},
                {"with mobile app ad field names with duplicates",
                        new GetRequest().withMobileAppAdFieldNames(mobileAppAdFieldNamesWithDuplicates),
                        expectedMobileAppAdFieldNames},
                {"with dynamic text ad field names",
                        new GetRequest().withDynamicTextAdFieldNames(dynamicTextAdFieldNames),
                        expectedDynamicTextAdFieldNames},
                {"with dynamic text ad field names with duplicates",
                        new GetRequest().withDynamicTextAdFieldNames(dynamicTextAdFieldNamesWithDuplicates),
                        expectedDynamicTextAdFieldNames},
                {"with text image ad field names",
                        new GetRequest().withTextImageAdFieldNames(textImageAdFieldNames),
                        expectedTextImageAdFieldNames},
                {"with text image ad field names with duplicates",
                        new GetRequest().withTextImageAdFieldNames(textImageAdFieldNamesWithDuplicates),
                        expectedTextImageAdFieldNames},
                {"with mobile app image ad field names",
                        new GetRequest().withMobileAppImageAdFieldNames(mobileAppImageAdFieldNames),
                        expectedMobileAppImageAdFieldNames},
                {"with mobile app image ad field names with duplicates",
                        new GetRequest().withMobileAppImageAdFieldNames(mobileAppImageAdFieldNamesWithDuplicates),
                        expectedMobileAppImageAdFieldNames},
                {"with mobile app cpc video field names",
                        new GetRequest().withMobileAppCpcVideoAdBuilderAdFieldNames(
                                mobileAppCpcVideoAdBuilderAdFieldNames),
                        expectedMobileAppCpcVideoAdBuilderAdFieldNames},
                {"with mobile app cpc video field names with duplicates",
                        new GetRequest().withMobileAppCpcVideoAdBuilderAdFieldNames(
                                mobileAppCpcVideoAdBuilderAdFieldNamesWithDuplicates),
                        expectedMobileAppCpcVideoAdBuilderAdFieldNames},
                {"with text adbuilder ad field names",
                        new GetRequest().withTextAdBuilderAdFieldNames(textAdBuilderAdFieldNames),
                        expectedTextAdBuilderAdFieldNames},
                {"with text adbuilder ad field names with duplicates",
                        new GetRequest().withTextAdBuilderAdFieldNames(textAdBuilderAdFieldNamesWithDuplicates),
                        expectedTextAdBuilderAdFieldNames},
                {"with mobile app adbuilder ad field names",
                        new GetRequest().withMobileAppAdBuilderAdFieldNames(mobileAppAdBuilderAdFieldNames),
                        expectedMobileAppAdBuilderAdFieldNames},
                {"with mobile app adbuilder ad field names with duplicates",
                        new GetRequest().withMobileAppAdBuilderAdFieldNames(
                                mobileAppAdBuilderAdFieldNamesWithDuplicates),
                        expectedMobileAppAdBuilderAdFieldNames},
                {"with cpm banner ad field names",
                        new GetRequest().withCpmBannerAdBuilderAdFieldNames(cpmBannerAdBuilderAdFieldNames),
                        expectedCpmBannerAdBuilderAdFieldNames},
                {"with cpm banner ad field names with duplicates",
                        new GetRequest().withCpmBannerAdBuilderAdFieldNames(
                                cpmBannerAdBuilderAdFieldNamesWithDuplicates),
                        expectedCpmBannerAdBuilderAdFieldNames},
                {"with cpc video adbuilder ad field names",
                        new GetRequest().withCpcVideoAdBuilderAdFieldNames(cpcVideoAdBuilderAdFieldNames),
                        expectedCpcVideoAdBuilderAdFieldNames},
                {"with cpc video adbuilder ad field names with duplicates",
                        new GetRequest().withCpcVideoAdBuilderAdFieldNames(cpcVideoAdBuilderAdFieldNamesWithDuplicates),
                        expectedCpcVideoAdBuilderAdFieldNames},
                {"with cpm video adbuilder ad field names",
                        new GetRequest().withCpmVideoAdBuilderAdFieldNames(cpmVideoAdBuilderAdFieldNames),
                        expectedCpmVideoAdBuilderAdFieldNames},
                {"with cpm video adbuilder ad field names with duplicates",
                        new GetRequest().withCpmVideoAdBuilderAdFieldNames(cpmVideoAdBuilderAdFieldNamesWithDuplicates),
                        expectedCpmVideoAdBuilderAdFieldNames},
                {"with smart ad field names",
                        new GetRequest().withSmartAdBuilderAdFieldNames(smartAdBuilderAdFieldNames),
                        expectedSmartAdBuilderAdFieldNames},
                {"with smart ad field names with duplicates",
                        new GetRequest().withSmartAdBuilderAdFieldNames(smartAdBuilderAdFieldNamesWithDuplicates),
                        expectedSmartAdBuilderAdFieldNames},
                {"with content promotion video ad field names",
                        new GetRequest().withContentPromotionVideoAdFieldNames(contentPromotionVideoAdFieldNames),
                        expectedContentPromotionVideoAdFieldNames},
                {"with content promotion collection ad field names", new GetRequest()
                        .withContentPromotionCollectionAdFieldNames(contentPromotionCollectionAdFieldNames),
                        expectedContentPromotionCollectionAdFieldNames},
                {"with content promotion service ad field names",
                        new GetRequest().withContentPromotionServiceAdFieldNames(contentPromotionServiceAdFieldNames),
                        expectedContentPromotionServiceAdFieldNames},
                {"with content promotion eda ad field names",
                        new GetRequest().withContentPromotionEdaAdFieldNames(contentPromotionEdaAdFieldNames),
                        expectedContentPromotionEdaAdFieldNames},
                {"with all field names",
                        new GetRequest().withFieldNames(adFieldNames)
                                .withTextAdFieldNames(textAdFieldNames)
                                .withTextAdPriceExtensionFieldNames(textAdPriceExtensionFieldNames)
                                .withMobileAppAdFieldNames(mobileAppAdFieldNames)
                                .withDynamicTextAdFieldNames(dynamicTextAdFieldNames)
                                .withTextImageAdFieldNames(textImageAdFieldNames)
                                .withMobileAppImageAdFieldNames(mobileAppImageAdFieldNames)
                                .withMobileAppCpcVideoAdBuilderAdFieldNames(mobileAppCpcVideoAdBuilderAdFieldNames)
                                .withTextAdBuilderAdFieldNames(textAdBuilderAdFieldNames)
                                .withMobileAppAdBuilderAdFieldNames(mobileAppAdBuilderAdFieldNames)
                                .withCpmBannerAdBuilderAdFieldNames(cpmBannerAdBuilderAdFieldNames)
                                .withCpcVideoAdBuilderAdFieldNames(cpcVideoAdBuilderAdFieldNames)
                                .withCpmVideoAdBuilderAdFieldNames(cpmVideoAdBuilderAdFieldNames)
                                .withSmartAdBuilderAdFieldNames(smartAdBuilderAdFieldNames)
                                .withContentPromotionVideoAdFieldNames(contentPromotionVideoAdFieldNames)
                                .withContentPromotionCollectionAdFieldNames(contentPromotionCollectionAdFieldNames)
                                .withContentPromotionServiceAdFieldNames(contentPromotionServiceAdFieldNames)
                                .withContentPromotionEdaAdFieldNames(contentPromotionEdaAdFieldNames),
                        expectedAllFieldNames},
                {"with all field names with duplicates",
                        new GetRequest().withFieldNames(adFieldNamesWithDuplicates)
                                .withTextAdFieldNames(textAdFieldNamesWithDuplicates)
                                .withTextAdPriceExtensionFieldNames(textAdFieldPriceExtensionNamesWithDuplicates)
                                .withMobileAppAdFieldNames(mobileAppAdFieldNamesWithDuplicates)
                                .withDynamicTextAdFieldNames(dynamicTextAdFieldNamesWithDuplicates)
                                .withTextImageAdFieldNames(textImageAdFieldNamesWithDuplicates)
                                .withMobileAppImageAdFieldNames(mobileAppImageAdFieldNamesWithDuplicates)
                                .withMobileAppCpcVideoAdBuilderAdFieldNames(
                                        mobileAppCpcVideoAdBuilderAdFieldNamesWithDuplicates)
                                .withTextAdBuilderAdFieldNames(textAdBuilderAdFieldNamesWithDuplicates)
                                .withMobileAppAdBuilderAdFieldNames(mobileAppAdBuilderAdFieldNamesWithDuplicates)
                                .withCpmBannerAdBuilderAdFieldNames(cpmBannerAdBuilderAdFieldNamesWithDuplicates)
                                .withCpcVideoAdBuilderAdFieldNames(cpcVideoAdBuilderAdFieldNamesWithDuplicates)
                                .withCpmVideoAdBuilderAdFieldNames(cpmVideoAdBuilderAdFieldNamesWithDuplicates)
                                .withSmartAdBuilderAdFieldNames(smartAdBuilderAdFieldNamesWithDuplicates)
                                .withCpmVideoAdBuilderAdFieldNames(cpmVideoAdBuilderAdFieldNamesWithDuplicates)
                                .withContentPromotionVideoAdFieldNames(contentPromotionVideoAdFieldNamesWithDuplicates)
                                .withContentPromotionCollectionAdFieldNames(
                                        contentPromotionCollectionAdFieldNamesWithDuplicates)
                                .withContentPromotionServiceAdFieldNames(
                                        contentPromotionServiceAdFieldNamesWithDuplicates)
                                .withContentPromotionEdaAdFieldNames(contentPromotionEdaAdFieldNamesWithDuplicates),
                        expectedAllFieldNames},
        };
    }

    @Before
    public void prepare() {
        delegate = new GetAdsDelegate(mock(ApiAuthenticationSource.class),
                mock(ShardHelper.class),
                mock(BannerService.class), mock(BannerCommonRepository.class), mock(CampaignService.class),
                mock(CalloutRepository.class), mock(BannerCreativeRepository.class), mock(CreativeRepository.class),
                mock(ModerationReasonService.class), mock(MobileContentRepository.class),
                mock(GetResponseConverter.class), mock(AdGroupService.class), mock(AdGroupRepository.class));
    }

    @Test
    public void test() {
        assertThat(delegate.extractFieldNames(request))
                .containsExactlyInAnyOrderElementsOf(expectedFields);
    }
}
