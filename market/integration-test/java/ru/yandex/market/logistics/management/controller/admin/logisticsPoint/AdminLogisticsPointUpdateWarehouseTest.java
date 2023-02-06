package ru.yandex.market.logistics.management.controller.admin.logisticsPoint;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistics.front.library.annotation.Editable;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.ScheduleDto;
import ru.yandex.market.logistics.management.domain.dto.front.logisticsPoint.WarehouseAddressDto;
import ru.yandex.market.logistics.management.domain.dto.front.logisticsPoint.WarehouseDetailDto;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.facade.LogisticsPointFacade;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_EDIT;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_REGION_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;
import static ru.yandex.market.logistics.management.util.TestUtil.point;

@ParametersAreNonnullByDefault
@DisplayName("Обновление или замена склада")
class AdminLogisticsPointUpdateWarehouseTest extends AbstractContextualTest {

    private static final String GEO_QUERY =
        "Новосибирская область, test-sub-region, Новосибирск, Николаева, дом 11, строение 1, корпус a";
    private static final double LATITUDE = 56.948048;
    private static final double LONGITUDE = 24.107018;
    private static final Long NEW_WAREHOUSE_ID = 1L;
    private static final Long OLD_WAREHOUSE_ID = 10004L;
    private static final String ERROR_MESSAGE_REQUIRED = "Обязательно для заполнения";
    private static final String ERROR_MESSAGE_GATE_SCHEDULE_WITHIN_WAREHOUSE =
        "Должно быть в пределах времени работы склада";

    @Autowired
    private LogisticsPointFacade logisticsPointFacade;

    @Autowired
    private GeoClient geoClient;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private RegionService regionService;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        when(geoClient.find(eq(GEO_QUERY), any(GeoSearchParams.class)))
            .thenReturn(List.of(geoObject(65, LATITUDE, LONGITUDE)));
        when(httpGeobase.getRegionId(anyDouble(), anyDouble()))
            .thenReturn(MOSCOW_REGION_ID);
        when(regionService.get()).thenReturn(buildRegionTree());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
        verifyNoMoreInteractions(geoClient, httpGeobase, regionService);
    }

    @DisplayName("Валидация dto обновления склада")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void updateWarehouseValidationError(String field, Consumer<WarehouseDetailDto> warehouseUpdater, String message)
        throws Exception {
        WarehouseDetailDto warehouse = createWarehouse();
        warehouseUpdater.accept(warehouse);
        mockMvc.perform(updateLogisticsPoint(1L, objectMapper.writeValueAsString(warehouse)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors.length()").value(1))
            .andExpect(jsonPath("errors[0].field").value(field))
            .andExpect(jsonPath("errors[0].defaultMessage").value(message));
    }

    @Nonnull
    private static Stream<Arguments> updateWarehouseValidationError() {
        var triples = Stream.<Triple<String, Consumer<WarehouseDetailDto>, String>>of(
            Triple.of("contactFirstName", w -> w.setContactFirstName(null), ERROR_MESSAGE_REQUIRED),
            Triple.of("contactLastName", w -> w.setContactLastName(null), ERROR_MESSAGE_REQUIRED),
            Triple.of("phoneNumber", w -> w.setPhoneNumber(null), ERROR_MESSAGE_REQUIRED),
            Triple.of("mondayFrom", w -> w.setMondayFrom(null), ERROR_MESSAGE_REQUIRED),
            Triple.of("mondayTo", w -> w.setMondayTo(null), ERROR_MESSAGE_REQUIRED),
            Triple.of("gateSchedule.mondayFrom", w -> w.getGateSchedule()
                .setMondayFrom(null), ERROR_MESSAGE_REQUIRED),
            Triple.of("gateSchedule.mondayTo", w -> w.getGateSchedule()
                .setMondayTo(null), ERROR_MESSAGE_REQUIRED),
            Triple.of("gateSchedule.mondayTo", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.parse("09:00")), ERROR_MESSAGE_GATE_SCHEDULE_WITHIN_WAREHOUSE)
        );
        return triples.map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @DisplayName(
        "Замена склада при обновлении полей, которые видны наружу в API. " +
            "У точки отсутствует locationId, поэтому обогащаем данными из geoClient'а"
    )
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("replaceWarehouseArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup("/data/controller/admin/logisticsPoint/update_warehouse.xml")
    void replaceWarehouse(String fieldName, Consumer<WarehouseDetailDto> warehouseUpdater) {
        replaceOrUpdate(
            fieldName,
            warehouseUpdater.andThen(address(w -> w.setLocationId(null))),
            not(equalTo(OLD_WAREHOUSE_ID.intValue()))
        );
        // any(String.class), потому что поля адреса обновляются
        verify(geoClient).find(any(String.class), any(GeoSearchParams.class));
        checkBuildWarehouseSegmentTask(OLD_WAREHOUSE_ID, NEW_WAREHOUSE_ID);
    }

    @DisplayName(
        "Замена склада при обновлении полей, которые видны наружу в API. " +
            "У точки проставлен locationId, поэтому запросов в geoClient нет"
    )
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("replaceWarehouseArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup("/data/controller/admin/logisticsPoint/update_warehouse.xml")
    void replaceWarehouseWithLocationId(String fieldName, Consumer<WarehouseDetailDto> warehouseUpdater) {
        replaceOrUpdate(fieldName, warehouseUpdater, not(equalTo(OLD_WAREHOUSE_ID.intValue())));
        checkBuildWarehouseSegmentTask(OLD_WAREHOUSE_ID, NEW_WAREHOUSE_ID);
    }

    @DisplayName("Замена склада без связанного партнера при обновлении полей, которые видны наружу в API")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("replaceWarehouseArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup("/data/controller/admin/logisticsPoint/update_warehouse_without_partner.xml")
    void replaceWarehouseWithoutPartner(String fieldName, Consumer<WarehouseDetailDto> warehouseUpdater) {
        replaceOrUpdate(
            fieldName,
            warehouseUpdater.andThen(address(w -> w.setLocationId(null))),
            not(equalTo(OLD_WAREHOUSE_ID.intValue()))
        );
        // any(String.class), потому что поля адреса обновляются
        verify(geoClient).find(any(String.class), any(GeoSearchParams.class));
        checkBuildWarehouseSegmentTask(OLD_WAREHOUSE_ID, NEW_WAREHOUSE_ID);
    }

    @Nonnull
    private static Stream<Arguments> replaceWarehouseArguments() {
        return replaceWarehouseFieldSetterPairStream().map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static Stream<Pair<String, Consumer<WarehouseDetailDto>>> replaceWarehouseFieldSetterPairStream() {
        return Stream.of(
            Pair.of("postCode", address(w -> w.setPostCode("111111"))),
            Pair.of("country", address(w -> w.setCountry("Россия"))),
            Pair.of("region", address(w -> w.setRegion("updatedRegion"))),
            Pair.of("subRegion", address(w -> w.setSubRegion("updatedSubRegion"))),
            Pair.of("settlement", address(w -> w.setSettlement("updatedSettlement"))),
            Pair.of("street", address(w -> w.setStreet("updatedStreet"))),
            Pair.of("house", address(w -> w.setHouse("updatedHouse"))),
            Pair.of("housing", address(w -> w.setHousing("updatedHousing"))),
            Pair.of("building", address(w -> w.setBuilding("updatedBuilding"))),
            Pair.of("estate", address(w -> w.setEstate("updatedEstate"))),
            Pair.of("apartment", address(w -> w.setApartment("updatedApartment"))),
            Pair.of("km", address(w -> w.setKm(123456789)))
        );
    }

    @DisplayName(
        "Обновление склада при обновлении полей, которые не видны наружу. " +
            "У точки отсутствует locationId, поэтому обогащаем данными из geoClient'а"
    )
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updateWarehouseArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup("/data/controller/admin/logisticsPoint/update_warehouse.xml")
    void updateWarehouse(String field, Consumer<WarehouseDetailDto> warehouseUpdater) {
        replaceOrUpdate(
            field,
            warehouseUpdater.andThen(address(w -> w.setLocationId(null))),
            equalTo(OLD_WAREHOUSE_ID.intValue())
        );
        verify(geoClient).find(eq(GEO_QUERY), any(GeoSearchParams.class));
        checkBuildWarehouseSegmentTask(OLD_WAREHOUSE_ID);
    }

    @DisplayName(
        "Обновление склада при обновлении полей, которые не видны наружу. " +
            "У точки проставлен locationId, поэтому запросов в geoClient нет"
    )
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updateWarehouseArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup("/data/controller/admin/logisticsPoint/update_warehouse.xml")
    void updateWarehouseWithLocationId(String field, Consumer<WarehouseDetailDto> warehouseUpdater) {
        replaceOrUpdate(field, warehouseUpdater, equalTo(OLD_WAREHOUSE_ID.intValue()));
        checkBuildWarehouseSegmentTask(OLD_WAREHOUSE_ID);
    }

    @DisplayName("Обновление склада без связанного партнера при обновлении полей, которые не видны наружу")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updateWarehouseArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup("/data/controller/admin/logisticsPoint/update_warehouse_without_partner.xml")
    void updateWarehouseWithoutPartner(String fieldName, Consumer<WarehouseDetailDto> warehouseUpdater) {
        replaceOrUpdate(
            fieldName,
            warehouseUpdater.andThen(address(w -> w.setLocationId(null))),
            equalTo(OLD_WAREHOUSE_ID.intValue())
        );
        verify(geoClient).find(eq(GEO_QUERY), any(GeoSearchParams.class));
        checkBuildWarehouseSegmentTask(OLD_WAREHOUSE_ID);
    }

    @Test
    @DisplayName("Есть тесты на изменение всех редактируемых полей dto склада")
    void allFieldsAreCovered() {
        Set<String> testingFieldNames = Stream.concat(
                updateWarehouseFieldSetterPairStream(),
                replaceWarehouseFieldSetterPairStream()
            )
            .map(Pair::getLeft)
            .collect(Collectors.toSet());
        Set<String> allEditableFields = StreamEx.<String>of()
            .append(
                FieldUtils.getFieldsListWithAnnotation(WarehouseDetailDto.class, Editable.class).stream()
                    .map(Field::getName)
                    .filter(cField -> !cField.equals("gateSchedule"))
                    .filter(cField -> !cField.equals("address"))
            )
            .append(
                FieldUtils.getFieldsListWithAnnotation(ScheduleDto.class, Editable.class).stream()
                    .map(Field::getName)
                    .map(f -> "gateSchedule." + f)
            )
            .append(
                FieldUtils.getFieldsListWithAnnotation(ScheduleDto.class, Editable.class).stream()
                    .map(Field::getName)
            )
            .collect(Collectors.toSet());
        softly.assertThat(testingFieldNames).containsAll(allEditableFields);
    }

    @Test
    @DisplayName("Есть тесты на изменение всех редактируемых полей dto склада")
    @DatabaseSetup("/data/controller/admin/logisticsPoint/update_warehouse_enrich_subregion.xml")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {AUTHORITY_ROLE_LOGISTICS_POINT, AUTHORITY_ROLE_LOGISTICS_POINT_EDIT}
    )
    void updateWarehouseEnrichSubRegion() {
        String query = "Москва и Московская область, " +
            "Волоколамское городское поселение, Николаева, дом 11, строение 1, корпус a";
        double latitude = 241.07018;
        double longitude = 69.480482;
        when(geoClient.find(anyString(), any(GeoSearchParams.class)))
            .thenReturn(List.of(geoObject(213977, latitude, longitude)));
        replaceOrUpdate("maxWeight", w -> w.setMaxWeight(100.), equalTo(NEW_WAREHOUSE_ID.intValue()));
        /* * DELIVERY-29742 Убрал автоприсваивание городского округа
        mockMvc.perform(get("/admin/lms/logistics-point/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("item.values.subRegion")
            .value("Волоколамский район"));*/
        verify(geoClient).find(eq(query), any(GeoSearchParams.class));
        verify(httpGeobase).getRegionId(latitude, longitude);
        verify(regionService).get();
        checkBuildWarehouseSegmentTask(OLD_WAREHOUSE_ID, NEW_WAREHOUSE_ID);
    }

    @Test
    @DisplayName("Активация изменённой логистической точки")
    @DatabaseSetup("/data/controller/admin/logisticsPoint/before/activate_changed_warehouse.xml")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {AUTHORITY_ROLE_LOGISTICS_POINT, AUTHORITY_ROLE_LOGISTICS_POINT_EDIT}
    )
    void activateChangedLogisticsPoint() throws Exception {
        WarehouseDetailDto warehouse =
            (WarehouseDetailDto) logisticsPointFacade.getLogisticsPointDetail(OLD_WAREHOUSE_ID);
        warehouse.setActive(true);
        mockMvc.perform(updateLogisticsPoint(OLD_WAREHOUSE_ID, objectMapper.writeValueAsString(warehouse)))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "Точка 10004 не может быть активирована, так как была заменена на точку с идентификатором 10005"
            ));
    }

    @Nonnull
    private static Stream<Arguments> updateWarehouseArguments() {
        return updateWarehouseFieldSetterPairStream().map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static Stream<Pair<String, Consumer<WarehouseDetailDto>>> updateWarehouseFieldSetterPairStream() {
        return Stream.of(
            Pair.of("name", w -> w.setName("updatedName")),
            Pair.of("maxWeight", w -> w.setMaxWeight(100.)),
            Pair.of("maxLength", w -> w.setMaxLength(100)),
            Pair.of("maxWidth", w -> w.setMaxWidth(100)),
            Pair.of("maxHeight", w -> w.setMaxHeight(100)),
            Pair.of("maxSidesSum", w -> w.setMaxSidesSum(100)),
            Pair.of("storagePeriod", w -> w.setStoragePeriod(30)),
            Pair.of("prepayAllowed", w -> w.setPrepayAllowed(false)),
            Pair.of("active", w -> w.setActive(false)),
            Pair.of("frozen", w -> w.setFrozen(true)),
            Pair.of("locationZoneId", w -> w.setLocationZoneId(10003L)),
            Pair.of("contactFirstName", w -> w.setContactFirstName("updatedFirstName")),
            Pair.of("contactLastName", w -> w.setContactLastName("updatedLastName")),
            Pair.of("contactMiddleName", w -> w.setContactMiddleName("updatedMiddleName")),
            Pair.of("phoneNumber", w -> w.setPhoneNumber("updatedPhone")),
            Pair.of("internalPhoneNumber", w -> w.setInternalPhoneNumber("updateInternalPhoneNumber")),
            Pair.of("instruction", w -> w.setInstruction("updatedInstruction")),
            Pair.of("mondayFrom", w -> w.setMondayFrom(LocalTime.of(0, 0))),
            Pair.of("mondayTo", w -> w.setMondayTo(LocalTime.of(23, 59))),
            Pair.of("tuesdayFrom", w -> w.setTuesdayFrom(LocalTime.of(0, 0))),
            Pair.of("tuesdayTo", w -> w.setTuesdayTo(LocalTime.of(23, 59))),
            Pair.of("wednesdayFrom", w -> w.setWednesdayFrom(LocalTime.of(0, 0))),
            Pair.of("wednesdayTo", w -> w.setWednesdayTo(LocalTime.of(23, 59))),
            Pair.of("thursdayFrom", w -> w.setThursdayFrom(LocalTime.of(0, 0))),
            Pair.of("thursdayTo", w -> w.setThursdayTo(LocalTime.of(23, 59))),
            Pair.of("fridayFrom", w -> w.setFridayFrom(LocalTime.of(0, 0))),
            Pair.of("fridayTo", w -> w.setFridayTo(LocalTime.of(23, 59))),
            Pair.of("saturdayFrom", w -> w.setSaturdayFrom(LocalTime.of(0, 0))),
            Pair.of("saturdayTo", w -> w.setSaturdayTo(LocalTime.of(23, 59))),
            Pair.of("sundayFrom", w -> w.setSundayFrom(LocalTime.of(0, 0))),
            Pair.of("sundayTo", w -> w.setSundayTo(LocalTime.of(23, 59))),
            Pair.of("handlingTimeDays", w -> w.setHandlingTimeDays(1L)),
            Pair.of("gateSchedule.mondayFrom", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.of(10, 0))),
            Pair.of("gateSchedule.mondayTo", w -> w.getGateSchedule()
                .setMondayTo(LocalTime.of(11, 0))),
            Pair.of("gateSchedule.tuesdayFrom", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.of(10, 0))),
            Pair.of("gateSchedule.tuesdayTo", w -> w.getGateSchedule()
                .setMondayTo(LocalTime.of(11, 0))),
            Pair.of("gateSchedule.wednesdayFrom", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.of(10, 0))),
            Pair.of("gateSchedule.wednesdayTo", w -> w.getGateSchedule()
                .setMondayTo(LocalTime.of(11, 0))),
            Pair.of("gateSchedule.thursdayFrom", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.of(10, 0))),
            Pair.of("gateSchedule.thursdayTo", w -> w.getGateSchedule()
                .setMondayTo(LocalTime.of(11, 0))),
            Pair.of("gateSchedule.fridayFrom", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.of(10, 0))),
            Pair.of("gateSchedule.fridayTo", w -> w.getGateSchedule()
                .setMondayTo(LocalTime.of(11, 0))),
            Pair.of("gateSchedule.saturdayFrom", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.of(10, 0))),
            Pair.of("gateSchedule.saturdayTo", w -> w.getGateSchedule()
                .setMondayTo(LocalTime.of(11, 0))),
            Pair.of("gateSchedule.sundayFrom", w -> w.getGateSchedule()
                .setMondayFrom(LocalTime.of(10, 0))),
            Pair.of("gateSchedule.sundayTo", w -> w.getGateSchedule()
                .setMondayTo(LocalTime.of(11, 0)))
        );
    }

    @SneakyThrows
    private void replaceOrUpdate(
        String fieldName,
        Consumer<WarehouseDetailDto> warehouseUpdater,
        Matcher<Integer> idMatcher
    ) {
        WarehouseDetailDto warehouse =
            (WarehouseDetailDto) logisticsPointFacade.getLogisticsPointDetail(OLD_WAREHOUSE_ID);
        warehouseUpdater.accept(warehouse);
        mockMvc.perform(updateLogisticsPoint(OLD_WAREHOUSE_ID, objectMapper.writeValueAsString(warehouse)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("item.id").value(idMatcher))
            .andExpect(
                jsonPath(String.format("item.values.['%s']", fieldName))
                    .value(equalTo(jsonValue(getFieldValue(fieldName, warehouse))))
            );
    }

    @Nullable
    @SneakyThrows
    private Object getFieldValue(String fieldName, WarehouseDetailDto warehouse) {
        String jsonString = objectMapper.writeValueAsString(warehouse);
        Map<String, Object> fieldsByName = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
        });
        return fieldsByName.get(fieldName);
    }

    @Nonnull
    private Object jsonValue(Object value) {
        if (value instanceof LocalTime) {
            return ((LocalTime) value).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        return value;
    }

    @Nonnull
    static WarehouseDetailDto createWarehouse() {
        WarehouseDetailDto warehouse = (WarehouseDetailDto) new WarehouseDetailDto()
            .setContactFirstName("Иван")
            .setContactLastName("Иванов")
            .setPhoneNumber("+74959998877")
            .setAddress(
                new WarehouseAddressDto()
                    .setPostCode("630090")
                    .setRegion("Новосибирская область")
                    .setSettlement("Новосибирск")
                    .setStreet("Николаева")
                    .setHouse("11")
            )
            .setMondayFrom(LocalTime.parse("10:00"))
            .setMondayTo(LocalTime.parse("18:00"));

        ScheduleDto gateSchedule = new ScheduleDto();
        gateSchedule.setMondayFrom(LocalTime.parse("10:00"));
        gateSchedule.setMondayTo(LocalTime.parse("18:00"));
        warehouse.setGateSchedule(gateSchedule);
        warehouse.setType(PointType.WAREHOUSE.name()).setActive(true);
        return warehouse;
    }

    @Nonnull
    private MockHttpServletRequestBuilder updateLogisticsPoint(long id, String body) {
        return put("/admin/lms/logistics-point/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    @Nonnull
    private static Consumer<WarehouseDetailDto> address(Consumer<WarehouseAddressDto> addressModifier) {
        return warehouse -> addressModifier.accept(warehouse.getAddress());
    }

    @Nonnull
    private static SimpleGeoObject geoObject(Integer geoId, double latitude, double longitude) {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(
                ToponymInfo.newBuilder()
                    .withGeoid(String.valueOf(geoId))
                    .withPoint(point(latitude, longitude))
                    .build()
            )
            .withAddressInfo(
                AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }
}
