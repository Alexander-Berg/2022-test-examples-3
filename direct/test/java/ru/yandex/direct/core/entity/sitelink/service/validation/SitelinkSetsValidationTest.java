package ru.yandex.direct.core.entity.sitelink.service.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.turbolanding.service.TurboLandingService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants.MAX_SITELINK_DESC_LENGTH;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants.MAX_SITELINK_TITLE_LENGTH;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants.SITELINKS_MAX_LENGTH;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkConstants.SITELINKS_PER_BLOCK;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.duplicateSitelinkDescs;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.duplicateSitelinkTitles;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.maxSetSitelinkTitlesSize;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.maxSitelinksFirstBlockTitlesLength;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkDefects.maxSitelinksSecondBlockTitlesLength;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkSetDefects.sitelinkSetInUse;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkSetValidationService.MAX_SITELINKS_PER_SET;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkSetValidationService.MIN_SITELINKS_PER_SET;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class SitelinkSetsValidationTest {

    private static final int SHARD = 1;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    private SitelinkSetValidationService sitelinkSetValidationService;
    private SitelinkSetRepository sitelinkSetRepository;

    @Autowired
    private TurboLandingService turboLandingService;

    @Autowired
    private FeatureService featureService;

    private Sitelink firstSitelink;
    private Sitelink secondSitelink;


    @Before
    public void before() {
        SitelinkValidationService sitelinkValidationService = new SitelinkValidationService();
        sitelinkSetRepository = mock(SitelinkSetRepository.class);
        when(sitelinkSetRepository.getSitelinkSetIdsMapUsed(SHARD, CLIENT_ID, singletonList(1L)))
                .thenReturn(singletonMap(1L, Boolean.FALSE));
        when(sitelinkSetRepository.getSitelinkSetIdsMapUsed(SHARD, CLIENT_ID, singletonList(2L)))
                .thenReturn(emptyMap());
        when(sitelinkSetRepository.getSitelinkSetIdsMapUsed(SHARD, CLIENT_ID, singletonList(3L)))
                .thenReturn(singletonMap(3L, Boolean.TRUE));
        when(sitelinkSetRepository.getSitelinkSetIdsMapUsed(SHARD, CLIENT_ID, singletonList(4L)))
                .thenReturn(singletonMap(4L, Boolean.FALSE));
        sitelinkSetValidationService =
                new SitelinkSetValidationService(
                        sitelinkValidationService,
                        turboLandingService,
                        sitelinkSetRepository,
                        featureService
                );

        firstSitelink = defaultSitelink();
        secondSitelink = defaultSitelink2();
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(defaultSitelinksSet());
        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void validate_SitelinksNull() {
        ValidationResult<SitelinkSet, Defect> actual =
                validateOneSitelinksSet(defaultSitelinksSet().withSitelinks(null));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("sitelinks")),
                        notNull())));
    }

    @Test
    public void validate_SitelinklistSitelinkNull() {
        ValidationResult<SitelinkSet, Defect> actual =
                validateOneSitelinksSet(defaultSitelinksSet().withSitelinks(singletonList(null)));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("sitelinks"), index(0)),
                        notNull())));
    }

    @Test
    public void validate_SitelinksEmptyList() {
        ValidationResult<SitelinkSet, Defect> actual =
                validateOneSitelinksSet(defaultSitelinksSet().withSitelinks(emptyList()));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("sitelinks")),
                        SitelinkSetDefects.sitelinkCountMustBeBetween(MIN_SITELINKS_PER_SET, MAX_SITELINKS_PER_SET))));
    }

    @Test
    public void validate_SitelinksListGreaterMax() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(Arrays.asList(firstSitelink, secondSitelink,
                        new Sitelink().withTitle("t3").withHref("http://video.ya.ru"),
                        new Sitelink().withTitle("t4").withHref("http://audio.ya.ru"),
                        new Sitelink().withTitle("t5").withHref("http://sound.ya.ru"),
                        new Sitelink().withTitle("t6").withHref("http://shmideo.ya.ru"),
                        new Sitelink().withTitle("t7").withHref("http://shmaudio.ya.ru"),
                        new Sitelink().withTitle("t8").withHref("http://shmound.ya.ru"),
                        new Sitelink().withTitle("t9").withHref("http://shm.ya.ru"))));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("sitelinks")),
                        SitelinkSetDefects.sitelinkCountMustBeBetween(MIN_SITELINKS_PER_SET, MAX_SITELINKS_PER_SET))));
    }

    @Test
    public void validate_SitelinksTitlesMaxLength() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(
                        Arrays.asList(firstSitelink.withTitle("VeryVeryBigSitelinkTitle"),
                                new Sitelink().withTitle("VeryVeryBigSitelinkTitle2").withHref("http://video.ya.ru"),
                                secondSitelink.withTitle("VeryVeryBigSitelinkTitle3"))));

        assertThat(actual.flattenErrors(), contains(
                validationError(path(field("sitelinks")), maxSetSitelinkTitlesSize(SITELINKS_MAX_LENGTH))));
    }

    @Test
    public void validate_SitelinksTitlesMaxLengthBlock1() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(generateSitelinks(5, MAX_SITELINK_TITLE_LENGTH,
                        MAX_SITELINK_DESC_LENGTH))
        );

        assertThat(actual.flattenErrors(), contains(
                validationError(path(field("sitelinks")), maxSitelinksFirstBlockTitlesLength(SITELINKS_MAX_LENGTH))));
    }

    @Test
    public void validate_SitelinksTitlesMaxLengthBlock2() {
        var siteLinks = generateSitelinks(SITELINKS_PER_BLOCK, 15, MAX_SITELINK_DESC_LENGTH);
        var invalidSecondBlock = generateSitelinks(SITELINKS_PER_BLOCK, MAX_SITELINK_TITLE_LENGTH, MAX_SITELINK_DESC_LENGTH);
        siteLinks.addAll(invalidSecondBlock);
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(siteLinks));

        assertThat(actual.flattenErrors(), contains(
                validationError(path(field("sitelinks")), maxSitelinksSecondBlockTitlesLength(SITELINKS_MAX_LENGTH))));
    }

    @Test
    public void validate_maxSitelinksCountAndLength(){
        var siteLinks = generateSitelinks(MAX_SITELINKS_PER_SET, MAX_SITELINK_TITLE_LENGTH, MAX_SITELINK_DESC_LENGTH);
        var result = sitelinkSetValidationService.validateOneSitelinksSet(defaultSitelinksSet().withSitelinks(siteLinks),
                new HashSet<>(), true, true);

        assertThat(result.flattenErrors(), hasSize(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validate_SitelinksTitlesUnique() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(
                        Arrays.asList(firstSitelink, secondSitelink.withTitle(firstSitelink.getTitle()))));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("sitelinks"), index(0)), duplicateSitelinkTitles()),
                        validationError(path(field("sitelinks"), index(1)), duplicateSitelinkTitles())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validate_SitelinksHrefsUnique() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(
                        Arrays.asList(firstSitelink, secondSitelink.withHref(firstSitelink.getHref()))));

        assertThat(actual.flattenErrors(), empty());
    }

    @Test
    public void validate_SitelinksHrefsWithTurboLandingURL() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(
                        Arrays.asList(firstSitelink, secondSitelink.withHref("https://yandex.ru/turbo?zzz"))));

        assertThat(actual, hasNoDefectsDefinitions());
    }


    @Test
    @SuppressWarnings("unchecked")
    public void validate_SitelinksDescsUnique() {
        ValidationResult<SitelinkSet, Defect> actual = validateOneSitelinksSet(
                defaultSitelinksSet().withSitelinks(
                        Arrays.asList(firstSitelink,
                                secondSitelink.withDescription(firstSitelink.getDescription()))));

        assertThat(actual.flattenErrors(),
                contains(validationError(path(field("sitelinks"), index(0)), duplicateSitelinkDescs()),
                        validationError(path(field("sitelinks"), index(1)), duplicateSitelinkDescs())));
    }

    @Test
    public void validate_SitelinkTurbolanding() {
        SitelinkSet sitelinkSet = defaultSitelinksSet().withSitelinks(
                singletonList(firstSitelink.withTurboLandingId(1L)));

        ValidationResult<SitelinkSet, Defect> vr = sitelinkSetValidationService.validateOneSitelinksSet(
                sitelinkSet, new HashSet<>(singletonList(1L)), true, false);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    //validate delete

    @Test
    public void validateDelete_positiveValidationResultWithoutBanner() {
        ValidationResult<List<Long>, Defect> actual =
                sitelinkSetValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(1L));

        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void validateDelete_positiveValidationResultWithEmptyCamp() {
        ValidationResult<List<Long>, Defect> actual =
                sitelinkSetValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(4L));

        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void validateDelete_ListNull() {
        ValidationResult<List<Long>, Defect> actual =
                sitelinkSetValidationService.validateDelete(SHARD, CLIENT_ID, null);

        assertThat(actual.flattenErrors(), contains(validationError(path(), notNull())));
    }

    @Test
    public void validateDelete_SitelinkSetIdNull() {
        ValidationResult<List<Long>, Defect> actual =
                sitelinkSetValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(null));

        assertThat(actual.flattenErrors(), contains(validationError(path(index(0)), notNull())));
    }

    @Test
    public void validateDelete_SitelinkSetIdAnotherClient() {
        ValidationResult<List<Long>, Defect> actual =
                sitelinkSetValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(2L));

        assertThat(actual.flattenErrors(), contains(validationError(path(index(0)), objectNotFound())));
    }

    @Test
    public void validateDelete_SitelinkSetIdInUse() {
        ValidationResult<List<Long>, Defect> actual =
                sitelinkSetValidationService.validateDelete(SHARD, CLIENT_ID, singletonList(3L));

        assertThat(actual.flattenErrors(), contains(validationError(path(index(0)), sitelinkSetInUse())));
    }

    private SitelinkSet defaultSitelinksSet() {
        return new SitelinkSet().withClientId(CLIENT_ID.asLong()).withSitelinks(singletonList(firstSitelink));
    }

    private ValidationResult<SitelinkSet, Defect> validateOneSitelinksSet(SitelinkSet sitelinkSet) {
        return sitelinkSetValidationService.validateOneSitelinksSet(sitelinkSet, new HashSet<>(), true, false);
    }

    private List<Sitelink> generateSitelinks(int siteLinksCount, int titleLength, int descriptionLength){
        return IntStream.rangeClosed(1, siteLinksCount).mapToObj(i ->
                        new Sitelink()
                                .withHref("https://yandex.com")
                                .withTitle(i + "" + RandomStringUtils.randomAlphabetic(titleLength - 1))
                                .withDescription(i + "" + RandomStringUtils.randomAlphabetic(descriptionLength - 1)))
                .collect(Collectors.toList());
    }
}
