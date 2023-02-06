package ru.yandex.market.billing.geobase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.core.geobase.model.Timezone;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.tanker.client.TankerClient;
import ru.yandex.market.tanker.client.request.AddTranslationRequestBuilder;
import ru.yandex.market.tanker.client.request.AdditionMode;

/**
 * Тесты для {@link TimezoneTankerExportService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TimezoneTankerExportServiceTest extends FunctionalTest {

    private static final String ASIA_ADEN = "Asia/Aden";
    private static final String AMERICA_CUIABA = "America/Cuiaba";
    private static final String ASIA_VLADIVOSTOK = "Asia/Vladivostok";
    private static final String AFRICA_EL_AAIUN = "Africa/El_Aaiun";
    private static final String ASIA_BAKU = "Asia/Baku";

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Генерация имени")
    void testNameGeneration() {
        final Map<String, Queue<Region>> tzMap = getTzMap();
        final TimezoneTankerExportService service = getService();
        final int citiesLimit = 5;

        final String name1 = service.generateName(tzMap.get(ASIA_ADEN), citiesLimit);
        Assertions.assertEquals("reg1.2, reg1.1", name1);

        final String name2 = service.generateName(tzMap.get(ASIA_VLADIVOSTOK), citiesLimit);
        Assertions.assertEquals("reg3.3, reg3.2", name2);

        final String name3 = service.generateName(tzMap.get(AFRICA_EL_AAIUN), citiesLimit);
        Assertions.assertEquals("reg4.1", name3);

        final String name4 = service.generateName(tzMap.get(ASIA_BAKU), citiesLimit);
        Assertions.assertEquals("reg5.6, reg5.5, reg5.4, reg5.3, reg5.2", name4);
    }

    @Test
    @DisplayName("Генерация запроса")
    void testRequest() {
        final Map<String, Queue<Region>> tzMap = getTzMap();
        final AddTranslationRequestBuilder request = getService().buildRequest(tzMap, 5);

        Assertions.assertEquals(AdditionMode.REPLACE, request.getMode());
        Assertions.assertEquals("market-partner", request.getProjectId());
    }

    @Test
    @DisplayName("Подготовка списка городов")
    void testPrepare() {
        final Map<String, Queue<Region>> result = getTzMap();

        Assertions.assertEquals(4, result.size());

        checkRegionsQueue(Arrays.asList(2, 1), result.get(ASIA_ADEN));
        checkRegionsQueue(Arrays.asList(3, 5, 4), result.get(ASIA_VLADIVOSTOK));
        checkRegionsQueue(Arrays.asList(6), result.get(AFRICA_EL_AAIUN));
        checkRegionsQueue(Arrays.asList(12, 11, 10, 9, 8, 7), result.get(ASIA_BAKU));
    }

    private Map<String, Queue<Region>> getTzMap() {
        final List<Timezone> timezones = Arrays.asList(
                // Таймзона без городов
                new Timezone(ASIA_ADEN, 10800),

                // Пустая таймзона
                new Timezone(AMERICA_CUIABA, -14400),

                // Таймзона с несколькими населенными пунктами
                new Timezone(ASIA_VLADIVOSTOK, 36000),

                // Таймзона с 1 пунктом
                new Timezone(AFRICA_EL_AAIUN, 0),

                // Таймзона с 6 пунктами
                new Timezone(ASIA_BAKU, 14400)
        );

        final List<Region> regions = Arrays.asList(
                createRegion(1, "reg1.1", RegionType.COUNTRY, ASIA_ADEN, 123),
                createRegion(2, "reg1.2", RegionType.CONTINENT, ASIA_ADEN, 1234),

                createRegion(3, "reg3.1", RegionType.CONTINENT, ASIA_VLADIVOSTOK, 30),
                createRegion(4, "reg3.2", RegionType.CITY, ASIA_VLADIVOSTOK, 3),
                createRegion(5, "reg3.3", RegionType.CITY, ASIA_VLADIVOSTOK, 12),

                createRegion(6, "reg4.1", RegionType.CITY, AFRICA_EL_AAIUN, 12),

                createRegion(7, "reg5.1", RegionType.CITY, ASIA_BAKU, 1),
                createRegion(8, "reg5.2", RegionType.CITY, ASIA_BAKU, 2),
                createRegion(9, "reg5.3", RegionType.CITY, ASIA_BAKU, 3),
                createRegion(10, "reg5.4", RegionType.CITY, ASIA_BAKU, 4),
                createRegion(11, "reg5.5", RegionType.CITY, ASIA_BAKU, 5),
                createRegion(12, "reg5.6", RegionType.CITY, ASIA_BAKU, 6)
        );

        final TimezoneTankerExportService service = getService();
        return service.prepare(timezones, regions);
    }

    private TimezoneTankerExportService getService() {
        return new TimezoneTankerExportService(Mockito.mock(TankerClient.class), environmentService);
    }

    private Region createRegion(final int id,
                                final String name,
                                final RegionType type,
                                final String timezone,
                                final int population) {
        final Region region = new Region(id, name, type, null);
        region.setCustomAttributeValue(CustomRegionAttribute.TIMEZONE, timezone);
        region.setCustomAttributeValue(CustomRegionAttribute.POPULATION, String.valueOf(population));
        return region;
    }

    private void checkRegionsQueue(final List<Integer> ids, final Queue<Region> regions) {
        final Iterator<Integer> it = ids.iterator();
        while (!regions.isEmpty()) {
            final Region region = Objects.requireNonNull(regions.poll());
            Assertions.assertEquals((int) it.next(), region.getId());
        }

        Assertions.assertFalse(it.hasNext());
    }
}
