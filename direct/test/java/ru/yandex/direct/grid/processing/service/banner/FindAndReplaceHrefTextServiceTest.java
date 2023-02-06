package ru.yandex.direct.grid.processing.service.banner;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkRepository;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefTargetType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefText;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceHrefTextHrefPart;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceHrefTextInstruction;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;
import static ru.yandex.direct.grid.processing.service.banner.converter.FindAndReplaceBannerHrefConverter.getEmptyPayload;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.notContainNulls;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FindAndReplaceHrefTextServiceTest {
    private static final String REPLACE_HREF_TEXT_ARG_NAME = "input";
    private static final Set<GdFindAndReplaceAdsHrefTargetType> ALL_TARGETS =
            ImmutableSet.of(GdFindAndReplaceAdsHrefTargetType.AD_HREF, GdFindAndReplaceAdsHrefTargetType.SITELINK_HREF);

    @Autowired
    private Steps steps;
    @Autowired
    private FindAndReplaceHrefTextService serviceUnderTest;
    @Autowired
    private BannerTypedRepository bannerRepository;
    @Autowired
    private SitelinkSetRepository sitelinkSetRepository;
    @Autowired
    private SitelinkRepository sitelinkRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ClientInfo clientInfo;
    private long operatorUid;
    private ClientId clientId;

    private GdFindAndReplaceAdsHrefText validInput;

    private TextBannerInfo bannerForUpdate;
    private SitelinkSetInfo sitelinkSetForUpdate;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        GdFindAndReplaceHrefTextInstruction validInstruction = new GdFindAndReplaceHrefTextInstruction()
                .withHrefParts(singletonSet(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH))
                .withSearch("abc")
                .withReplace("dce");

        sitelinkSetForUpdate = steps.sitelinkSetSteps().createSitelinkSet(new SitelinkSet()
                .withSitelinks(asList(defaultSitelink().withHref("http://yandex.ru"),
                        defaultSitelink2().withHref("http://abc.com/another_path?q1=p1#fr"))), clientInfo);
        bannerForUpdate = steps.bannerSteps().createBanner(activeTextBanner()
                        .withHref("http://abc.com/path/1?q1=p1#fr")
                        .withSitelinksSetId(sitelinkSetForUpdate.getSitelinkSetId()),
                clientInfo);

        validInput = new GdFindAndReplaceAdsHrefText()
                .withAdIds(singletonList(bannerForUpdate.getBannerId()))
                .withReplaceInstruction(validInstruction)
                .withTargetTypes(singletonSet(GdFindAndReplaceAdsHrefTargetType.AD_HREF))
                .withAdIdsHrefExceptions(emptySet())
                .withSitelinkIdsHrefExceptions(emptyMap());
    }

    @Test
    public void previewForBannerAndSitelinksReplaceByPattern() {
        validInput.withTargetTypes(ALL_TARGETS);
        GdCachedResult preview =
                serviceUnderTest.preview(validInput, operatorUid, clientId,
                        REPLACE_HREF_TEXT_ARG_NAME);

        Sitelink sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        Sitelink sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);
        GdCachedResult expectedPreview = new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                .withTotalCount(1)
                .withRowset(singletonList(new GdFindAndReplaceAdsHrefPayloadItem()
                        .withAdId(validInput.getAdIds().get(0))
                        .withOldHref("http://abc.com/path/1?q1=p1#fr")
                        .withNewHref("http://dce.com/path/1?q1=p1#fr")
                        .withSitelinks(asList(
                                new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                        .withSitelinkId(sitelink1.getId())
                                        .withTitle(sitelink1.getTitle())
                                        .withOldHref("http://yandex.ru")
                                        .withNewHref(null),
                                new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                        .withSitelinkId(sitelink2.getId())
                                        .withTitle(sitelink2.getTitle())
                                        .withOldHref("http://abc.com/another_path?q1=p1#fr")
                                        .withNewHref("http://dce.com/another_path?q1=p1#fr")
                        ))));

        assertThat(preview, beanDiffer(expectedPreview).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void previewForBannerAndSitelinksReplaceAll() {
        validInput.withTargetTypes(ALL_TARGETS)
                .withReplaceInstruction(validInput.getReplaceInstruction()
                        .withSearch("*")
                        .withReplace("http://ya.ru"));
        GdCachedResult preview =
                serviceUnderTest.preview(validInput, operatorUid, clientId,
                        REPLACE_HREF_TEXT_ARG_NAME);

        Sitelink sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        Sitelink sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);
        GdCachedResult expectedPreview = new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                .withTotalCount(1)
                .withRowset(singletonList(new GdFindAndReplaceAdsHrefPayloadItem()
                        .withAdId(validInput.getAdIds().get(0))
                        .withOldHref("http://abc.com/path/1?q1=p1#fr")
                        .withNewHref("http://ya.ru?q1=p1#fr")
                        .withSitelinks(asList(
                                new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                        .withSitelinkId(sitelink1.getId())
                                        .withTitle(sitelink1.getTitle())
                                        .withOldHref("http://yandex.ru")
                                        .withNewHref("http://ya.ru"),
                                new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                        .withSitelinkId(sitelink2.getId())
                                        .withTitle(sitelink2.getTitle())
                                        .withOldHref("http://abc.com/another_path?q1=p1#fr")
                                        .withNewHref("http://ya.ru?q1=p1#fr")
                        ))));

        assertThat(preview, beanDiffer(expectedPreview).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void previewForOnlySecondSitelinkChanged_CorrectRowset() {
        validInput.withTargetTypes(singletonSet(GdFindAndReplaceAdsHrefTargetType.SITELINK_HREF));
        Sitelink sitelink1 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(0);
        Sitelink sitelink2 = sitelinkSetForUpdate.getSitelinkSet().getSitelinks().get(1);
        validInput.withSitelinkIdsHrefExceptions(singletonMap(bannerForUpdate.getBannerId(), singletonSet(
                sitelink1.getId())));

        GdCachedResult preview =
                serviceUnderTest.preview(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);

        GdCachedResult expectedPreview = new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                .withTotalCount(1)
                .withRowset(singletonList(new GdFindAndReplaceAdsHrefPayloadItem()
                        .withSitelinks(asList(
                                new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                        .withSitelinkId(sitelink1.getId())
                                        .withOldHref(sitelink1.getHref())
                                        .withNewHref(null),
                                new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                        .withSitelinkId(sitelink2.getId())
                                        .withOldHref("http://abc.com/another_path?q1=p1#fr")
                                        .withNewHref("http://dce.com/another_path?q1=p1#fr")))));

        DefaultCompareStrategy compareStrategy = onlyFields(
                newPath(GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink.SITELINK_ID.name()),
                newPath(GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink.NEW_HREF.name()),
                newPath(GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink.OLD_HREF.name()));
        assertThat(preview, beanDiffer(expectedPreview).useCompareStrategy(compareStrategy));
    }

    // total replace
    @Test
    public void successfullyReplaceAllInBanner_WhenSearchIsNull() {
        String newUrl = "http://ya.ru";

        validInput
                .withReplaceInstruction(new GdFindAndReplaceHrefTextInstruction()
                        .withHrefParts(Set.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                                GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT))
                        .withSearch(null)
                        .withReplace(newUrl));

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        checkResultOfSuccessfulUpdate(result);

        TextBanner banner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        assertThat("href обновлен некорректно", banner.getHref(), is(newUrl));
    }

    @Test
    public void successfullyReplaceAllInBanner_WhenSearchIsAllRegexp() {
        String newUrl = "http://ya.ru";

        validInput
                .withReplaceInstruction(new GdFindAndReplaceHrefTextInstruction()
                        .withHrefParts(Set.of(GdFindAndReplaceHrefTextHrefPart.PROTOCOL_DOMAIN_PATH,
                                GdFindAndReplaceHrefTextHrefPart.QUERY_AND_FRAGMENT))
                        .withSearch("*")
                        .withReplace(newUrl));

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        checkResultOfSuccessfulUpdate(result);

        TextBanner banner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        assertThat("href обновлен некорректно", banner.getHref(), is(newUrl));
    }

    //find and replace
    @Test
    public void successfullyReplaceInBanner() {
        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        checkResultOfSuccessfulUpdate(result);

        TextBanner banner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        assertThat("href обновлен некорректно", banner.getHref(), is("http://dce.com/path/1?q1=p1#fr"));
    }

    @Test
    public void successfullyReplaceInSitelink() {
        validInput.withTargetTypes(singletonSet(GdFindAndReplaceAdsHrefTargetType.SITELINK_HREF));

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        checkResultOfSuccessfulUpdate(result);

        TextBanner updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());
        Long sitelinkSetId = updatedBanner.getSitelinksSetId();
        assertThat("не привязан новый сайтлинк сет", sitelinkSetId, not(sitelinkSetForUpdate.getSitelinkSetId()));
        SitelinkSet newSitelinkSet =
                sitelinkSetRepository.get(clientInfo.getShard(), singletonList(sitelinkSetId)).get(0);
        Long sitelinkId = newSitelinkSet.getSitelinks().get(1).getId();
        Sitelink sitelink = sitelinkRepository.get(clientInfo.getShard(), singletonList(sitelinkId)).get(0);
        assertThat("href обновлен некорректно", sitelink.getHref(), is("http://dce.com/another_path?q1=p1#fr"));
    }

    @Test
    public void replaceOnlyInSitelink_BannerNewHrefIsNull() {
        TextBannerInfo textBanner = steps.bannerSteps().createBanner(activeTextBanner()
                .withHref("http://replace.net")
                .withSitelinksSetId(sitelinkSetForUpdate.getSitelinkSetId()), clientInfo);

        validInput
                .withAdIds(singletonList(textBanner.getBannerId()))
                .withTargetTypes(ALL_TARGETS);

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        checkResultOfSuccessfulUpdate(result);
        assertThat("если ссылка баннера не была обновлена, newHref должен быть null",
                result.getRowset().get(0).getNewHref(), nullValue());
    }

    @Test
    public void successfullyReplaceInBannerAndSitelink() {
        validInput.withTargetTypes(ALL_TARGETS);
        validInput.getReplaceInstruction().withSearch("path").withReplace("replace");

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        checkResultOfSuccessfulUpdate(result);

        TextBanner updatedBanner = getBanner(clientInfo.getShard(), bannerForUpdate.getBannerId());

        assertThat("href баннера обновлен некорректно", updatedBanner.getHref(),
                is("http://abc.com/replace/1?q1=p1#fr"));

        Long sitelinkSetId = updatedBanner.getSitelinksSetId();
        assertThat("не привязан новый сайтлинк сет", sitelinkSetId, not(sitelinkSetForUpdate.getSitelinkSetId()));
        SitelinkSet newSitelinkSet =
                sitelinkSetRepository.get(clientInfo.getShard(), singletonList(sitelinkSetId)).get(0);
        Long sitelinkId = newSitelinkSet.getSitelinks().get(1).getId();
        Sitelink sitelink = sitelinkRepository.get(clientInfo.getShard(), singletonList(sitelinkId)).get(0);
        assertThat("href сайтлинка обновлен некорректно", sitelink.getHref(),
                is("http://abc.com/another_replace?q1=p1#fr"));
    }

    @Test
    public void replaceIsNotRequired() {
        validInput.getReplaceInstruction().withSearch("nosuchtext").withReplace("replace");

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        assertThat("результат должен быть пустым", result, beanDiffer(getEmptyPayload()));
    }

    @Test
    public void bannerIsInExceptionsSitelinksNotInTarget_EmptyPayload() {
        validInput.withAdIdsHrefExceptions(singletonSet(bannerForUpdate.getBannerId()));

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        assertThat("результат должен быть пустым", result, beanDiffer(getEmptyPayload()));
    }

    //validation
    @Test
    public void coreValidation() {
        validInput.getReplaceInstruction().withSearch("abc").withReplace(RandomStringUtils.randomAlphanumeric(64));
        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
        Path expectedPath = path(index(0), field(OldBanner.HREF.name()));
        assertThat(result.getValidationResult().getErrors(), contains(new GdDefect()
                .withPath(expectedPath.toString())
                .withCode(invalidHref().defectId().getCode())
        ));
    }

    @Test
    public void validationErrorWhenInstructionIsNull() {
        validInput.withReplaceInstruction(null);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.REPLACE_INSTRUCTION.name(),
                        new Defect<>(DefectIds.CANNOT_BE_NULL)))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenHrefPartsIsNull() {
        validInput.getReplaceInstruction().withHrefParts(null);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.REPLACE_INSTRUCTION.name() +
                                "." + GdFindAndReplaceHrefTextInstruction.HREF_PARTS.name(),
                        notNull()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenHrefPartsIsEmpty() {
        validInput.getReplaceInstruction().withHrefParts(emptySet());
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.REPLACE_INSTRUCTION.name() +
                                "." + GdFindAndReplaceHrefTextInstruction.HREF_PARTS.name(),
                        notEmptyCollection()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenSomeHrefPartIsNull() {
        validInput.getReplaceInstruction().withHrefParts(singletonSet(null));
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.REPLACE_INSTRUCTION.name() +
                                "." + GdFindAndReplaceHrefTextInstruction.HREF_PARTS.name(),
                        notContainNulls()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenSearchIsBlank() {
        validInput.getReplaceInstruction().withSearch("");
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.REPLACE_INSTRUCTION.name() +
                                "." + GdFindAndReplaceHrefTextInstruction.SEARCH.name(),
                        notEmptyString()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenReplaceIsNull() {
        validInput.getReplaceInstruction().withReplace(null);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.REPLACE_INSTRUCTION.name() +
                                "." + GdFindAndReplaceHrefTextInstruction.REPLACE.name(),
                        notNull()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenReplaceIsBlank() {
        validInput.getReplaceInstruction().withReplace("");
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.REPLACE_INSTRUCTION.name() +
                                "." + GdFindAndReplaceHrefTextInstruction.REPLACE.name(),
                        notEmptyString()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenTargetTypesIsNull() {
        validInput.withTargetTypes(null);
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.TARGET_TYPES.name(),
                        notNull()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenTargetTypesIsEmpty() {
        validInput.withTargetTypes(emptySet());
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.TARGET_TYPES.name(),
                        notEmptyCollection()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    @Test
    public void validationErrorWhenSomeTargetTypeIsNull() {
        validInput.withTargetTypes(singletonSet(null));
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(REPLACE_HREF_TEXT_ARG_NAME + "." + GdFindAndReplaceAdsHrefText.TARGET_TYPES.name(),
                        notContainNulls()))));
        serviceUnderTest.replace(validInput, operatorUid, clientId, REPLACE_HREF_TEXT_ARG_NAME);
    }

    private void checkResultOfSuccessfulUpdate(GdResult<GdFindAndReplaceAdsHrefPayloadItem> result) {
        assertThat("присутствуют ошибки валидации", result.getValidationResult(), nullValue());
        assertThat("неверные id обновленных баннеров",
                mapList(result.getRowset(), GdFindAndReplaceAdsHrefPayloadItem::getAdId),
                containsInAnyOrder(validInput.getAdIds().toArray()));
        assertThat("неверное количество обновленных баннеров", result.getSuccessCount(),
                is(1));
    }

    private TextBanner getBanner(int shard, long bannerId) {
        return bannerRepository.getStrictly(shard, singletonList(bannerId), TextBanner.class).get(0);
    }
}
