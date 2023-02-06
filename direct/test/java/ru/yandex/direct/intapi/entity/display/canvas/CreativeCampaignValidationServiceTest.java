package ru.yandex.direct.intapi.entity.display.canvas;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeCampaignRequest;
import ru.yandex.direct.intapi.entity.display.canvas.validation.CreativeCampaignValidationService;
import ru.yandex.direct.validation.result.PathHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CreativeCampaignValidationServiceTest {

    @Parameterized.Parameter(0)
    public CreativeCampaignRequest request;

    @Parameterized.Parameter(1)
    public String expectedMessage;

    @Autowired
    private CreativeCampaignValidationService creativeCampaignValidationService;

    @SuppressWarnings("RedundantArrayCreation")
    @Parameterized.Parameters(name = "message {1} ")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {null, "request cannot be null"},
                {new CreativeCampaignRequest().withCreativeIds(emptyList()), "request.creativeIds cannot be empty"},
                {new CreativeCampaignRequest().withCreativeIds(singletonList(null)),
                        "request.creativeIds[0] cannot be null"},
        });
    }

    @Before
    public void setUp() throws Exception {
        creativeCampaignValidationService = new CreativeCampaignValidationService();
    }

    @Test
    public void expectException() {
        String validationError =
                getErrorText(creativeCampaignValidationService.validate(request), path(PathHelper.field("request")));
        MatcherAssert.assertThat("должна быть ошибка валидации", validationError, equalTo(expectedMessage));
    }

}
