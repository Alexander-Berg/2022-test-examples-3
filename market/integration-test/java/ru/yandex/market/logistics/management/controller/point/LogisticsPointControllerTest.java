package ru.yandex.market.logistics.management.controller.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.EntityManagerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.ServiceCode;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointActivateFreezeRequest;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointEnrichAddressRequest;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter.LogisticsPointFilterBuilder;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.service.calendar.CalendarService;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestRegions.SARKANDAUGAVA_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;
import static ru.yandex.market.logistics.management.util.TestUtil.hasResolvedExceptionContainingMessage;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.point;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@ParametersAreNonnullByDefault
@DisplayName("Логистические точки")
@DatabaseSetup("/data/controller/point/before/prepare_data.xml")
class LogisticsPointControllerTest extends AbstractContextualTest {

    private static final String URI = "/externalApi/logisticsPoints";
    private static final String RETURN_POINT_URI = URI + "/returnPointForPartner";

    private static final String POINT_2 = point(51.379909, 42.125175);
    private static final String POINT_1 = point(56.948048, 24.107018);
    private static final GeoObject GEO_OBJECT = geoObject(
        toponymInfo("11475", Kind.LOCALITY, POINT_1),
        "Рига, Уриекстес, дом 14а"
    );

    @Autowired
    private GeoClient geoClient;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private RegionService regionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final ArrayList<Runnable> verifications = new ArrayList<>();

    @BeforeEach
    void setup() {
        when(regionService.get()).thenReturn(buildRegionTree());
    }

    @AfterEach
    void verifyInteractions() {
        verifications.forEach(Runnable::run);
        verifications.clear();
        verifyNoMoreInteractions(geoClient);
        verifyNoMoreInteractions(httpGeobase);
    }

    @Test
    @DisplayName("Успешное создание точки с дочерними таблицами")
    void createLogisticsPointSuccessful() throws Exception {
        createLogisticsPointSuccessful(
            "data/controller/point/create_point_successful.json",
            "data/controller/point/created_point.json"
        );
    }

    @Test
    @DisplayName("Успешное создание точки с обогащением координат")
    void createLogisticsPointEnrichCoordinates() throws Exception {
        when(geoClient.find(eq("Рижский регион, Рижский округ, Рига, Уриекстес, дом 14а"), any(GeoSearchParams.class)))
            .thenReturn(List.of(GEO_OBJECT));

        verifications.add(() -> verify(geoClient)
            .find(eq("Рижский регион, Рижский округ, Рига, Уриекстес, дом 14а"), any(GeoSearchParams.class)));
        createLogisticsPointSuccessful(
            "data/controller/point/create_point_enrich_coordinates.json",
            "data/controller/point/created_point_enrich_coordinates.json"
        );
    }

    //    @Test // * DELIVERY-29742. Убрали автоматичекое обогащение subRegion, чтобы лог. точка не пересоздавалась
    @DisplayName("Успешное создание точки с обогащением поля subRegion")
    void createLogisticsPointEnrichSubRegionSuccessful() throws Exception {
        createLogisticsPointSuccessful(
            "data/controller/point/create_point_enrich_subregion.json",
            "data/controller/point/created_point_enrich_subregion.json"
        );
    }

    @Test
    @DisplayName("Успешное создание точки с указанием partnerId")
    void createLogisticsPointWithPartnerId() throws Exception {
        createLogisticsPointSuccessful(
            "data/controller/point/create_point_partner_id.json",
            "data/controller/point/created_point_partner_id.json"
        );
    }

    @Test
    @DisplayName("Создание активной точки с уже существующими partnerId, externalId, type")
    @ExpectedDatabase(
        value = "/data/controller/point/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createExistingLogisticsPoint() throws Exception {
        mockMvc.perform(
            post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/point/create_existing_point.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/externalApi/logisticsPoints/1"))
            .andExpect(testJson("data/controller/point/return_existing_point.json"));
    }

    @Test
    @DisplayName("Создание точки с адресом без указания addressString и shortAddressString")
    void createLogisticsPointNoAddressStrings() throws Exception {
        mockMvc.perform(
            post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/point/create_point_no_address_strings.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(testJson("data/controller/point/created_point_no_address_strings.json"));
    }

    @Test
    @DisplayName("Заполнение идентификатора локации точки при создании")
    @ExpectedDatabase(
        value = "/data/controller/point/after/create_logistics_point_and_set_location_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticsPointSuccessfulWithFindingAbsentLocationId() throws Exception {
        when(geoClient.find(
            eq("Рижский регион, Рижский округ, Рига, Уриекстес, дом 14а"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(geoObject(toponymInfo("11475", Kind.LOCALITY, null), "Рига, Уриекстес, дом 14а")));

        when(geoClient.find(
            eq("Рижский регион, Рижский округ, Рига"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(geoObject(toponymInfo("11475", Kind.LOCALITY, null), "Рига, Уриекстес")));

        mockHttpGeobaseRegionRequest();

        createLogisticsPointSuccessful(
            "data/controller/point/create_point_without_location_id_successful.json",
            "data/controller/point/created_without_location_id_point.json"
        );

        verify(geoClient).find(
            eq("Рижский регион, Рижский округ, Рига, Уриекстес, дом 14а"),
            any(GeoSearchParams.class)
        );
        verify(geoClient).find(
            eq("Рижский регион, Рижский округ, Рига"),
            any(GeoSearchParams.class)
        );
    }

    @Test
    @DisplayName("Создание точки без поля address")
    void createLogisticsPointWithoutAddress() throws Exception {
        mockMvc.perform(
            post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/point/create_point_without_address.json"))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание точки, неизвестный вид услуги")
    void createLogisticsPointWithNonexistentService() throws Exception {
        mockMvc.perform(
            post(URI + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/point/create_point_with_nonexistent_service.json"))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание точки, которая не работает в государственные праздники")
    @DatabaseSetup("/data/controller/point/before/location_calendar.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/closed_on_holidays.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createClosedOnRegionHolidaysLogisticsPoint() throws Exception {
        createLogisticsPointSuccessful(
            "data/controller/point/create_point_closed_on_holidays.json",
            "data/controller/point/created_point_with_calendar.json"
        );
    }

    @Test
    @DisplayName("Успешное получение точки по идентификатору")
    void getByIdSuccessful() throws Exception {
        mockMvc.perform(get(URI + "/1"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/non_changed_point_1.json"));
    }

    @Test
    @DisplayName("Получение точки по неизвестному идентификатору")
    void getByNonexistentId() throws Exception {
        mockMvc.perform(get(URI + "/123"))
            .andExpect(status().isOk())
            .andExpect(content().string("null"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("Валидация фильтра поиска")
    @MethodSource("filterValidationParams")
    void searchFilterValidation(
        @SuppressWarnings("unused") String displayName,
        UnaryOperator<LogisticsPointFilterBuilder> adjuster
    ) throws Exception {
        search(adjuster.apply(LogisticsPointFilter.newBuilder()).build())
            .andExpect(status().isBadRequest());
    }

    @Nonnull
    private static Stream<Arguments> filterValidationParams() {
        return Stream.<Pair<String, UnaryOperator<LogisticsPointFilterBuilder>>>of(
            Pair.of("partnerIds", f -> f.partnerIds(Collections.singleton(null))),
            Pair.of("businessIds", f -> f.businessIds(Collections.singleton(null))),
            Pair.of("externalIds", f -> f.externalIds(Collections.singleton(null))),
            Pair.of("ids", f -> f.ids(Collections.singleton(null))),
            Pair.of("orderLength", f -> f.orderLength(0)),
            Pair.of("orderWidth", f -> f.orderWidth(0)),
            Pair.of("orderHeight", f -> f.orderHeight(0)),
            Pair.of("orderWeight", f -> f.orderWeight(0.0))
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Test
    @DisplayName("Успешная деактивация точки")
    @ExpectedDatabase(
        value = "/data/controller/point/after/deactivate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deactivate() throws Exception {
        mockMvc.perform(put(URI + "/deactivate/1"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Деактивация несуществующей точки")
    void deactivateNonexistentPoint() throws Exception {
        mockMvc.perform(put(URI + "/deactivate/123"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Успешное обогащение точки координатами")
    @DatabaseSetup(value = "/data/controller/point/before/logistics_point_without_coordinates.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/enrichAddress.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPointAddressSuccess() throws Exception {
        when(geoClient.find(
            eq("region3, subRegion3, Борисоглебск, Матросовская, дом 162"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(
                geoObject(toponymInfo("10675", null, point(51.379909, 42.125175)), "Борисоглебск, Матросовская, д. 162")
            ));

        mockHttpGeobaseRegionRequest();

        mockMvc.perform(put(URI + "/enrich-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogisticsPointEnrichAddressRequest(List.of(8L))))
        )
            .andExpect(status().isOk());

        verify(geoClient).find(
            eq("region3, subRegion3, Борисоглебск, Матросовская, дом 162"),
            any(GeoSearchParams.class)
        );
        checkBuildWarehouseSegmentTask(8L);
    }

    @Test
    @DisplayName("Успешное обогащение ПВЗ координатами")
    @DatabaseSetup(value = "/data/controller/point/before/logistics_pickup_point_without_coordinates.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/enrichPickupPointAddress.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPickupPointAddressSuccess() throws Exception {
        when(geoClient.find(
            eq("region3, subRegion3, Борисоглебск, Матросовская, дом 162"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(
                geoObject(toponymInfo("10675", null, POINT_2), "Борисоглебск, Матросовская, д. 162")
            ));

        when(geoClient.find(
            eq("region3, subRegion3, Борисоглебск"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(
                geoObject(toponymInfo("10675", Kind.PROVINCE, POINT_2), "Борисоглебск")
            ));

        mockHttpGeobaseRegionRequest();

        mockMvc.perform(put(URI + "/enrich-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogisticsPointEnrichAddressRequest(List.of(8L))))
        )
            .andExpect(status().isOk());

        verify(geoClient).find(
            eq("region3, subRegion3, Борисоглебск, Матросовская, дом 162"),
            any(GeoSearchParams.class)
        );

        verify(geoClient).find(
            eq("region3, subRegion3, Борисоглебск"),
            any(GeoSearchParams.class)
        );
    }

    @Test
    @DisplayName("Успешное обогащение точки координатами - координаты уже есть")
    @ExpectedDatabase(
        value = "/data/controller/point/after/addresses_without_changes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPointAddressNoChangesSuccess() throws Exception {
        mockMvc.perform(put(URI + "/enrich-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogisticsPointEnrichAddressRequest(List.of(3L))))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ошибка обогащения точек - нет списка идентификаторов")
    @ExpectedDatabase(
        value = "/data/controller/point/after/addresses_without_changes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPointAddressNoPointsFailure() throws Exception {
        mockMvc.perform(put(URI + "/enrich-address"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ошибка обогащения точек - пустой список")
    @ExpectedDatabase(
        value = "/data/controller/point/after/addresses_without_changes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPointAddressEmptyPointsFailure() throws Exception {
        mockMvc.perform(put(URI + "/enrich-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of()))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ошибка обогащения точек - слишком много точек")
    @ExpectedDatabase(
        value = "/data/controller/point/after/addresses_without_changes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPointAddressPointsIdsSizeFailure() throws Exception {
        mockMvc.perform(put(URI + "/enrich-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogisticsPointEnrichAddressRequest(
                Stream.generate(() -> 1L).limit(101).collect(Collectors.toList()))
            ))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ошибка обогащения точек - идентификаторы содержат null")
    @ExpectedDatabase(
        value = "/data/controller/point/after/addresses_without_changes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPointAddressNullIdSizeFailure() throws Exception {
        mockMvc.perform(put(URI + "/enrich-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogisticsPointEnrichAddressRequest(
                Collections.singleton(null)
            )))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Успешное обогащение координат, locationId не заполняется")
    @DatabaseSetup(value = "/data/controller/point/before/logistics_point_without_coordinates.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/enrichAddressWithoutLocationId.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void enrichPointAddressNullLocationIdSuccess() throws Exception {
        when(geoClient.find(
            eq("region3, subRegion3, Борисоглебск, Матросовская, дом 162"),
            any(GeoSearchParams.class)
        ))
            .thenReturn(List.of(
                geoObject(toponymInfo(null, null, point(51.379909, 42.125175)), "Борисоглебск, Матросовская, д. 162")
            ));
        mockHttpGeobaseRegionRequest();
        mockMvc.perform(put(URI + "/enrich-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogisticsPointEnrichAddressRequest(List.of(8L))))
        )
            .andExpect(status().isOk());

        verify(geoClient).find(
            eq("region3, subRegion3, Борисоглебск, Матросовская, дом 162"),
            any(GeoSearchParams.class)
        );
        checkBuildWarehouseSegmentTask(8L);
    }

    @Test
    @DisplayName("Поиск с указанием размера страницы и подсчетом статистики запросов")
    void searchByPageSize() throws Exception {
        StatisticsImplementor statistics = entityManagerFactory.unwrap(SessionFactoryImpl.class).getStatistics();
        statistics.clear();
        statistics.setStatisticsEnabled(true);

        search(LogisticsPointFilter.newBuilder().build(), PageRequest.of(0, 50))
            .andExpect(IntegrationTestUtils.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("data/controller/point/page_size.json", false));

        statistics.setStatisticsEnabled(false);
        softly.assertThat(statistics.getPrepareStatementCount()).isEqualTo(6);
        EntityStatistics serviceCodeStats = statistics.getEntityStatistics(ServiceCode.class.getName());
        softly.assertThat(serviceCodeStats.getLoadCount()).isEqualTo(2);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Параметры постраничного поиска")
    void withPaging(
        @SuppressWarnings("unused") String displayName,
        Pageable pageable,
        String resultPath
    ) throws Exception {
        search(LogisticsPointFilter.newBuilder().build(), pageable)
            .andExpect(IntegrationTestUtils.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent(resultPath, false));
    }

    @Nonnull
    private static Stream<Arguments> withPaging() {
        return Stream.of(
            Arguments.of("Размер страницы", PageRequest.of(0, 50), "data/controller/point/page_size.json"),
            Arguments.of("Последняя страница", PageRequest.of(3, 2), "data/controller/point/last_page.json"),
            Arguments.of(
                "Маленький размер страницы",
                PageRequest.of(0, 2),
                "data/controller/point/small_page_size.json"
            ),
            Arguments.of(
                "Слишком большая страница",
                PageRequest.of(5, 10),
                "data/controller/point/empty_page_size_10.json"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource({
        "commonSearchSource",
        "emptyResultSearchSource",
        "locationSearchSource",
        "orderDimensionsSearchSource",
        "fieldExisting",
        "partnerType",
        "partnerSubtype",
        "retailPartner"
    })
    @DatabaseSetup(
        value = "/data/controller/point/before/prepare_retail_partner_data.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Поиск логистических точек")
    void searchPoints(
        @SuppressWarnings("unused") String displayName,
        LogisticsPointFilter filter,
        String response
    ) throws Exception {
        search(filter)
            .andExpect(status().isOk())
            .andExpect(testJson(response));
    }

    @Nonnull
    private static Stream<Arguments> commonSearchSource() {
        return Stream.of(
            Arguments.of(
                "по списку идентификаторов точек",
                LogisticsPointFilter.newBuilder()
                    .ids(Set.of(2L, 3L, 4L))
                    .build(),
                "data/controller/point/search_result_2_3_4.json"
            ),
            Arguments.of(
                "по типу ПВЗ",
                LogisticsPointFilter.newBuilder()
                    .pickupPointType(PickupPointType.POST_OFFICE)
                    .build(),
                "data/controller/point/search_result_4.json"
            ),
            Arguments.of(
                "по partnerIds",
                LogisticsPointFilter.newBuilder()
                    .partnerIds(Set.of(2L, 3L))
                    .build(),
                "data/controller/point/search_result_2_3_4.json"
            ),
            Arguments.of(
                "по platformClientId",
                LogisticsPointFilter.newBuilder()
                    .platformClientId(3L)
                    .build(),
                "data/controller/point/search_result_3_4.json"
            ),
            Arguments.of(
                "active = true",
                LogisticsPointFilter.newBuilder()
                    .active(true)
                    .build(),
                "data/controller/point/search_result_1_2_4_5_6_7.json"
            ),
            Arguments.of(
                "active = false",
                LogisticsPointFilter.newBuilder()
                    .active(false)
                    .build(),
                "data/controller/point/search_result_3.json"
            ),
            Arguments.of(
                "по externalId",
                LogisticsPointFilter.newBuilder()
                    .externalIds(Set.of("R2"))
                    .build(),
                "data/controller/point/search_result_3_6.json"
            ),
            Arguments.of(
                "по типу точки",
                LogisticsPointFilter.newBuilder()
                    .type(PointType.PICKUP_POINT)
                    .build(),
                "data/controller/point/search_result_1_2_4.json"
            ),
            Arguments.of(
                "по availableForOnDemand = true",
                LogisticsPointFilter.newBuilder()
                    .availableForOnDemand(true)
                    .build(),
                "data/controller/point/search_result_3.json"
            ),
            Arguments.of(
                "по availableForOnDemand = false",
                LogisticsPointFilter.newBuilder()
                    .availableForOnDemand(false)
                    .build(),
                "data/controller/point/search_result_1_2_4_5_6_7.json"
            ),
            Arguments.of(
                "по deferredCourierAvailable = true",
                LogisticsPointFilter.newBuilder()
                    .deferredCourierAvailable(true)
                    .build(),
                "data/controller/point/search_result_3.json"
            ),
            Arguments.of(
                "по deferredCourierAvailable = false",
                LogisticsPointFilter.newBuilder()
                    .deferredCourierAvailable(false)
                    .build(),
                "data/controller/point/search_result_1_2_4_5_6_7.json"
            ),
            Arguments.of(
                "по darkStore = true",
                LogisticsPointFilter.newBuilder()
                    .darkStore(true)
                    .build(),
                "data/controller/point/search_result_4.json"
            ),
            Arguments.of(
                "по darkStore = false",
                LogisticsPointFilter.newBuilder()
                    .darkStore(false)
                    .build(),
                "data/controller/point/search_result_1_2_3_5_6_7.json"
            ),
            Arguments.of(
                "по businessId",
                LogisticsPointFilter.newBuilder()
                    .businessIds(Set.of(110L, 120L))
                    .build(),
                "data/controller/point/search_result_5_6_7.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> emptyResultSearchSource() {
        return Stream.of(
            Arguments.of(
                "по пустому списку partnerIds",
                LogisticsPointFilter.newBuilder()
                    .partnerIds(Set.of())
                    .build(),
                "data/controller/point/search_result_empty.json"
            ),
            Arguments.of(
                "по пустому списку externalIds",
                LogisticsPointFilter.newBuilder()
                    .externalIds(Set.of())
                    .build(),
                "data/controller/point/search_result_empty.json"
            ),
            Arguments.of(
                "по пустому списку ids",
                LogisticsPointFilter.newBuilder()
                    .ids(Set.of())
                    .build(),
                "data/controller/point/search_result_empty.json"
            ),
            Arguments.of(
                "по несуществующему partnerId",
                LogisticsPointFilter.newBuilder()
                    .partnerIds(Set.of(5L))
                    .build(),
                "data/controller/point/search_result_empty.json"
            ),
            Arguments.of(
                "по несуществующим externalIds",
                LogisticsPointFilter.newBuilder()
                    .externalIds(Set.of("17325", "235", "5321"))
                    .build(),
                "data/controller/point/search_result_empty.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> locationSearchSource() {
        return Stream.of(
            Arguments.of(
                "по locationId СФО",
                LogisticsPointFilter.newBuilder()
                    .locationId(59)
                    .build(),
                "data/controller/point/search_result_3.json"
            ),
            Arguments.of(
                "по locationId Земли",
                LogisticsPointFilter.newBuilder()
                    .locationId(10000)
                    .build(),
                "data/controller/point/search_result_2_3_4.json"
            ),
            Arguments.of(
                "по верхней границе долготы",
                LogisticsPointFilter.newBuilder()
                    .longitudeTo(new BigDecimal("30"))
                    .build(),
                "data/controller/point/search_result_1.json"
            ),
            Arguments.of(
                "по нижней границе долготы",
                LogisticsPointFilter.newBuilder()
                    .longitudeFrom(new BigDecimal("50"))
                    .build(),
                "data/controller/point/search_result_3.json"
            ),
            Arguments.of(
                "по верхней границе широты",
                LogisticsPointFilter.newBuilder()
                    .latitudeTo(new BigDecimal("56.5"))
                    .build(),
                "data/controller/point/search_result_3_6_7.json"
            ),
            Arguments.of(
                "по нижней границе широты",
                LogisticsPointFilter.newBuilder()
                    .latitudeFrom(new BigDecimal("56.7"))
                    .build(),
                "data/controller/point/search_result_1.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> orderDimensionsSearchSource() {
        return Stream.of(
            Arguments.of(
                "по orderLength",
                LogisticsPointFilter.newBuilder()
                    .orderLength(100)
                    .build(),
                "data/controller/point/search_result_1_6_7.json"
            ),
            Arguments.of(
                "по orderWidth",
                LogisticsPointFilter.newBuilder()
                    .orderWidth(100)
                    .build(),
                "data/controller/point/search_result_2_6_7.json"
            ),
            Arguments.of(
                "по orderHeight",
                LogisticsPointFilter.newBuilder()
                    .orderHeight(100)
                    .build(),
                "data/controller/point/search_result_3_6_7.json"
            ),
            Arguments.of(
                "по сумме orderLength, orderWidth, orderHeight",
                LogisticsPointFilter.newBuilder()
                    .orderLength(10)
                    .orderWidth(10)
                    .orderHeight(10)
                    .build(),
                "data/controller/point/search_result_4_6_7.json"
            ),
            Arguments.of(
                "по orderWeight",
                LogisticsPointFilter.newBuilder()
                    .orderWeight(100.5)
                    .build(),
                "data/controller/point/search_result_5_6_7.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> fieldExisting() {
        return Stream.of(
            Arguments.of(
                "по существованию партнера",
                LogisticsPointFilter.newBuilder()
                    .hasPartner(true)
                    .build(),
                "data/controller/point/search_result_1_2_4_6_7.json"
            ),
            Arguments.of(
                "по отсутствию партнера",
                LogisticsPointFilter.newBuilder()
                    .hasPartner(false)
                    .build(),
                "data/controller/point/search_result_5.json"
            ),
            Arguments.of(
                "по существованию бизнес-идентификатора",
                LogisticsPointFilter.newBuilder()
                    .hasBusinessId(true)
                    .build(),
                "data/controller/point/search_result_5_6_7.json"
            ),
            Arguments.of(
                "по отсутствию бизнес-идентификатора",
                LogisticsPointFilter.newBuilder()
                    .hasBusinessId(false)
                    .build(),
                "data/controller/point/search_result_1_2_3_4.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> partnerType() {
        return Stream.of(
            Arguments.of(
                "по типам партнера",
                LogisticsPointFilter.newBuilder()
                    .partnerTypes(Set.of(PartnerType.DROPSHIP, PartnerType.SUPPLIER))
                    .build(),
                "data/controller/point/search_result_6_7.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> partnerSubtype() {
        return Stream.of(
            Arguments.of(
                "по исключённым подтипам партнера",
                LogisticsPointFilter.newBuilder()
                    .partnerSubtypesToExclude(Set.of(1L, 3L))
                    .build(),
                "data/controller/point/search_result_1_6_7.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> retailPartner() {
        return Stream.of(
            Arguments.of(
                "По типу партнера RETAIL",
                LogisticsPointFilter.newBuilder()
                    .partnerTypes(Set.of(PartnerType.RETAIL))
                    .build(),
                "data/controller/point/search_result_retail_warehouse.json"
            ),
            Arguments.of(
                "По id RETAIL партнера",
                LogisticsPointFilter.newBuilder()
                    .partnerIds(Set.of(1000L))
                    .build(),
                "data/controller/point/search_result_retail_warehouse.json"
            ),
            Arguments.of(
                "По id склада RETAIL партнера",
                LogisticsPointFilter.newBuilder()
                    .ids(Set.of(1000000L))
                    .build(),
                "data/controller/point/search_result_retail_warehouse.json"
            )
        );
    }

    @Test
    @DisplayName("Корректный запрос на изменение storagePeriod")
    @DatabaseSetup(value = "/data/controller/point/before/update_storage_period.xml",
            type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
            value = "/data/controller/point/after/update_storage_period.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateStoragePeriod() throws Exception {
        String json = mockMvc.perform(get(URI + "/8"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        LogisticsPointResponse logisticsPoint = objectMapper.readValue(json, LogisticsPointResponse.class);
        LogisticsPointUpdateRequest update = LogisticsPointUpdateRequest.fromResponse(logisticsPoint).build();

        ReflectionTestUtils.setField(update, "storagePeriod", 10);

        mockMvc.perform(
                        post(URI + "/8")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(update))
                )
                .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(8L);
    }

    @Test
    @DisplayName("Корректный запрос на изменение, полученный из LogisticsPointResponse")
    @DatabaseSetup(value = "/data/controller/point/before/get_and_update.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/get_and_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getAndUpdatePoint() throws Exception {
        String json = mockMvc.perform(get(URI + "/8"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        LogisticsPointResponse logisticsPoint = objectMapper.readValue(json, LogisticsPointResponse.class);

        LogisticsPointUpdateRequest update = LogisticsPointUpdateRequest.fromResponse(logisticsPoint).build();

        mockMvc.perform(
            post(URI + "/8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(update))
        )
            .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(8L);
    }

    @Test
    @DisplayName("Корректный запрос на изменение externalId для ДБС партнера, полученный из LogisticsPointResponse")
    @DatabaseSetup(value = "/data/controller/point/before/get_and_update_dbs.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/get_and_update_dbs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getAndUpdatePointDbsPartner() throws Exception {
        callWithExternalIdChange();
        checkBuildWarehouseSegmentTask(8L);
    }

    @Test
    @DisplayName("Корректный запрос на изменение externalId для не-ДБС партнера, полученный из LogisticsPointResponse")
    @DatabaseSetup(value = "/data/controller/point/before/get_and_update_non_dbs.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/get_and_update_non_dbs.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getAndUpdatePointNonDbsPartner() throws Exception {
        callWithExternalIdChange();
        checkBuildWarehouseSegmentTask(8L);
    }

    private void callWithExternalIdChange() throws Exception {
        String json = mockMvc.perform(get(URI + "/8"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        LogisticsPointUpdateRequest update = LogisticsPointUpdateRequest.fromResponse(
            objectMapper.readValue(
                json,
                LogisticsPointResponse.class
            )
        )
            .externalId("NEW")
            .build();

        mockMvc.perform(
            post(URI + "/8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(update))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("У точки был календарь с наследованием, в запросе closedOnRegionHolidays=false")
    @DatabaseSetup("/data/controller/point/before/location_calendar.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/works_on_holidays.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticsPointClosedOnRegionHolidaysFromTrueToFalse() throws Exception {
        createThenUpdateSetClosedOnHolidays(
            "data/controller/point/create_point_closed_on_holidays.json",
            false
        );
    }

    @Test
    @DisplayName("У точки был календарь с наследованием, в запросе closedOnRegionHolidays=true")
    @DatabaseSetup("/data/controller/point/before/location_calendar.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/closed_on_holidays.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticsPointSetClosedOnRegionHolidaysFromTrueToTrue() throws Exception {
        createThenUpdateSetClosedOnHolidays(
            "data/controller/point/create_point_closed_on_holidays.json",
            true
        );
    }

    @Test
    @DisplayName("У точки не было календаря с наследованием, в запросе closedOnRegionHolidays=true")
    @DatabaseSetup("/data/controller/point/before/location_calendar.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/closed_on_holidays.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticsPointSetClosedOnRegionHolidaysFromFalseToTrue() throws Exception {
        createThenUpdateSetClosedOnHolidays(
            "data/controller/point/create_point_successful.json",
            true
        );
    }

    @Test
    @DisplayName("У точки не было календаря с наследованием, в запросе closedOnRegionHolidays=false")
    @DatabaseSetup("/data/controller/point/before/location_calendar.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/works_on_holidays_no_new_calendar.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticsPointSetClosedOnRegionHolidaysFromFalseToFalse() throws Exception {
        createThenUpdateSetClosedOnHolidays(
            "data/controller/point/create_point_successful.json",
            false
        );
    }

    private void createThenUpdateSetClosedOnHolidays(
        String createRequestFilepath,
        boolean closedOnRegionHolidays
    ) throws Exception {
        String json = mockMvc.perform(
            post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(createRequestFilepath))
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        LogisticsPointResponse logisticsPoint = objectMapper.readValue(json, LogisticsPointResponse.class);

        LogisticsPointUpdateRequest update = LogisticsPointUpdateRequest.fromResponse(logisticsPoint)
            .closedOnRegionHolidays(closedOnRegionHolidays)
            .build();

        mockMvc.perform(
            post(URI + "/8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(update))
        )
            .andExpect(status().isOk());

        // Обновление идемпотентно
        mockMvc.perform(
            post(URI + "/8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(update))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Изменение родительского календаря")
    @DatabaseSetup("/data/controller/point/before/location_calendar.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/closed_on_holidays.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticsPointSetClosedOnRegionHolidaysTrue_calendarAlreadyInherited() throws Exception {
        String json = mockMvc.perform(
            post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/point/create_point_closed_on_holidays.json"))
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        Calendar logisticsPointCalendar = calendarService.getCalendarById(1L);
        calendarService.saveCalendar(
            logisticsPointCalendar.setParent(calendarService.getCalendarById(1001L))
        );

        LogisticsPointResponse logisticsPoint = objectMapper.readValue(json, LogisticsPointResponse.class);

        LogisticsPointUpdateRequest update = LogisticsPointUpdateRequest.fromResponse(logisticsPoint)
            .closedOnRegionHolidays(true)
            .build();

        mockMvc.perform(
            post(URI + "/8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(update))
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ошибка создания точки - deferredCourierAvailable == true при availableForOnDemand == false")
    @ExpectedDatabase(
        value = "/data/controller/point/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDeferredCourierAvailableButNonAvailableForOnDemandPointFailure() throws Exception {
        createLogisticsPointFailure(
            "data/controller/point/" +
                "create_point_with_deferred_courier_available_true_and_available_for_on_demand_false.json"
        );
    }

    @Test
    @DisplayName("Ошибка создания точки - deferredCourierAvailable == true при availableForOnDemand == null")
    @ExpectedDatabase(
        value = "/data/controller/point/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDeferredCourierAvailableButAvailableForOnDemandIsNullPointFailure() throws Exception {
        createLogisticsPointFailure(
            "data/controller/point/" +
                "create_point_with_deferred_courier_available_true_and_available_for_on_demand_null.json"
        );
    }

    @Test
    @DisplayName("Деактивация и заморозка списка точкек")
    @ExpectedDatabase(
        value = "/data/controller/point/after/logistics_points_batch_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void batchDeactivateAndFreezePoints() throws Exception {
        mockMvc.perform(post(URI + "/batch-activate-freeze")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LogisticsPointActivateFreezeRequest(
                List.of(1L, 2L, 3L),
                false,
                true
            )))
        )
            .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(3L);
    }

    @DisplayName("Невалидный запрос деактивации и заморозки списка точек")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidBodyForDeactivateAndFreezePointsValidation")
    void batchDeactivateAndFreezePointsValidation(
        @SuppressWarnings("unused") String name,
        LogisticsPointActivateFreezeRequest request,
        String response
    ) throws Exception {
        mockMvc.perform(post(URI + "/batch-activate-freeze")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.testJson(response, Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS));
    }

    @Nonnull
    private static Stream<Arguments> invalidBodyForDeactivateAndFreezePointsValidation() {
        return Stream.of(
            Triple.of(
                "Пустой список идентификаторов точек",
                new LogisticsPointActivateFreezeRequest(List.of(), false, true),
                "data/controller/point/empty_ids_list.json"
            ),
            Triple.of(
                "Список идентификаторов с null",
                new LogisticsPointActivateFreezeRequest(Arrays.asList(1L, 2L, null, 3L, 4L), false, true),
                "data/controller/point/null_id_in_ids_list.json"
            ),
            Triple.of(
                "Список идентификаторов длиннее 1000",
                new LogisticsPointActivateFreezeRequest(
                    LongStream.range(1, 1002).boxed().collect(Collectors.toList()),
                    false,
                    true
                ),
                "data/controller/point/too_large_ids_list.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @DisplayName("Получение информации о воротах логистической точки")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getLogisticsPointGates")
    void getLogisticsPointGates(
        @SuppressWarnings("unused") String name,
        Long logisticsPointId,
        String response
    ) throws Exception {
        mockMvc.perform(get(URI + "/" + logisticsPointId + "/gates"))
            .andExpect(status().isOk())
            .andExpect(testJson(response));
    }

    @Test
    @DisplayName("Получить точку выдачи партнёра: тип партнёра отличный от DROPSHIP")
    void getReturnPointForPartner_NotDropship() throws Exception {
        mockMvc.perform(get(RETURN_POINT_URI + "/1"))
            .andExpect(status().isNotFound())
            .andExpect(hasResolvedExceptionContainingMessage("Partner with id=1 and type=DROPSHIP doesn't exist"));
    }

    @Test
    @DisplayName("Получить точку выдачи партнёра: партнёр не найден")
    void getReturnPointForPartner_NotFound() throws Exception {
        mockMvc.perform(get(RETURN_POINT_URI + "/100"))
            .andExpect(status().isNotFound())
            .andExpect(hasResolvedExceptionContainingMessage("Partner with id=100 and type=DROPSHIP doesn't exist"));
    }

    @Test
    @DisplayName("Получить точку выдачи партнёра: точка выдачи мерчу отсутствует")
    void getReturnPointForPartner_NoReturnPoint() throws Exception {
        mockMvc.perform(get(RETURN_POINT_URI + "/4"))
            .andExpect(status().isOk())
            .andExpect(content().string("null"));
    }

    @Test
    @DisplayName("Получить точку выдачи партнёра: отсутствуют активные сервисы")
    @DatabaseSetup(
        type = DatabaseOperation.INSERT,
        value = "/data/controller/point/before/one_return_point_no_active_services.xml"
    )
    void getReturnPointForPartner_NoActiveServices() throws Exception {
        mockMvc.perform(get(RETURN_POINT_URI + "/4"))
            .andExpect(status().isOk())
            .andExpect(content().string("null"));
    }

    @Test
    @DisplayName("Получить точку выдачи партнёра: больше чем одна точка выдачи")
    @DatabaseSetup(
        type = DatabaseOperation.INSERT,
        value = "/data/controller/point/before/many_return_points.xml"
    )
    void getReturnPointForPartner_ManyReturnPoints() throws Exception {
        mockMvc.perform(get(RETURN_POINT_URI + "/4"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/one_return_point.json"));
    }

    @Test
    @DisplayName("Получить точку выдачи партнёра: успех")
    @DatabaseSetup(
        type = DatabaseOperation.INSERT,
        value = "/data/controller/point/before/one_return_point.xml"
    )
    void getReturnPointForPartner_OneReturnPoint() throws Exception {
        mockMvc.perform(get(RETURN_POINT_URI + "/4"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/one_return_point.json"));
    }

    @Test
    @DisplayName("Получить точку выдачи партнёра (дропофф): успех")
    @DatabaseSetup(
        type = DatabaseOperation.INSERT,
        value = "/data/controller/point/before/one_return_point_dropoff.xml"
    )
    void getReturnPointForPartner_OneReturnPoint_Dropoff() throws Exception {
        mockMvc.perform(get(RETURN_POINT_URI + "/4"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/one_return_point_dropoff.json"));
    }

    @Nonnull
    private static Stream<Arguments> getLogisticsPointGates() {
        return Stream.of(
            Arguments.of(
                "Ворота с расписаниями",
                3L,
                "data/controller/point/get_logistics_point_gates_with_schedules.json"
            ),
            Arguments.of(
                "Ворота без расписания",
                5L,
                "data/controller/point/get_logistics_point_gates_without_schedules.json"
            )
        );
    }

    private void createLogisticsPointSuccessful(String requestFilePath, String responseFilePath) throws Exception {
        mockMvc.perform(
            post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(requestFilePath))
        )
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/externalApi/logisticsPoints/8"))
            .andExpect(testJson(responseFilePath));
    }

    private void createLogisticsPointFailure(String requestFilePath) throws Exception {
        mockMvc.perform(
            post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(requestFilePath))
        ).andExpect(status().isBadRequest())
         .andExpect(result -> assertThat(result.getResponse().getErrorMessage())
             .isEqualTo("Point must be available for ondemand if it is available for deferred courier")
         );
    }

    @Nonnull
    private ResultActions search(LogisticsPointFilter filter) throws Exception {
        return mockMvc.perform(put(URI)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsBytes(filter)));
    }

    @Nonnull
    private ResultActions search(LogisticsPointFilter filter, @Nullable Pageable pageable) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = put(URI + "/page")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsBytes(filter));
        if (pageable == null) {
            return mockMvc.perform(requestBuilder);
        }
        return mockMvc.perform(
            requestBuilder
                .param("size", String.valueOf(pageable.getPageSize()))
                .param("page", String.valueOf(pageable.getPageNumber()))
        );
    }

    private void mockHttpGeobaseRegionRequest() {
        doReturn(SARKANDAUGAVA_ID).when(httpGeobase).getRegionId(anyDouble(), anyDouble());
        verifications.add(() -> verify(httpGeobase).getRegionId(anyDouble(), anyDouble()));
    }

    @Nonnull
    private static GeoObject geoObject(ToponymInfo toponymInfo, String addressLine) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(toponymInfo)
            .withAddressInfo(
                AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .withAddressLine(addressLine)
                    .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }

    @Nonnull
    private static ToponymInfo toponymInfo(@Nullable String geoId, @Nullable Kind kind, @Nullable String point) {
        return ToponymInfo.newBuilder()
            .withGeoid(geoId)
            .withKind(kind)
            .withPoint(point)
            .build();
    }
}
