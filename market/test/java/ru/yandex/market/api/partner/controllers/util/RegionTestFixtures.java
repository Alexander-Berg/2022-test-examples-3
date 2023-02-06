package ru.yandex.market.api.partner.controllers.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.geobase.model.RegionType;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 10.03.16
 * Time: 21:07
 */
public class RegionTestFixtures {
    public static final String PARIS = "Париж";
    public static final Path FRANCE = buildFrenchPath();
    public static final Path US = buildAmericanPath();
    public static final Path RUSSIA = buildRussianPath();

    @Nonnull
    private static Path buildFrenchPath() {
        List<Region> path = new ArrayList<>();
        path.add(new Region(10502L, PARIS, 104365L, RegionType.CITY));
        path.add(new Region(104365L, "Иль-де-Франс", 124L, RegionType.REPUBLIC));
        path.add(new Region(124L, "Франция", 111L, RegionType.COUNTRY));
        path.add(new Region(111L, "Европа", 10001L, RegionType.CONTINENT));
        path.add(new Region(10001L, "Евразия", 10000L, RegionType.CONTINENT));
        path.add(new Region(10000L, "Земля", null, RegionType.UNKNOWN));
        return new Path("France", path,
                //filter out continents
                Arrays.asList(10502L, 104365L, 124L
                        //,111L, 10001L, 10000L
                ),
                Arrays.asList(PARIS, "Иль-де-Франс", "Франция"
                        //, "Европа", "Евразия", "Земля"
                ));
    }

    @Nonnull
    private static Path buildAmericanPath() {
        List<Region> path = new ArrayList<>();
        path.add(new Region(110531L, PARIS, 29360L, RegionType.CITY));
        path.add(new Region(29360L, "Штат Техас", 84L, RegionType.REPUBLIC));
        path.add(new Region(84L, "США", 10002L, RegionType.COUNTRY));
        path.add(new Region(10002L, "Северная Америка", 10000L, RegionType.CONTINENT));
        path.add(new Region(10000L, "Земля", null, RegionType.UNKNOWN));
        return new Path("USA", path,
                //filter out continents
                Arrays.asList(110531L, 29360L, 84L
                        //, 10002L, 10000L
                ),
                Arrays.asList(PARIS, "Штат Техас", "США"
                        //, "Северная Америка", "Земля"
                ));
    }

    @Nonnull
    private static Path buildRussianPath() {
        List<Region> path = new ArrayList<>();
        path.add(new Region(143697L, PARIS, 100017L, RegionType.TOWN));
        path.add(new Region(100017L, "Нагайбакский район", 11225L, RegionType.REPUBLIC_AREA));
        path.add(new Region(11225L, "Челябинская область", 52L, RegionType.REPUBLIC));
        path.add(new Region(52L, "Уральский федеральный округ", 225L, RegionType.AREA));
        path.add(new Region(225L, "Россия", 10001L, RegionType.COUNTRY));
        path.add(new Region(10001L, "Евразия", 10000L, RegionType.CONTINENT));
        path.add(new Region(10000L, "Земля", null, RegionType.UNKNOWN));
        return new Path("Russia", path,
                //filter out continents
                Arrays.asList(143697L, 100017L, 11225L, 52L, 225L
                        //, 10001L, 10000L
                ),
                Arrays.asList(PARIS, "Нагайбакский район", "Челябинская область",
                        "Уральский федеральный округ", "Россия"
                        //, "Евразия", "Земля"
                ));
    }

    public static final class Path {
        final String location;
        final List<Region> regions;
        final List<Long> ids;
        final List<String> names;

        public Path(String location, List<Region> regions, List<Long> ids, List<String> names) {
            this.location = location;
            this.regions = regions;
            this.ids = ids;
            this.names = names;
        }
    }
}
