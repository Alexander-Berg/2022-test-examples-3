package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.templateIsValid;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidTemplateQuery;

public class BannerTextConstraintsTemplateTest extends BannerTextConstraintsBaseTest {

    public BannerTextConstraintsTemplateTest() {
        super(templateIsValid());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"без шаблона", "без шаблона", null},
                {"пустой шаблон", "##", null},
                {"корректный шаблон 1", "#мебель#", null},
                {"корректный шаблон 2", "купить #фрукты# в Москве", null},
                {"корректный шаблон 2", "купить#фрукты#Москва", null},
                {"неправильный шаблон 1", "купить#вещи", null},
                {"неправильный шаблон 2", "купить #вещи##", null},
                {"неправильный шаблон 3", "#hash_tag", null},
                {"шаблонов больше одного", "#купить# #машину#", invalidTemplateQuery()},
        });
    }
}
