package ru.yandex.market.mbi.partner.registration.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.csv.CSVReader;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.cache.WhsRegionsCache;
import ru.yandex.market.mbi.partner.registration.model.LogisticPoint;
import ru.yandex.market.mbi.partner.registration.placement.type.AvailablePlacementTypesService;
import ru.yandex.market.mbi.partner.registration.placement.type.AvailablePlacementTypesUpdateExecutor;
import ru.yandex.market.mbi.partner.registration.placement.type.yt.AvailablePlacementTypesYtDao;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тесты для {@link  ru.yandex.market.mbi.partner.registration.placement.type.yt.AvailablePlacementTypesYtDao}
 */
@Disabled("Проверка чтения с YT данных из таблицы available_region_by_model")
public class AvailablePlacementTypesUpdateIntegrationTest extends AbstractFunctionalTest {
    @Autowired
    private AvailablePlacementTypesYtDao availablePlacementTypesYtDao;

    private AvailablePlacementTypesUpdateExecutor availablePlacementTypesUpdateExecutor;

    @Autowired
    private AvailablePlacementTypesService availablePlacementTypesService;

    @Autowired
    private WhsRegionsCache whsRegionsCache;

    @BeforeEach
    void setUp() {
        availablePlacementTypesUpdateExecutor = new AvailablePlacementTypesUpdateExecutor(
                availablePlacementTypesService,
                availablePlacementTypesYtDao
        );
    }

    @DisplayName("Проверяем, что данные считываются и джоба отрабатывает")
    @Test
    void testImport() {
        var result = availablePlacementTypesYtDao.getAvailableRegionByModelFromYt();
        availablePlacementTypesUpdateExecutor.doJob(null);
    }

    @DisplayName("Проверяем, что кэши прогреваются и обновляются")
    @Test
    void testCache() {
        availablePlacementTypesYtDao.getRussionRegionsFromYt()
                .forEach(r -> {
                    assertEquals(whsRegionsCache.getRegion(r.getId()), r);
                });
        availablePlacementTypesYtDao.getExpressRegionsFromYt()
                .forEach(r -> {
                    assertEquals(whsRegionsCache.isExpress(r.getRegionId()), true);
                });
        availablePlacementTypesYtDao.getFbsWhsInfoFromYt()
                .forEach(r -> {
                    assertNotNull(whsRegionsCache.getFbsWhs(r.getId()));
                });
        availablePlacementTypesYtDao.getFbyWhsInfoFromYt()
                .forEach(r -> {
                    assertNotNull(whsRegionsCache.getFfWhs(r.getId()));
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    @DisplayName("Расчет доступных моделей размещения для каждого города")
    void velocityCalcAvailabelPartnersModels(Integer regionId) {
        //TODO: отрефакторить тест, он же ничего не проверяет https://st.yandex-team.ru/MBI-84666
        availablePlacementTypesService.calculateMarketLogisticPointsInfo(regionId);
    }

    private static Stream<Arguments> velocityCalcAvailabelPartnersModels() throws IOException {
        return loadLogisticPoint("ru/yandex/market/mbi/partner/registration/cache/russian_cities.csv").stream()
                .map(lp -> Arguments.of(
                        lp.getId().intValue()
                ));
    }

    private static List<LogisticPoint> loadLogisticPoint(String lp) throws IOException {
        CSVReader reader = new CSVReader(getSystemResourceAsStream(lp));
        reader.readHeaders();
        List<LogisticPoint> points = new ArrayList<>();
        while (reader.readRecord()) {
            points.add(new LogisticPoint(
                    Integer.valueOf(reader.getField(1)),
                    reader.getField(0),
                    Double.valueOf(reader.getField(2)),
                    Double.valueOf(reader.getField(3)),
                    reader.getField(4)
            ));
        }

        return points;
    }

}
