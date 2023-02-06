package ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhrasePredicates.validMinusMark;

@RunWith(Parameterized.class)
public class PhrasePredicatesMinusMarkPositiveTest {

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"-н"},
                {"-2"},
                {"-на"},
                {"-123"},

                {" -на "},
                {"[-на]"},
                {"\"-на\""},

                {"-!на"},
                {" -!на "},
                {"[-!на]"},
                {"\"-!на\""},

                {"-+на"},
                {" -+на "},
                {"[-+на]"},
                {"\"-+на\""},

                {"из-за"},
                {"[санкт-петербург]"},
                {"\"санкт-петербург\""},

                {"из-за"},
                {"[-санкт-петербург]"},
                {"\"-санкт-петербург\""},

                {"полет -на луну"},

                {"-на -луну -санкт-петербург"},
                {"полет -на -луну -санкт-петербург"}
        });
    }

    @Parameterized.Parameter
    public String keyword;

    @Test
    public void testParameterized() {
        assertThat(validMinusMark().test(keyword), is(true));
    }
}
