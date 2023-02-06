package ru.yandex.direct.grid.processing.service.validation;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceLinkMode;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceOptions;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceText;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceTextInstruction;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdReplacementMode.FIND_AND_REPLACE;
import static ru.yandex.direct.validation.result.PathHelper.pathFromStrings;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GridValidationServiceFindAndReplaceFailureTest {
    private static final String REPLACE_TEXT_ARG_NAME = "input";

    private static final GdFindAndReplaceOptions DEFAULT_OPTIONS = new GdFindAndReplaceOptions()
            .withCaseSensitive(true)
            .withReplacementMode(FIND_AND_REPLACE)
            .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL);

    @Mock
    private GridValidationResultConversionService validationResultConversionService;

    @InjectMocks
    private GridValidationService service;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GdFindAndReplaceText request;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> parameters() {
        return List.of(new Object[][]{
                {
                    "No ad ids (null)",
                    new GdFindAndReplaceText()
                            .withAdIds(null)
                },
                {
                    "No ad ids (empty list)",
                    new GdFindAndReplaceText()
                            .withAdIds(List.of())
                },
                {
                    "Illegal ad id",
                    new GdFindAndReplaceText()
                            .withAdIds(List.of(-1L))
                },
                {
                    "Duplicate ad id",
                    new GdFindAndReplaceText()
                            .withAdIds(List.of(1L, 1L))
                },
                {
                    "No replace instruction",
                    new GdFindAndReplaceText()
                            .withAdIds(List.of(1L))
                            .withReplaceInstruction(null)
                },
                {
                    "Search string is null",
                    new GdFindAndReplaceText()
                        .withAdIds(List.of(1L))
                        .withReplaceInstruction(
                                new GdFindAndReplaceTextInstruction()
                                        .withSearch(null)
                                        .withReplace("a")
                                        .withOptions(DEFAULT_OPTIONS))
                },
                {
                    "Empty search string",
                    new GdFindAndReplaceText()
                        .withAdIds(List.of(1L))
                        .withReplaceInstruction(
                                new GdFindAndReplaceTextInstruction()
                                        .withSearch("")
                                        .withReplace("a")
                                        .withOptions(DEFAULT_OPTIONS))
                },
                {
                    "Replacement is null",
                    new GdFindAndReplaceText()
                        .withAdIds(List.of(1L))
                        .withReplaceInstruction(
                                new GdFindAndReplaceTextInstruction()
                                        .withSearch("a")
                                        .withReplace(null)
                                        .withOptions(DEFAULT_OPTIONS))
                },
                {
                    "Emptry replacement",
                    new GdFindAndReplaceText()
                            .withAdIds(List.of(1L))
                            .withReplaceInstruction(
                                    new GdFindAndReplaceTextInstruction()
                                            .withSearch("a")
                                            .withReplace("")
                                            .withOptions(DEFAULT_OPTIONS))
                },
                {
                    "No target types (null)",
                    new GdFindAndReplaceText()
                        .withAdIds(List.of(1L))
                        .withReplaceInstruction(
                                new GdFindAndReplaceTextInstruction()
                                        .withSearch("a")
                                        .withReplace("b")
                                        .withOptions(DEFAULT_OPTIONS))
                        .withTargetTypes(null)
                },
                {
                    "No target types (empty set)",
                    new GdFindAndReplaceText()
                        .withAdIds(List.of(1L))
                        .withReplaceInstruction(
                                new GdFindAndReplaceTextInstruction()
                                        .withSearch("a")
                                        .withReplace("b")
                                        .withOptions(DEFAULT_OPTIONS))
                        .withTargetTypes(Set.of())
                },
                {
                    "Search string is null with FIND_AND_REPLACE mode",
                    new GdFindAndReplaceText()
                            .withAdIds(List.of(1L))
                            .withReplaceInstruction(
                                    new GdFindAndReplaceTextInstruction()
                                            .withSearch(null)
                                            .withReplace("b")
                                            .withOptions(new GdFindAndReplaceOptions()
                                                    .withCaseSensitive(true)
                                                    .withReplacementMode(FIND_AND_REPLACE)
                                                    .withLinkReplacementMode(GdFindAndReplaceLinkMode.FULL)))
                            .withTargetTypes(Set.of(GdFindAndReplaceAdsTargetType.TITLE))
                }
        });
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        doReturn(new GdValidationResult())
                .when(validationResultConversionService).buildGridValidationResult(any(), any());
    }

    @Test
    public void checkValidation() {
        assertThatThrownBy(() ->
                service.validateFindAndReplaceBannerTextRequest(request, pathFromStrings(REPLACE_TEXT_ARG_NAME)))
                .isInstanceOf(GridValidationException.class);
    }
}
