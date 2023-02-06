package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.stringIsNotBlank;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.stringShouldNotBeBlank;

public class BannerTextConstraintsNotBlankTest extends BannerTextConstraintsBaseTest {

    public BannerTextConstraintsNotBlankTest() {
        super(stringIsNotBlank());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"пустая строка", "", stringShouldNotBeBlank()},
                {"строка из одного пробела", " ", stringShouldNotBeBlank()},
                {"строка из одного переноса строки", "\n", stringShouldNotBeBlank()},
                {"строка из нескольких пробелов", "    ", stringShouldNotBeBlank()},
                {"строка из одной табуляции", " ", stringShouldNotBeBlank()},
                {"строка из одной буквы", "a", null},
                {"строка с буквами и пробелами", "a b", null},
        });
    }
}
