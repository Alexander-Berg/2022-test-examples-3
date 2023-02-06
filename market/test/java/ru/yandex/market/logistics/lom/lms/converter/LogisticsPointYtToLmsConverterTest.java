package ru.yandex.market.logistics.lom.lms.converter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.lms.model.LogisticsPointLightModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtAddress;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPoint;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPointsAggModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtMigrationModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPhoneByLogisticsPointId;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.lom.utils.YtTestUtils;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

@DisplayName("Конвертация логистической точки из модели yt в модель lms")
class LogisticsPointYtToLmsConverterTest extends AbstractTest {
    private LogisticsPointYtToLmsConverter logisticsPointConverter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        PhoneYtToLmsConverter phoneConverter = new PhoneYtToLmsConverter(objectMapper);
        ScheduleYtToLmsConverter scheduleConverter = new ScheduleYtToLmsConverter(objectMapper);

        logisticsPointConverter = new LogisticsPointYtToLmsConverter(phoneConverter, scheduleConverter, objectMapper);
    }

    @Test
    @DisplayName("Конвертация логистической точки со всеми заполненными в yt полями")
    void convertFullModel() {
        long pointId = 1L;
        YtLogisticsPoint point = buildPoint(pointId)
            .setPhones(buildPhone(pointId))
            .setScheduleDays(buildSchedule(pointId));

        softly.assertThat(logisticsPointConverter.convert(point))
            .usingRecursiveComparison()
            .isEqualTo(expectedPoint(allFieldsFilledBuilder(pointId)));
    }

    @Test
    @DisplayName("Конвертация логистической точки без контакта")
    void convertModelWithoutContact() {
        long pointId = 1L;
        YtLogisticsPoint point = buildPoint(pointId)
            .setPhones(buildPhone(pointId))
            .setScheduleDays(buildSchedule(pointId))
            .setContactId(Optional.empty())
            .setContactName(Optional.empty())
            .setContactSurname(Optional.empty())
            .setContactPatronymic(Optional.empty());

        softly.assertThat(logisticsPointConverter.convert(point))
            .usingRecursiveComparison()
            .isEqualTo(expectedPoint(allFieldsFilledBuilder(pointId).contact(null)));
    }

    @Test
    @DisplayName("Конвертация логистической точки, в которой все nullable поля == null")
    void convertNullPartnerId() {
        long pointId = 1L;
        YtLogisticsPoint point = buildPointWithNulls(pointId)
            .setPhones(buildNullPhone(pointId))
            .setScheduleDays(buildSchedule(pointId));

        softly.assertThat(logisticsPointConverter.convert(point))
            .usingRecursiveComparison()
            .isEqualTo(expectedPoint(allNullableFieldsAreNullBuilder(pointId)));
    }

    @Test
    @DisplayName("Извлечение id точек из агрегированной модели")
    void extractPointIdsTest() {
        softly.assertThat(logisticsPointConverter.extractLogisticsPointIds(buildAggModel(1L)))
            .isEqualTo(Set.of(100L, 101L, 102L));
    }

    @DisplayName("Извлечение модели логистической точки из итератора YT")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getYtMigrationModelsArguments")
    void oneYtMigrationModelsConvertTest(
        @SuppressWarnings("unused") String displayName,
        Class<? extends YtMigrationModel> modelClass,
        Map<String, ?> mapToConvert,
        YtMigrationModel expected
    ) {
        Iterator<YTreeMapNode> iterator =
            YtTestUtils.getIterator(List.of(YtTestUtils.buildMapNode(mapToConvert)));

        Optional<? extends YtMigrationModel> actual =
            logisticsPointConverter.extractEntityFromRow(iterator, modelClass);

        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get())
            .usingRecursiveComparison()
            .isEqualTo(expected);
    }

    static Stream<Arguments> getYtMigrationModelsArguments() {
        return Stream.of(
            Arguments.of(
                "Конвертация логистической точки из итератора YT",
                YtLogisticsPoint.class,
                buildPointMap(1L),
                buildPoint(1L)
            ),
            Arguments.of(
                "Конвертация расписания логистической точки из итератора YT",
                YtScheduleDays.class,
                buildScheduleMap(1L),
                buildSchedule(1L)
            ),
            Arguments.of(
                "Конвертация телефона логистической точки из итератора YT",
                YtPhoneByLogisticsPointId.class,
                buildPhonesMap(1L),
                buildPhone(1L)
            ),
            Arguments.of(
                "Конвертация агрегрированных логистических точек из итератора YT",
                YtLogisticsPointsAggModel.class,
                buildLogisticsPointAggMap(1L),
                buildAggModel(1L)
            )
        );
    }

    @Test
    @DisplayName("Ошибка при попытке десериализации неподдерживаемого класса")
    void errorOnDeserializingUnsupportedClass() {
        Class<? extends YtMigrationModel> cls = YtMigrationModel.class;
        Iterator<YTreeMapNode> iterator =
            YtTestUtils.getIterator(List.of(YtTestUtils.buildMapNode(Map.of("field", "value"))));

        softly.assertThatThrownBy(() -> logisticsPointConverter.extractEntitiesFromRows(iterator, cls))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Destination class YtMigrationModel is not supported by converter");
    }

    @DisplayName("Извлечение нескольких моделей логистической точки из итератора YT")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getManyYtMigrationModelsArguments")
    void manyYtMigrationModelsConvertTest(
        @SuppressWarnings("unused") String displayName,
        Class<? extends YtMigrationModel> modelClass,
        List<Map<String, ?>> mapsToConvert,
        List<YtMigrationModel> expectedModels
    ) {
        Iterator<YTreeMapNode> iterator =
            YtTestUtils.getIterator(mapsToConvert.stream().map(YtTestUtils::buildMapNode).collect(Collectors.toList()));

        List<? extends YtMigrationModel> actual = logisticsPointConverter.extractEntitiesFromRows(iterator, modelClass);

        softly.assertThat(actual).isNotEmpty();
        softly.assertThat(expectedModels).containsExactlyInAnyOrderElementsOf(actual);
    }

    static Stream<Arguments> getManyYtMigrationModelsArguments() {
        return Stream.of(
            Arguments.of(
                "Конвертация логистических точек из итератора YT",
                YtLogisticsPoint.class,
                List.of(buildPointMap(1L), buildPointMap(2L)),
                List.of(buildPoint(1L), buildPoint(2L))
            ),
            Arguments.of(
                "Конвертация расписаний логистических точкек из итератора YT",
                YtScheduleDays.class,
                List.of(buildScheduleMap(1L), buildScheduleMap(2L)),
                List.of(buildSchedule(1L), buildSchedule(2L))
            ),
            Arguments.of(
                "Конвертация телефонов логистической точки из итератора YT",
                YtPhoneByLogisticsPointId.class,
                List.of(buildPhonesMap(1L), buildPhonesMap(2L)),
                List.of(buildPhone(1L), buildPhone(2L))
            ),
            Arguments.of(
                "Конвертация списка агрегрированных логистических точек из итератора YT",
                YtLogisticsPointsAggModel.class,
                List.of(buildLogisticsPointAggMap(1L), buildLogisticsPointAggMap(2L)),
                List.of(buildAggModel(1L), buildAggModel(2L))
            )
        );
    }

    @Nonnull
    private LogisticsPointLightModel expectedPoint(
        LogisticsPointResponse.LogisticsPointResponseBuilder pointBuilder
    ) {
        return LogisticsPointLightModel.build(pointBuilder.build());
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder allFieldsFilledBuilder(long id) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .active(true)
            .instruction("instruction" + id)
            .type(PointType.WAREHOUSE)
            .name("name-" + id)
            .externalId("ext-id-" + id)
            .partnerId(id + 1)
            .contact(
                new Contact("contact_name-" + id, "contact_surname-" + id, "contact_patronymic-" + id)
            )
            .address(
                Address.newBuilder()
                    .locationId((int) id)
                    .country("country-" + id)
                    .settlement("address_settlement-" + id)
                    .postCode("address_post_code-" + id)
                    .latitude(BigDecimal.valueOf(2.576))
                    .longitude(BigDecimal.valueOf(3.576))
                    .street("street-" + id)
                    .house("address_house-" + id)
                    .housing("address_housing-" + id)
                    .building("address_building-" + id)
                    .region("address_region-" + id)
                    .apartment("address_apartment-" + id)
                    .comment("address_comment-" + id)
                    .build()
            )
            .phones(
                Set.of(new Phone("+7(495)9843122", String.valueOf(id * 100), null, null))
            )
            .schedule(buildExpectedSchedule((int) id));
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder allNullableFieldsAreNullBuilder(long id) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .instruction(null)
            .type(PointType.WAREHOUSE)
            .name(null)
            .externalId("ext-id-" + id)
            .partnerId(null)
            .contact(null)
            .address(Address.newBuilder().country("Россия").build())
            .phones(Collections.emptySet())
            .schedule(buildExpectedSchedule((int) id));
    }

    @Nonnull
    Set<ScheduleDayResponse> buildExpectedSchedule(int day) {
        return Set.of(
            new ScheduleDayResponse(
                37465941L,
                day,
                LocalTime.of(12, 00),
                LocalTime.of(21, 00),
                false
            )
        );
    }

    @Nonnull
    private static YtLogisticsPoint buildPoint(long id) {
        return new YtLogisticsPoint()
            .setId(id)
            .setName(Optional.of("name-" + id))
            .setExternalId("ext-id-" + id)
            .setContactId(Optional.of(id + 1))
            .setContactName(Optional.of("contact_name-" + id))
            .setContactSurname(Optional.of("contact_surname-" + id))
            .setContactPatronymic(Optional.of("contact_patronymic-" + id))
            .setScheduleId(Optional.of(id))
            .setActive(true)
            .setInstruction(Optional.of("instruction" + id))
            .setType(PointType.WAREHOUSE)
            .setPartnerId(Optional.of(id + 1))
            .setAddress(
                new YtAddress()
                    .setId(id + 1)
                    .setLocationId(Optional.of((int) id))
                    .setCountry(Optional.of("country-" + id))
                    .setSettlement(Optional.of("address_settlement-" + id))
                    .setPostCode(Optional.of("address_post_code-" + id))
                    .setLatitude(Optional.of(2.576))
                    .setLongitude(Optional.of(3.576))
                    .setStreet(Optional.of("street-" + id))
                    .setHouse(Optional.of("address_house-" + id))
                    .setHousing(Optional.of("address_housing-" + id))
                    .setBuilding(Optional.of("address_building-" + id))
                    .setRegion(Optional.of("address_region-" + id))
                    .setApartment(Optional.of("address_apartment-" + id))
                    .setComment(Optional.of("address_comment-" + id))
            );
    }

    @Nonnull
    private YtLogisticsPoint buildPointWithNulls(long id) {
        return new YtLogisticsPoint()
            .setId(id)
            .setName(Optional.empty())
            .setExternalId("ext-id-" + id)
            .setContactId(Optional.empty())
            .setContactName(Optional.empty())
            .setContactSurname(Optional.empty())
            .setContactPatronymic(Optional.empty())
            .setScheduleId(Optional.empty())
            .setInstruction(Optional.empty())
            .setType(PointType.WAREHOUSE)
            .setPartnerId(Optional.empty())
            .setAddress(
                new YtAddress()
                    .setId(id + 1)
                    .setLocationId(Optional.empty())
                    .setCountry(Optional.empty())
                    .setSettlement(Optional.empty())
                    .setPostCode(Optional.empty())
                    .setLatitude(Optional.empty())
                    .setLongitude(Optional.empty())
                    .setStreet(Optional.empty())
                    .setHouse(Optional.empty())
                    .setHousing(Optional.empty())
                    .setBuilding(Optional.empty())
                    .setRegion(Optional.empty())
                    .setApartment(Optional.empty())
                    .setComment(Optional.empty())
            );
    }

    @Nonnull
    private static YtPhoneByLogisticsPointId buildPhone(long id) {
        return new YtPhoneByLogisticsPointId()
            .setId(id)
            .setPhones(Optional.of(
                "{\"phones\":[{"
                    + "\"internal_number\":" + id * 100 + ","
                    + "\"number\":\"+7(495)9843122\""
                    + "}]}"
            ));
    }

    @Nonnull
    private YtPhoneByLogisticsPointId buildNullPhone(long id) {
        return new YtPhoneByLogisticsPointId()
            .setId(id)
            .setPhones(Optional.empty());
    }

    @Nonnull
    private static YtScheduleDays buildSchedule(long id) {
        return new YtScheduleDays()
            .setId(id)
            .setScheduleDays(
                "{\"schedule_days\":["
                    + "{\"day\":" + id + ","
                    + "\"id\":" + (37465940 + id) + ","
                    + "\"is_main\":false,"
                    + "\"time_from\":\"12:00:00.000000\","
                    + "\"time_to\":\"21:00:00.000000\""
                    + "}"
                    + "]}"
            );
    }

    @Nonnull
    private static YtLogisticsPointsAggModel buildAggModel(long partnerId) {
        return new YtLogisticsPointsAggModel()
            .setPartnerId(Optional.of(partnerId))
            .setActive(true)
            .setType(PointType.WAREHOUSE)
            .setLogisticsPoints("{\"logistics_points\":[100,101,102]}");
    }

    @Nonnull
    private static Map<String, ?> buildPointMap(long id) {
        return Map.ofEntries(
            Map.entry("id", id),
            Map.entry("active", true),
            Map.entry("instruction", "instruction" + id),
            Map.entry("type", PointType.WAREHOUSE.toString()),
            Map.entry("name", "name-" + id),
            Map.entry("external_id", "ext-id-" + id),
            Map.entry("partner_id", id + 1),
            Map.entry("contact_id", id + 1),
            Map.entry("contact_name", "contact_name-" + id),
            Map.entry("contact_surname", "contact_surname-" + id),
            Map.entry("contact_patronymic", "contact_patronymic-" + id),
            Map.entry("address_id", id + 1),
            Map.entry("address_location_id", (int) id),
            Map.entry("address_country", "country-" + id),
            Map.entry("address_settlement", "address_settlement-" + id),
            Map.entry("address_post_code", "address_post_code-" + id),
            Map.entry("address_latitude", 2.576),
            Map.entry("address_longitude", 3.576),
            Map.entry("address_street", "street-" + id),
            Map.entry("address_house", "address_house-" + id),
            Map.entry("address_housing", "address_housing-" + id),
            Map.entry("address_building", "address_building-" + id),
            Map.entry("address_region", "address_region-" + id),
            Map.entry("address_apartment", "address_apartment-" + id),
            Map.entry("address_comment", "address_comment-" + id),
            Map.entry("schedule_id", id)
        );
    }

    @Nonnull
    private static Map<String, ?> buildScheduleMap(long id) {
        return Map.ofEntries(
            Map.entry("schedule_id", id),
            Map.entry("schedule_days", buildScheduleDaysJson(id))
        );
    }

    @Nonnull
    private static String buildScheduleDaysJson(long id) {
        return "{\"schedule_days\":["
            + "{\"day\":" + id + ","
            + "\"id\":" + (37465940 + id) + ","
            + "\"is_main\":false,"
            + "\"time_from\":\"12:00:00.000000\","
            + "\"time_to\":\"21:00:00.000000\""
            + "}"
            + "]}";
    }

    @Nonnull
    private static Map<String, ?> buildPhonesMap(long id) {
        return Map.ofEntries(
            Map.entry("id", id),
            Map.entry("phones", buildPhonesJson(id))
        );
    }

    @Nonnull
    private static String buildPhonesJson(long id) {
        return "{\"phones\":[{"
            + "\"internal_number\":" + id * 100 + ","
            + "\"number\":\"+7(495)9843122\""
            + "}]}";
    }

    @Nonnull
    private static Map<String, ?> buildLogisticsPointAggMap(long id) {
        return Map.ofEntries(
            Map.entry("active", true),
            Map.entry("partner_id", id),
            Map.entry("type", PointType.WAREHOUSE.toString()),
            Map.entry("logistics_points", buildLogisticsPointsJson())
        );
    }

    @Nonnull
    private static String buildLogisticsPointsJson() {
        return "{\"logistics_points\":[100,101,102]}";
    }
}
