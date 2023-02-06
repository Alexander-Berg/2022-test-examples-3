package ru.yandex.direct.intapi.entity.metrika.service.objectinfo.validation;

import java.util.Collection;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class MetrikaTimeTokenConstraintTest {

    @Parameterized.Parameter(0)
    public String timeToken;

    @Parameterized.Parameter(1)
    public Matcher matcher;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"2017-01-24T12:20:15/123", nullValue()},
                {null, nullValue()},

                {"2017-01-24T12:20:15//123", notNullValue()},
                {"2017-01-24T12:20:15/123/", notNullValue()},
                {"2017-01-24T12:20:15/1f", notNullValue()},
                {"2017-01-24T12:20:15/a", notNullValue()},
                {"2017-01-24T12:20:15/", notNullValue()},
                {"2017-01-24T12:20:15/_", notNullValue()},
                {"2017-01-24T12:20:15:", notNullValue()},
                {"2017-01-24T12:20:15", notNullValue()},
                {"123/123", notNullValue()},
                {"a/123", notNullValue()},
                {"/123", notNullValue()},
                {"_/123", notNullValue()},
                {"123", notNullValue()},
                {"/", notNullValue()},
                {"//", notNullValue()},
                {"a", notNullValue()},
                {"", notNullValue()},
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConstraint() {
        assertThat(MetrikaTimeTokenConstraint.validTimeToken().apply(timeToken), matcher);
    }
}
