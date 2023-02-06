package ru.yandex.direct.grid.processing.service.showcondition.converter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.processing.model.retargeting.GdRetargeting;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingAccess;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFeatures;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.service.showcondition.converter.RetargetingConverter.toGdRetargetingFeatures;
import static ru.yandex.direct.grid.processing.util.RetargetingTestDataUtils.defaultGdRetargeting;

@RunWith(Parameterized.class)
public class RetargetingConverterCanEditCountTest {

    @Parameterized.Parameter(0)
    public List<GdRetargeting> gdRetargetings;
    @Parameterized.Parameter(1)
    public int canEditCount;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> parameters() {
        GdRetargeting activeGdRetargeting = defaultGdRetargeting();
        GdRetargeting readOnlyGdRetargeting = defaultGdRetargeting()
                .withAccess(new GdRetargetingAccess().withCanEdit(false));

        return Arrays.asList(new Object[][]{
                {emptyList(), 0},
                {List.of(readOnlyGdRetargeting), 0},
                {List.of(activeGdRetargeting), 1},
                {List.of(activeGdRetargeting, readOnlyGdRetargeting), 1},
                {List.of(activeGdRetargeting, activeGdRetargeting), 2}
        });
    }

    @Test
    public void testCanEdit() {
        GdRetargetingFeatures actual = toGdRetargetingFeatures(gdRetargetings);

        GdRetargetingFeatures expected = new GdRetargetingFeatures()
                .withCanEditCount(canEditCount);

        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }
}
