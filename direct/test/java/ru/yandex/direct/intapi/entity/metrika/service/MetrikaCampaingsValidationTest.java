package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.PathHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class MetrikaCampaingsValidationTest {

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(value = 0)
    public List<Long> ids;

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(value = 1)
    public String expectedMessage;

    @Autowired
    private MetrikaCampaignsService metrikaCampaignsService;

    @SuppressWarnings("RedundantArrayCreation")
    @Parameterized.Parameters(name = "message {1} ")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {Collections.singletonList(null), "params[0] cannot be null"},
                {Collections.emptyList(), "params cannot be empty"},
                {Collections.singletonList(0L), "params[0] must be greater than 0"},
                {Arrays.asList(1L, null), "params[1] cannot be null"}
        });
    }

    @Before
    public void setUp() throws Exception {
        metrikaCampaignsService = new MetrikaCampaignsService();
    }

    @Test
    public void expectException() {
        String validationError =
                getErrorText(metrikaCampaignsService.validate(ids), path(PathHelper.field("params")));
        assertThat("должна быть ошибка валидации", validationError, equalTo(expectedMessage));
    }
}
