package ru.yandex.direct.core.entity.banner.type.flags;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotChangeBannerFlagsFromAgeToOtherTypes;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotChangeBannerFlagsFromBabyFoodToOtherTypes;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.clientCannotSetBannerFlags;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.age;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.babyFood;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.empty;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithFlagsConstraintsTextBannerFlagsTypeCannotBeChangedTest {
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
                {
                        "{BABY_FOOD=1} -> {BABY_FOOD=3}",
                        babyFood(1), babyFood(3), null
                },
                {
                        "{AGE=6} -> {AGE=6}",
                        age(6), age(12), null
                },
                {
                        "{AGE=6} -> {BABY_FOOD=6}",
                        age(6), babyFood(6), cannotChangeBannerFlagsFromAgeToOtherTypes()
                },
                {
                        "{BABY_FOOD=6} -> {AGE=6}",
                        babyFood(6), age(6), cannotChangeBannerFlagsFromBabyFoodToOtherTypes()
                },
                {
                        "{} -> {AGE=12}",
                        empty(), age(12), clientCannotSetBannerFlags()
                },
                {
                        "{} -> {AGE=3}",
                        empty(), babyFood(3), clientCannotSetBannerFlags()
                },
        });
    }

    @Test
    public void immutableFlagsCannotBeChanged() {
        var result = BannerWithFlagsConstraints.textBannerFlagsTypeCannotBeChanged(oldFlags).apply(newFlags);
        assertThat(result, equalTo(expectedDefect));
    }
}
