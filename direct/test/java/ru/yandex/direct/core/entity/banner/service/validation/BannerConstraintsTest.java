package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstraints.limitBannersInGroup;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;

@RunWith(Parameterized.class)
public class BannerConstraintsTest<T> {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"максимальное кол-во баннеров в группе",
                        limitBannersInGroup(OldTextBanner.AD_GROUP_ID, GROUP_BANNERS_COUNTER, null, false, false),
                        new OldTextBanner().withAdGroupId(1L), null},
                {"кол-во баннеров в группе превышено",
                        limitBannersInGroup(OldTextBanner.AD_GROUP_ID, GROUP_BANNERS_COUNTER, null, false, false),
                        new OldTextBanner().withAdGroupId(2L), maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP)},

        });
    }

    private static final Map<Long, Long> GROUP_BANNERS_COUNTER;

    static {
        GROUP_BANNERS_COUNTER = new HashMap<>();
        GROUP_BANNERS_COUNTER.put(1L, 50L);
        GROUP_BANNERS_COUNTER.put(2L, 51L);
    }

    private final Constraint<T, Defect> constraint;
    private final T arg;
    private final Defect expectedDefect;

    public BannerConstraintsTest(@SuppressWarnings("unused") String testName,
                                 Constraint<T, Defect> constraint, T arg, Defect defect) {
        this.constraint = constraint;
        this.arg = arg;
        this.expectedDefect = defect;
    }

    @Test
    public void testParametrized() {
        assertThat(this.constraint.apply(arg), is(expectedDefect));
    }
}
