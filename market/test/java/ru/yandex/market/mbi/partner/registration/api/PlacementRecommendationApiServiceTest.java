package ru.yandex.market.mbi.partner.registration.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.csv.CSVReader;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.model.ExpressRegionInfo;
import ru.yandex.market.mbi.partner.registration.model.LogisticPoint;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.AvailablePartnerPlacementType;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.Coordinates;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.NearestLogisticPointInfo;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.PartnerPlacementTypeInfo;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.PartnerPlacementTypeInfoMarketLogisticPointInfo;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.PartnerPlacementTypeRecommendation;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.PartnerPlacementTypeRecommendationRequest;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.Mockito.doReturn;

public class PlacementRecommendationApiServiceTest extends AbstractFunctionalTest {

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

    @ParameterizedTest(name = "{0}")
    @MethodSource
    @DisplayName("Проверяем рассчет приоритетов моделей размещения")
    void getPlacementModels(String name, long regionId, List<PartnerPlacementTypeInfo> expected) {
        var response = placementRecommendationApiClient
            .getPartnerPlacementTypes(new PartnerPlacementTypeRecommendationRequest()
                .regionId(regionId)
            )
            .schedule()
            .join();
        org.assertj.core.api.Assertions.assertThat(response.getResult())
            .extracting(PartnerPlacementTypeRecommendation::getTypes)
            .isEqualTo(expected);
    }

    private static Stream<Arguments> getPlacementModels() {
        return Stream.of(
            Arguments.of(
                "Доступны все модели",
                213L,
                List.of(
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.FBY)
                        .available(true)
                        .marketLogisticPointInfo(
                            new PartnerPlacementTypeInfoMarketLogisticPointInfo()
                                .numberOfLogisticPointsInLocalRegion(1)
                        ),
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.FBS)
                        .available(true)
                        .marketLogisticPointInfo(
                            new PartnerPlacementTypeInfoMarketLogisticPointInfo()
                                .numberOfLogisticPointsInLocalRegion(2)
                        ),
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.EXPRESS)
                        .available(true),
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.DBS)
                        .available(true)
                )
            ),
            Arguments.of(
                "FBS и FBY на отдалении",
                10754L,
                List.of(
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.FBY)
                        .available(true)
                        .marketLogisticPointInfo(
                            new PartnerPlacementTypeInfoMarketLogisticPointInfo()
                                .numberOfLogisticPointsInLocalRegion(0)
                                .nearestLogisticPoint(new NearestLogisticPointInfo()
                                    .distance(80)
                                    .address("140126, Московская область, Раменский район, " +
                                        "Софьинское сельское поселение, село Софьино, " +
                                        "территория Логистический технопарк Софьино, к1")
                                    .city("Софьино")
                                    .coordinates(new Coordinates().latitude(55.49695).longitude(38.158359)
                                    )
                                )),
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.FBS)
                        .available(true)
                        .marketLogisticPointInfo(
                            new PartnerPlacementTypeInfoMarketLogisticPointInfo()
                                .numberOfLogisticPointsInLocalRegion(0)
                                .nearestLogisticPoint(new NearestLogisticPointInfo()
                                    .distance(57)
                                    .address("142119, Московская область, Подольск, Ленинградская улица, д. 4")
                                    .city("Подольск")
                                    .coordinates(new Coordinates().latitude(55.428465).longitude(37.503639))
                                )
                        ),
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.DBS)
                        .available(true),
                    new PartnerPlacementTypeInfo()
                        .placementType(AvailablePartnerPlacementType.EXPRESS)
                        .available(false)
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
