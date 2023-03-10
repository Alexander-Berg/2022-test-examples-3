package ru.yandex.direct.api.v5.entity.ads.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.yandex.direct.api.v5.ads.AdsSelectionCriteria;
import com.yandex.direct.api.v5.ads.GetRequest;
import com.yandex.direct.api.v5.ads.TextAdFieldEnum;
import com.yandex.direct.api.v5.general.LimitOffset;
import one.util.streamex.LongStreamEx;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.common.validation.ApiDefectPresentation;
import ru.yandex.direct.api.v5.common.validation.DefaultApiPresentations;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.entity.ads.converter.ButtonExtensionConverter;
import ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter;
import ru.yandex.direct.api.v5.entity.ads.converter.LogoConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.util.PropertyFilter;
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
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.yandex.direct.api.v5.ads.AdsSelectionCriteria.PropInfo.AD_EXTENSION_IDS;
import static com.yandex.direct.api.v5.ads.AdsSelectionCriteria.PropInfo.AD_GROUP_IDS;
import static com.yandex.direct.api.v5.ads.AdsSelectionCriteria.PropInfo.AD_IMAGE_HASHES;
import static com.yandex.direct.api.v5.ads.AdsSelectionCriteria.PropInfo.CAMPAIGN_IDS;
import static com.yandex.direct.api.v5.ads.AdsSelectionCriteria.PropInfo.IDS;
import static com.yandex.direct.api.v5.ads.AdsSelectionCriteria.PropInfo.SITELINK_SET_IDS;
import static com.yandex.direct.api.v5.ads.AdsSelectionCriteria.PropInfo.V_CARD_IDS;
import static com.yandex.direct.api.v5.ads.GetRequest.PropInfo.SELECTION_CRITERIA;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MAX_OFFSET;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_IDS_COUNT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_LIMIT;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_MIN_OFFSET;
import static ru.yandex.direct.api.v5.common.constants.GetRequestCommonConstants.DEFAULT_RELATED_OBJECT_IDS_COUNT;
import static ru.yandex.direct.api.v5.entity.ads.Constants.MAX_ADGROUP_IDS_COUNT;
import static ru.yandex.direct.api.v5.entity.ads.Constants.MAX_CAMPAIGN_IDS_COUNT;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageLimitExceeded;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageNegativeOffset;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageNonPositiveLimit;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.incorrectPageOffsetExceeded;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.maxIdsInSelection;
import static ru.yandex.direct.api.v5.validation.ApiDefectDefinitions.selectionCriteriaParamMissed;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidUseOfField;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class GetAdsDelegateValidateRequestNegativeTest {

    private static final Long DEFAULT_AD_ID = 1L;

    private static GetAdsDelegate delegate;
    private static DefectPresentationService defectPresentationService =
            new DefectPresentationService(DefaultApiPresentations.HOLDER);

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Consumer<ValidationResult<GetRequest, DefectType>> assertion;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"SelectionCriteria ???? ??????????????",
                        new GetRequest(),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.getLocalPart())),
                                CommonDefects.notNull())
                },
                {"?????????????? ???????????? SelectionCriteria",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.getLocalPart())),
                                selectionCriteriaParamMissed(
                                        asList(IDS.schemaName.getLocalPart(), AD_GROUP_IDS.schemaName.getLocalPart(),
                                                CAMPAIGN_IDS.schemaName.getLocalPart())))
                },
                {"???????????? SelectionCriteria.Ids ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MAX_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                field(IDS.schemaName.getLocalPart())),
                                maxIdsInSelection())
                },
                {"???????????? SelectionCriteria.AdGroupIds ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withAdGroupIds(LongStreamEx.range(MAX_ADGROUP_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                        field(AD_GROUP_IDS.schemaName.getLocalPart())),
                                maxIdsInSelection())
                },
                {"???????????? SelectionCriteria.CampaignIds ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(MAX_CAMPAIGN_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                        field(CAMPAIGN_IDS.schemaName.getLocalPart())),
                                maxIdsInSelection())
                },
                {"???????????? SelectionCriteria.VCardIds ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withIds(DEFAULT_AD_ID)
                                .withVCardIds(
                                        LongStreamEx.range(DEFAULT_RELATED_OBJECT_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                field(V_CARD_IDS.schemaName.getLocalPart())),
                                maxIdsInSelection())
                },
                {"???????????? SelectionCriteria.SitelinkSetIds ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withIds(DEFAULT_AD_ID)
                                .withSitelinkSetIds(
                                        LongStreamEx.range(DEFAULT_RELATED_OBJECT_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                        field(SITELINK_SET_IDS.schemaName.getLocalPart())),
                                maxIdsInSelection())
                },
                {"???????????? SelectionCriteria.AdImageHashes ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withIds(DEFAULT_AD_ID)
                                .withAdImageHashes(Stream.generate(new Random()::nextInt)
                                        .limit(DEFAULT_RELATED_OBJECT_IDS_COUNT + 1)
                                        .map(i -> "adimage_hash" + i).collect(toList()))),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                        field(AD_IMAGE_HASHES.schemaName.getLocalPart())),
                                maxIdsInSelection())
                },
                {"???????????? SelectionCriteria.AdExtensionIds ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withIds(DEFAULT_AD_ID)
                                .withAdExtensionIds(
                                        LongStreamEx.range(DEFAULT_RELATED_OBJECT_IDS_COUNT + 1).boxed().toList())),
                        assertionForErrors(
                                path(field(SELECTION_CRITERIA.schemaName.getLocalPart()),
                                        field(AD_EXTENSION_IDS.schemaName.getLocalPart())),
                                maxIdsInSelection())
                },
                {"???????????????? SelectionCriteria.Limit ???????????? ???????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withLimit(DEFAULT_MIN_LIMIT - 1)),
                        assertionForErrors(path(field("Page"), field("Limit")), incorrectPageNonPositiveLimit())
                },
                {"???????????????? SelectionCriteria.Limit ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withLimit(DEFAULT_MAX_LIMIT + 1)),
                        assertionForErrors(path(field("Page"), field("Limit")),
                                incorrectPageLimitExceeded(DEFAULT_MAX_LIMIT))
                },
                {"???????????????? SelectionCriteria.Offset ???????????? ???????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withOffset(DEFAULT_MIN_OFFSET - 1)),
                        assertionForErrors(path(field("Page"), field("Offset")), incorrectPageNegativeOffset())
                },
                {"???????????????? SelectionCriteria.Offset ???????????? ?????????????????????? ??????????????????????",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria()
                                .withCampaignIds(LongStreamEx.range(DEFAULT_MIN_IDS_COUNT).boxed().toList())).withPage(
                                new LimitOffset().withOffset(DEFAULT_MAX_OFFSET + 1)),
                        assertionForErrors(path(field("Page"), field("Offset")),
                                incorrectPageOffsetExceeded(DEFAULT_MAX_OFFSET))
                },
                {"?? TextAdFieldNames ???????????? ?????????????????????????? ???????? LFHref",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria().withIds(singletonList(1L)))
                                .withTextAdFieldNames(singletonList(TextAdFieldEnum.LF_HREF)),
                        assertionForErrors(path(field("TextAdFieldNames"), index(0)), invalidUseOfField())
                },
                {"?? TextAdFieldNames ???????????? ?????????????????????????? ???????? LFButtonText",
                        new GetRequest().withSelectionCriteria(new AdsSelectionCriteria().withIds(singletonList(1L)))
                                .withTextAdFieldNames(singletonList(TextAdFieldEnum.LF_BUTTON_TEXT)),
                        assertionForErrors(path(field("TextAdFieldNames"), index(0)), invalidUseOfField())
                }
        });
    }

    @BeforeClass
    public static void setUp() {
        delegate = new GetAdsDelegate(mock(ApiAuthenticationSource.class),
                mock(ShardHelper.class),
                mock(BannerService.class), mock(BannerCommonRepository.class), mock(CampaignService.class),
                mock(CalloutRepository.class), mock(BannerCreativeRepository.class), mock(CreativeRepository.class),
                mock(ModerationReasonService.class), mock(MobileContentRepository.class),
                new GetResponseConverter(mock(PropertyFilter.class), mock(TranslationService.class),
                        mock(ButtonExtensionConverter.class), mock(LogoConverter.class)),
                mock(AdGroupService.class), mock(AdGroupRepository.class));
    }

    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForErrors(Path path,
                                                                                         Defect defect) {
        ApiDefectPresentation presentation = defectPresentationService.getPresentationFor(defect.defectId());

        DefectType expectedDefectType = presentation.toDefectType(defect.params());

        return (vresult) -> assertThat(vresult.flattenErrors(), contains(validationError(path, expectedDefectType)));
    }

    private static Consumer<ValidationResult<GetRequest, DefectType>> assertionForErrors(Path path,
                                                                                         DefectType expectedDefectType) {
        return (vresult) -> assertThat(vresult.flattenErrors(), contains(validationError(path, expectedDefectType)));
    }

    @Test
    public void test() {
        ValidationResult<GetRequest, DefectType> validationResult = delegate.validateRequest(request);
        assertion.accept(validationResult);
    }

}
