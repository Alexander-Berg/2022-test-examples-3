package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.withoutTemplates;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotHaveTemplate;

public class BannerTextConstraintsWithoutTemplatesTest extends BannerTextConstraintsBaseTest {

    public BannerTextConstraintsWithoutTemplatesTest() {
        super(withoutTemplates());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"без шаблона", "без шаблона", null},
                {"пустой шаблон", "##", cannotHaveTemplate()},
                {"корректный шаблон 1", "#мебель#", cannotHaveTemplate()},
                {"корректный шаблон 2", "купить #фрукты# в Москве", cannotHaveTemplate()},
                {"корректный шаблон 2", "купить#фрукты#Москва", cannotHaveTemplate()},
                {"неправильный шаблон 1", "купить#вещи", null},
                {"неправильный шаблон 2", "купить #вещи##", cannotHaveTemplate()},
                {"неправильный шаблон 3", "#hash_tag", null},
                {"шаблонов больше одного", "#купить# #машину#", cannotHaveTemplate()},
        });
    }

}
