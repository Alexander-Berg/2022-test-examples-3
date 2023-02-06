package ru.yandex.direct.intapi.entity.metrika.service.objectinfo.validation;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.intapi.entity.metrika.model.objectinfo.MetrikaObjectInfoRequest;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.intapi.entity.metrika.service.objectinfo.validation.MetrikaObjectInfoRequestValidator.validateObjectInfoRequest;

@RunWith(Parameterized.class)
public class MetrikaObjectInfoRequestValidatorTest {

    @Parameterized.Parameter(0)
    public String timeToken;

    @Parameterized.Parameter(1)
    public Integer limit;

    @Parameterized.Parameter(2)
    public boolean errorsExpected;

    @Parameterized.Parameters(name = "timeToken: {0}, limit: {1}. Is errors expected: {2}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"2017-01-24T12:20:15/123", 1, false},
                {"2017-01-24T12:20:15/123", 10000, false},
                {null, 10000, false},
                {"2017-01-24T12:20:15/123", null, false},
                {null, null, false},

                // неверный тайм-токен
                {"abc", 10, true},

                // неверный лимит
                {"2017-01-24T12:20:15/123", 0, true},
                {"2017-01-24T12:20:15/123", -1, true},
                {null, 0, true},
                {null, -1, true},
        });
    }

    @Test
    public void testValidator() {
        MetrikaObjectInfoRequest request = new MetrikaObjectInfoRequest(timeToken, limit);
        assertThat(validateObjectInfoRequest(request).hasAnyErrors(), is(errorsExpected));
    }
}
