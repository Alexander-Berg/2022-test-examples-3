package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.IdModFilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;

@RunWith(Parameterized.class)
public class ModFilterValidationServiceTest {

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(value = 0)
    public Long divisor;
    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(value = 1)
    public Long reminder;
    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(value = 2)
    public String expectedMessage;

    private ModFilterValidationService modFilterValidationService;
    private IdModFilter filterParam;

    @SuppressWarnings("RedundantArrayCreation")
    @Parameterized.Parameters(name = "divisor {0} reminder {1}")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {0L, 1L,
                        "divisor must be greater than 1"},
                {2L, -1L,
                        "reminder must be greater than or equal to 0"},
                {2L, 3L,
                        "divisor must be greater than reminder"},

        });
    }

    @Before
    public void setUp() throws Exception {
        modFilterValidationService = new ModFilterValidationService();
        filterParam = new IdModFilter(divisor, reminder);
    }

    @Test
    public void expectException() {
        String validationError = getErrorText(modFilterValidationService.validate(filterParam));
        assertThat("должна быть ошибка валидации", validationError, equalTo(expectedMessage));
    }
}
