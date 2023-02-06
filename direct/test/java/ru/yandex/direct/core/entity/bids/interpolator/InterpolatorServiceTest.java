package ru.yandex.direct.core.entity.bids.interpolator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.bids.container.interpolator.Cap;
import ru.yandex.direct.core.entity.bids.container.interpolator.CapKey;
import ru.yandex.direct.core.entity.currency.model.CurrencyRate;
import ru.yandex.direct.core.entity.currency.repository.CurrencyRateRepository;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.utils.math.Point;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
public class InterpolatorServiceTest {
    private static final CapKey CAP_KEY = new CapKey(true, 1, "");
    private final Cap defaultCap;
    private InterpolatorService interpolatorService;

    private CapFactory capFactory;

    private CurrencyRateRepository currencyRateRepository;

    public InterpolatorServiceTest() {
        currencyRateRepository = mock(CurrencyRateRepository.class);
        when(currencyRateRepository.getCurrencyRate(eq(CurrencyCode.USD), any()))
                .thenAnswer(invocation -> {
                    LocalDate date = (LocalDate) invocation.getArguments()[1];
                    return new CurrencyRate()
                            .withCurrencyCode(CurrencyCode.USD)
                            .withDate(date)
                            .withRate(BigDecimal.valueOf(60));
                });
        capFactory = new CapFactory();
        CurrencyRateService currencyRateService = new CurrencyRateService(currencyRateRepository);
        interpolatorService = new InterpolatorService(currencyRateService, capFactory);
        defaultCap = capFactory.getCap(CAP_KEY);
    }

    @Test
    public void calculateAbscissaByOrdinate() {
        List<Point> points = asList(Point.fromDoubles(0, 0),
                Point.fromDoubles(2, 2));
        List<Point> result = interpolatorService
                .calculateAbscissaByOrdinate(points, Collections.singletonList(1.0), CurrencyCode.USD);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualByComparingTo(Point.fromDoubles(1, 1));
    }

    @Test
    public void calculateAbscissaByOrdinate_ConstFunction() {
        List<Point> points = asList(Point.fromDoubles(0, 0),
                Point.fromDoubles(2, 0));
        List<Point> result = interpolatorService
                .calculateAbscissaByOrdinate(points, Collections.singletonList(0.0), CurrencyCode.USD);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualByComparingTo(Point.fromDoubles(0, 0));
        assertThat(result.get(1)).isEqualByComparingTo(Point.fromDoubles(2, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPointForOrdinateInterval_IllegalArgs() {
        Point p1 = Point.fromDoubles(0, 0);
        Point p2 = Point.fromDoubles(0, 0);
        interpolatorService.interpolateLinear(p1, p2, 0.0, CurrencyCode.USD);
    }

    @Test
    public void getPointsFromInterpolatedFunction_customOrdinate() {
        List<Point> points = asList(Point.fromDoubles(0L, 0L),
                Point.fromDoubles(2L, 2L));
        List<Point> result = interpolatorService
                .getInterpolatedPoints(defaultCap, CurrencyCode.USD, points, Collections.singletonList(0.0));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualByComparingTo(Point.fromDoubles(0, 0));
    }

    @Test
    public void getPointsFromInterpolatedFunction_defaultOrdinate() {
        List<Point> points = asList(Point.fromDoubles(0, 0),
                Point.fromDoubles(100, 100));
        List<Point> result = interpolatorService.getInterpolatedPoints(defaultCap, CurrencyCode.USD, points, null);

        assertThat(result).hasSize(101);
        assertThat(result.get(100)).isEqualByComparingTo(Point.fromDoubles(120, 100));
    }

    @Test
    public void getInterpolatedTrafaretBidItems() {
        CurrencyCode usd = CurrencyCode.USD;
        List<TrafaretBidItem> trafaretBidItems = Collections.singletonList(new TrafaretBidItem()
                .withBid(Money.valueOfMicros(10_000_000, usd))
                .withPositionCtrCorrection(60_000)
                .withPrice(Money.valueOfMicros(20_000_000, usd)));
        List<TrafaretBidItem> interpolatedTrafaretBidItems =
                interpolatorService.getInterpolatedTrafaretBidItems(CAP_KEY, trafaretBidItems, null, usd);
        assertThat(interpolatedTrafaretBidItems.get(0))
                .isEqualToComparingOnlyGivenFields(new TrafaretBidItem().withPositionCtrCorrection(50_000),
                        "positionCtrCorrection");
        assertThat(interpolatedTrafaretBidItems.get(interpolatedTrafaretBidItems.size() - 1))
                .isEqualToComparingOnlyGivenFields(new TrafaretBidItem().withPositionCtrCorrection(60_000),
                        "positionCtrCorrection");

    }

    @Test
    public void removePointsViolateNonDecreasingOrder() {
        List<Point> points = asList(Point.fromDoubles(0, 0),
                Point.fromDoubles(100, 100),
                Point.fromDoubles(200, 50),
                Point.fromDoubles(250, 60),
                Point.fromDoubles(300, 150));
        List<Point> result = interpolatorService.removePointsViolateNonDecreasingOrder(points);
        assertThat(result).hasSize(3);
        assertThat(result).doesNotContain(Point.fromDoubles(200, 50), Point.fromDoubles(250, 60));
    }

}
