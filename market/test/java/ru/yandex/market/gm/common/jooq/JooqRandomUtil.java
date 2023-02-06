package ru.yandex.market.gm.common.jooq;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

import ru.yandex.market.global.common.jooq.Point;

import static io.github.benas.randombeans.FieldPredicates.named;
import static ru.yandex.market.global.common.test.RandomUtil.fixedPrecisionBigDecimalRandomizer;

/**
 * @author moskovkin@yandex-team.ru
 * @since 22.01.2022
 */
public class JooqRandomUtil {
    private static final int PG_POLYGON_PRECISION = 15;

    private JooqRandomUtil() {
    }

    public static Randomizer<List<Point>> geoPolygonPointsRandomizer(long seed) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();
            private final Randomizer<Point> pointRadomizer = geoPointRandomizer(seed);

            @Override
            public List<Point> getRandomValue() {
                AtomicDouble dx = new AtomicDouble(1);
                AtomicDouble dy = new AtomicDouble(1);
                Point point = pointRadomizer.getRandomValue();
                return IntStream.range(1, 4 + random.nextInt(2))
                    .mapToObj(i -> new Point()
                        .setLat(point.getLat()
                                .multiply(BigDecimal.valueOf(i))
                                .add(BigDecimal.valueOf(dx.getAndSet(dx.doubleValue() / 2)))
                                .setScale(15, RoundingMode.HALF_UP)
                                .stripTrailingZeros()
                        )
                        .setLon(point.getLon()
                                .multiply(BigDecimal.valueOf(i))
                                .add(BigDecimal.valueOf(dy.getAndSet(dy.doubleValue() / 4)))
                                .setScale(15, RoundingMode.HALF_UP)
                                .stripTrailingZeros()
                        )
                    ).collect(Collectors.toList());
            }
        };
    }

    public static Randomizer<Point> geoPointRandomizer(long seed) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .randomize(named("lat").or(named("lon")), fixedPrecisionBigDecimalRandomizer(seed, PG_POLYGON_PRECISION))
                    .seed(seed)
                    .build();

            @Override
            public Point getRandomValue() {
                return random.nextObject(Point.class);
            }
        };
    }
}
