package ru.yandex.direct.grid.processing.service.validation;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceOptions;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceText;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceTextInstruction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsTargetType.TITLE;
import static ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceLinkMode.FULL;
import static ru.yandex.direct.validation.result.PathHelper.pathFromStrings;

@ParametersAreNonnullByDefault
public class GridValidationServiceFindAndReplaceSuccessTest {
    private static final String REPLACE_TEXT_ARG_NAME = "input";

    @Mock
    private GridValidationResultConversionService validationResultConversionService;

    @InjectMocks
    private GridValidationService service;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        doReturn(new GdValidationResult())
                .when(validationResultConversionService).buildGridValidationResult(any(), any());
    }

    @Test
    public void testCompleteRequest() {
        var options = new GdFindAndReplaceOptions()
                .withCaseSensitive(true)
                .withLinkReplacementMode(FULL);
        var request = new GdFindAndReplaceText()
                .withAdIds(List.of(1L))
                .withTargetTypes(Set.of(TITLE))
                .withReplaceInstruction(
                        new GdFindAndReplaceTextInstruction()
                                .withSearch("a")
                                .withReplace("b")
                                .withOptions(options));
        service.validateFindAndReplaceBannerTextRequest(request, pathFromStrings(REPLACE_TEXT_ARG_NAME));
    }
}
