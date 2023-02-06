package ru.yandex.direct.web.entity.keyword.stat.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.entity.keyword.stat.model.KeywordStatShowsBulkRequest;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusKeywords;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.keyword.stat.service.KeywordStatShowsValidationService.COMMON_MINUS_WORDS_MAX_LENGTH;

@RunWith(Parameterized.class)
public class KeywordStatShowsValidationTest {

    private static final String CORRECT_GEO = "1";
    private static final String PHRASE = "test";

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(0)
    public KeywordStatShowsBulkRequest request;

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(1)
    public Path expectedPath;

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(3)
    public String message;

    private KeywordStatShowsValidationService keywordStatShowsValidationService;

    @SuppressWarnings("RedundantArrayCreation")
    @Parameterized.Parameters(name = "message {3} ")
    public static Collection params() {
        return asList(new Object[][]{
                {null, path(), notNull(), "request cannot be null"},
                {new KeywordStatShowsBulkRequest()
                        .withPhrases(Collections.singletonList(PHRASE)), path(field("geo")),
                        notNull(), "request.geo cannot be null"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo("")
                        .withPhrases(Collections.singletonList(PHRASE)), path(field("geo")),
                        notEmptyString(), "request.geo cannot be empty"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO), path(field("phrases")),
                        notNull(), "request.phrases cannot be null"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO)
                        .withPhrases(Collections.emptyList()), path(field("phrases")),
                        notEmptyCollection(), "request.phrases cannot be empty"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO)
                        .withPhrases(Collections.singletonList(null)), path(field("phrases"), index(0)),
                        notNull(), "request.phrases[0] cannot be null"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO)
                        .withPhrases(Collections.singletonList("")), path(field("phrases"), index(0)),
                        notEmptyString(), "request.phrases[0] cannot be empty"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO)
                        .withPhrases(Collections.singletonList(PHRASE))
                        .withCommonMinusPhrases(
                        Collections.singletonList(null)),
                        path(field("commonMinusPhrases"), index(0)),
                        notNull(), "request.commonMinusPhrases[0] cannot be null"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO)
                        .withPhrases(Collections.singletonList(PHRASE))
                        .withCommonMinusPhrases(
                        Collections.singletonList("")),
                        path(field("commonMinusPhrases"), index(0)),
                        notEmptyString(), "request.commonMinusPhrases[0] cannot be empty"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO)
                        .withPhrases(Collections.singletonList(PHRASE))
                        .withCommonMinusPhrases(
                        asList(RandomStringUtils.randomAlphabetic(COMMON_MINUS_WORDS_MAX_LENGTH), "p")),
                        path(field("commonMinusPhrases")),
                        maxLengthMinusKeywords(COMMON_MINUS_WORDS_MAX_LENGTH),
                        "request.commonMinusPhrases more than max"},
                {new KeywordStatShowsBulkRequest()
                        .withGeo(CORRECT_GEO)
                        .withPhrases(List.of("-test")),
                        path(field("phrases"), index(0)),
                        new Defect<>(PhraseDefectIds.Gen.NO_PLUS_WORDS),
                        "request.phrases should have valid syntax"},
        });
    }

    @Before
    public void setUp() {
        keywordStatShowsValidationService = new KeywordStatShowsValidationService();
    }

    @Test
    public void expectException() {
        ValidationResult<KeywordStatShowsBulkRequest, Defect> vr = keywordStatShowsValidationService.validate(request);
        assertThat(vr.flattenErrors(), contains(validationError(expectedPath, expectedDefect)));
    }
}
