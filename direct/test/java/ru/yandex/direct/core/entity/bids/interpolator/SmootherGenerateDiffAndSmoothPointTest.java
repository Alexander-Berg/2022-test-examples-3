package ru.yandex.direct.core.entity.bids.interpolator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.bids.container.interpolator.CapKey;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.utils.math.Point;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(Parameterized.class)
public class SmootherGenerateDiffAndSmoothPointTest {
    @Parameterized.Parameter
    public Point point1;

    @Parameterized.Parameter(1)
    public Point point2;

    @Parameterized.Parameter(2)
    public Point capPoint;

    @Parameterized.Parameter(3)
    public Double minPremiumBid;

    @Parameterized.Parameter(4)
    public Point expectedPoint;

    @Parameterized.Parameter(5)
    public String name;

    private Smoother smoother;


    @Parameterized.Parameters(name = "For generation {5} used")
    public static Object[][] getParameters() {
        return new Object[][]{
                {Point.fromDoubles(1000, 1000),
                        Point.fromDoubles(1000, 1000),
                        Point.fromDoubles(1000, 1000),
                        0.1,
                        Point.fromDoubles(700.0, 0.0),
                        "maxPrice"
                },
                {Point.fromDoubles(0.1, 1.0),
                        Point.fromDoubles(1, 1),
                        Point.fromDoubles(-1, 1),
                        0.2,
                        Point.fromDoubles(0.1, 0.0),
                        "point1 abscissa"
                },
                {Point.fromDoubles(0.1, 1.0),
                        Point.fromDoubles(1, 1),
                        Point.fromDoubles(-1, 1),
                        0.05,
                        Point.fromDoubles(0.05, 0.0),
                        "minPrice"
                },
                {Point.fromDoubles(0.1, 1.0),
                        Point.fromDoubles(1, 1),
                        Point.fromDoubles(1, 1),
                        0.05,
                        Point.fromDoubles(0.2, 0.0),
                        "diff"
                },
        };
    }

    @Before
    public void before() {
        CapFactory capFactory = new CapFactory();
        smoother = new Smoother(capFactory.getCap(new CapKey(true, 1, "")), CurrencyCode.USD, minPremiumBid);
    }

    @Test
    public void generatePair() {
        Point point = smoother.generateDiffAndSmoothPoint(point1, point2, capPoint);
        assertThat(point, beanDiffer(expectedPoint).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

}
