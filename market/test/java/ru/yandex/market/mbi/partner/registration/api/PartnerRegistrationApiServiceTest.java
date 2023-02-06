package ru.yandex.market.mbi.partner.registration.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.csv.CSVReader;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.model.ExpressRegionInfo;
import ru.yandex.market.mbi.partner.registration.model.LogisticPoint;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.AvailablePartnerPlacementType;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.AvailablePartnerPlacementTypeInfo;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.Coordinates;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.Mockito.doReturn;

/**
 * Тест на {@link PartnerRegistrationApiService}
 */
public class PartnerRegistrationApiServiceTest extends AbstractFunctionalTest {

    @BeforeEach
    public void mockYt() throws IOException {
        doReturn(loadExpressRegions())
                .when(availablePlacementTypesYtDao).getExpressRegionsFromYt();
        doReturn(loadLogisticPoint("ru/yandex/market/mbi/partner/registration/cache/russian_cities.csv"))
                .when(availablePlacementTypesYtDao).getRussionRegionsFromYt();
        doReturn(loadLogisticPoint("ru/yandex/market/mbi/partner/registration/cache/fbs_whs.csv"))
                .when(availablePlacementTypesYtDao).getFbsWhsInfoFromYt();
        doReturn(loadLogisticPoint("ru/yandex/market/mbi/partner/registration/cache/ff_whs.csv"))
                .when(availablePlacementTypesYtDao).getFbyWhsInfoFromYt();
    }

    @Test
    void testNoModels() {
        var response = partnerRegistrationApiClient.getAvailablePlacementTypes(213)
                .schedule()
                .join();
        Assertions.assertEquals(List.of(AvailablePartnerPlacementType.DBS), response.getResult().getTypes());
    }

    @Test
    @DbUnitDataSet(before = "PartnerRegistrationApiServiceTest.region.before.csv")
    void testRegion() {
        var response = partnerRegistrationApiClient.getAvailablePlacementTypes(213)
                .schedule()
                .join();
        MatcherAssert.assertThat(
                response.getResult().getTypes(),
                Matchers.containsInAnyOrder(AvailablePartnerPlacementType.DBS, AvailablePartnerPlacementType.FBS)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    @DisplayName("Проверяем расчет доступных моделей размещения")
    void availableModelByRegionTest(String name, Integer regionId, List<AvailablePartnerPlacementTypeInfo> models) {
        var response = partnerRegistrationApiClient
                .getAvailablePlacementTypesInfo(regionId)
                .schedule()
                .join();
        response.getResult().getTypes().stream().allMatch(item -> models.contains(item));
    }

    private static Stream<Arguments> availableModelByRegionTest() {
        return Stream.of(
                Arguments.of(
                        "Доступны все модели",
                        Integer.valueOf(213),
                        List.of(
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.DBS)
                                        .availability(true),
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.EXPRESS)
                                        .availability(true),
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.FBS)
                                        .availability(true)
                                        .countWhs(2),
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.FBY)
                                        .availability(true)
                                        .countWhs(1)
                        )
                ),
                Arguments.of(
                        "Не доступна FBS и FBY модель",
                        Integer.valueOf(10754),
                        List.of(
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.DBS)
                                        .availability(true),
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.EXPRESS)
                                        .availability(true),
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.FBS)
                                        .availability(false)
                                        .countWhs(0)
                                        .distance(57)
                                        .address("142119, Московская область, Подольск, Ленинградская улица, д. 4")
                                        .city("Подольск")
                                        .coordinates(new Coordinates().latitude(55.428465).longitude(37.503639)),
                                new AvailablePartnerPlacementTypeInfo()
                                        .model(AvailablePartnerPlacementType.FBY)
                                        .availability(false)
                                        .countWhs(0)
                                        .distance(80)
                                        .address("140126, Московская область, Раменский район, " +
                                                "Софьинское сельское поселение, село Софьино, " +
                                                "территория Логистический технопарк Софьино, к1")
                                        .city("Софьино")
                                        .coordinates(new Coordinates().latitude(55.49695).longitude(38.158359))
                                )
                )
        );
    }

    private List<LogisticPoint> loadLogisticPoint(String lp) throws IOException {
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

    private List<ExpressRegionInfo> loadExpressRegions() throws IOException {
        CSVReader reader = new CSVReader(getSystemResourceAsStream(
                "ru/yandex/market/mbi/partner/registration/cache/express_regions.csv")
        );
        reader.readHeaders();
        List<ExpressRegionInfo> points = new ArrayList<>();
        while (reader.readRecord()) {
            points.add(new ExpressRegionInfo(
                    Integer.valueOf(reader.getField(1)),
                    reader.getField(0)
            ));
        }

        return points;
    }
}
