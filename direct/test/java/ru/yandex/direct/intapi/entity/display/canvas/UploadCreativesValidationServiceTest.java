package ru.yandex.direct.intapi.entity.display.canvas;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeUploadData;
import ru.yandex.direct.intapi.entity.display.canvas.validation.UploadCreativesValidationService;
import ru.yandex.direct.validation.result.PathHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class UploadCreativesValidationServiceTest {

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(0)
    public List<CreativeUploadData> creatives;

    @Parameterized.Parameter(1)
    @SuppressWarnings("WeakerAccess")
    public String expectedMessage;

    @Autowired
    private UploadCreativesValidationService uploadCreativesValidationService;

    @SuppressWarnings("RedundantArrayCreation")
    @Parameterized.Parameters(name = "message {1} ")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {null, "creatives cannot be null"},
                {Collections.emptyList(), "creatives cannot be empty"},
                {Collections.singletonList(null), "creatives[0] cannot be null"},
        });
    }

    @Before
    public void setUp() throws Exception {
        uploadCreativesValidationService = new UploadCreativesValidationService();
    }

    @Test
    public void expectException() {
        String validationError =
                getErrorText(uploadCreativesValidationService.validate(creatives), path(PathHelper.field("creatives")));
        assertThat("должна быть ошибка валидации", validationError, equalTo(expectedMessage));
    }

}
