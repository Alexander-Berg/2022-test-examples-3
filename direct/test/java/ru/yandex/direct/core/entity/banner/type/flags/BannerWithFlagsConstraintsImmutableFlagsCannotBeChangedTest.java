package ru.yandex.direct.core.entity.banner.type.flags;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.insufficientRights;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.age;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.alcohol;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.babyFood;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.empty;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithFlagsConstraintsImmutableFlagsCannotBeChangedTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public BannerFlags oldFlags;

    @Parameterized.Parameter(2)
    public BannerFlags newFlags;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // ничего не меняем
                {
                        "null -> {}",
                        null, empty(), null
                },
                {
                        "{} -> {}",
                        empty(), empty(), null
                },
                {
                        "{AGE=18} -> {AGE=18}",
                        age(18), age(18), null
                },
                {
                        "{ALCOHOL} -> {ALCOHOL}",
                        alcohol(), alcohol(), null
                },

                // добавление флагов
                {
                        "{} -> {AGE=18}",
                        empty(), age(18), null
                },
                {
                        "{ALCOHOL} -> {ALCOHOL, AGE=18}",
                        alcohol(), alcohol().with(BannerFlags.AGE, Age.AGE_18), null
                },
                {
                        "{} -> {ALCOHOL}",
                        empty(), alcohol(), insufficientRights()
                },

                // изменение значений флагов
                {
                        "{AGE=16} -> {AGE=18}",
                        age(16), age(18), null,
                },
                {
                        "{ALCOHOL, AGE=16} -> {ALCOHOL, AGE=18}",
                        alcohol().with(BannerFlags.AGE, Age.AGE_16), alcohol().with(BannerFlags.AGE, Age.AGE_18), null
                },
                {
                        "{AGE=6} -> {BABY_FOOD=6}",
                        age(6), babyFood(6), null
                },

                // удаление флагов
                {
                        "{AGE=16} -> {}",
                        age(16), empty(), null
                },
                {
                        "{ALCOHOL, AGE=16} -> {ALCOHOL}",
                        alcohol().with(BannerFlags.AGE, Age.AGE_16), alcohol(), null
                },
                {
                        "{ALCOHOL} -> {}",
                        alcohol(), empty(), insufficientRights()
                },
        });
    }

    @Test
    public void immutableFlagsCannotBeChanged() {
        var result = BannerWithFlagsConstraints.immutableFlagsCannotBeChanged(oldFlags).apply(newFlags);
        assertThat(result, equalTo(expectedDefect));
    }
}
