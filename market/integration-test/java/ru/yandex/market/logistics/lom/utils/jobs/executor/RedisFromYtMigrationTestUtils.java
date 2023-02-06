package ru.yandex.market.logistics.lom.utils.jobs.executor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.SneakyThrows;

import ru.yandex.inside.yt.kosher.cypress.Range;
import ru.yandex.inside.yt.kosher.cypress.RangeLimit;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.service.yt.dto.YtAddress;
import ru.yandex.market.logistics.lom.service.yt.dto.YtInboundSchedule;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPoint;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPointsAggModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartner;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerApiSettings;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerExternalParam;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelation;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelationTo;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPhoneByLogisticsPointId;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDayById;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDays;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

@ParametersAreNonnullByDefault
public class RedisFromYtMigrationTestUtils {

    public static final long ROWS_COUNT = 3L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

    private RedisFromYtMigrationTestUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static YPath expectedPath(String tableName) {
        return YPath.simple(tableName).plusRange(new Range(RangeLimit.row(0), RangeLimit.row(5000)));
    }

    @Nonnull
    public static Map<String, ?> buildMapForTable(
        LmsYtProperties lmsYtProperties,
        String version,
        String tableName,
        long rowIndex
    ) {
        if (lmsYtProperties.getLogisticsPointPath(version).equals(tableName)) {
            return buildPointMap(rowIndex);
        }

        if (lmsYtProperties.getSchedulePath(version).equals(tableName)) {
            return buildScheduleMap(rowIndex);
        }

        if (lmsYtProperties.getPhonesPath(version).equals(tableName)) {
            return buildPhonesMap(rowIndex);
        }

        if (lmsYtProperties.getLogisticsPointsAggPath(version).equals(tableName)) {
            return buildLogisticsPointAggMap(rowIndex);
        }

        if (lmsYtProperties.getPartnerPath(version).equals(tableName)) {
            return buildPartnerMap(rowIndex);
        }

        if (lmsYtProperties.getPartnerExternalParamPath(version).equals(tableName)) {
            return buildPartnerExternalParamsMap(rowIndex);
        }

        if (lmsYtProperties.getPartnerRelationPath(version).equals(tableName)) {
            return buildPartnerRelationMap(rowIndex);
        }

        if (lmsYtProperties.getPartnerRelationToPath(version).equals(tableName)) {
            return buildPartnerRelationToMap(rowIndex);
        }

        if (lmsYtProperties.getInboundSchedulePath(version).equals(tableName)) {
            return buildInboundScheduleMap(rowIndex);
        }

        if (lmsYtProperties.getDynamicScheduleDayByIdPath(version).equals(tableName)) {
            return buildScheduleDayByIdMap(rowIndex);
        }

        if (lmsYtProperties.getPartnerApiSettingsPath(version).equals(tableName)) {
            return buildPartnerApiSettingsMap(rowIndex);
        }

        throw new IllegalStateException("Calling method for not implemented test map");
    }

    @Nonnull
    @SneakyThrows
    public static Map<String, String> expectedEntityMap(
        LmsYtProperties lmsYtProperties,
        String version,
        String tableName,
        int fromId,
        int toId
    ) {
        if (lmsYtProperties.getLogisticsPointPath(version).equals(tableName)) {
            return expectedPointsMap(fromId, toId);
        }

        if (lmsYtProperties.getSchedulePath(version).equals(tableName)) {
            return expectedScheduleMap(fromId, toId);
        }

        if (lmsYtProperties.getPhonesPath(version).equals(tableName)) {
            return expectedPhonesMap(fromId, toId);
        }

        if (lmsYtProperties.getLogisticsPointsAggPath(version).equals(tableName)) {
            return expectedPointsAggMap(fromId, toId);
        }

        if (lmsYtProperties.getPartnerPath(version).equals(tableName)) {
            return expectedPartnersMap(fromId, toId);
        }

        if (lmsYtProperties.getPartnerExternalParamPath(version).equals(tableName)) {
            return expectedPartnerExternalParamsMap(fromId, toId);
        }

        if (lmsYtProperties.getPartnerRelationPath(version).equals(tableName)) {
            return expectedPartnerRelationMap(fromId, toId);
        }

        if (lmsYtProperties.getPartnerRelationToPath(version).equals(tableName)) {
            return expectedPartnerRelationToMap(fromId, toId);
        }

        if (lmsYtProperties.getInboundSchedulePath(version).equals(tableName)) {
            return expectedInboundScheduleMap(fromId, toId);
        }

        if (lmsYtProperties.getDynamicScheduleDayByIdPath(version).equals(tableName)) {
            return expectedScheduleDayByIdMap(fromId, toId);
        }

        if (lmsYtProperties.getPartnerApiSettingsPath(version).equals(tableName)) {
            return expectedPartnerApiSettings(fromId, toId);
        }

        throw new IllegalStateException("Calling method for not implemented entity map");
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPointsMap(long fromId, long toId) {
        Map<String, String> expectedPoints = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            expectedPoints.put(String.valueOf(i), OBJECT_MAPPER.writeValueAsString(buildPoint(i)));
        }
        return expectedPoints;
    }

    @Nonnull
    public static YtLogisticsPoint buildPoint(long id) {
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
    public static YtPartnerExternalParam buildExternalParams() {
        return buildExternalParams("DISABLE_AUTO_CANCEL_AFTER_SLA");
    }

    @Nonnull
    public static YtPartnerExternalParam buildExternalParams(String key) {
        return new YtPartnerExternalParam()
            .setKey(key)
            .setPartnerValues(buildPartnerValuesJson());
    }

    @Nonnull
    public static YtPartnerExternalParam buildExternalParams(String key, Long partnerId, String value) {
        return new YtPartnerExternalParam()
            .setKey(key)
            .setPartnerValues(String.format(
                "{\"partner_values\":["
                    + "{\"partner_id\":%d,"
                    + "\"value\":\"%s\"}"
                    + "]}",
                partnerId,
                value
            ));
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedScheduleMap(long fromId, long toId) {
        Map<String, String> expectedDays = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtScheduleDays schedule = buildSchedule(i);
            expectedDays.put(schedule.getId(), OBJECT_MAPPER.writeValueAsString(schedule));
        }
        return expectedDays;
    }

    @Nonnull
    public static YtScheduleDays buildSchedule(long id) {
        return new YtScheduleDays()
            .setId(id)
            .setScheduleDays(buildScheduleDaysJson(id));
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPhonesMap(long fromId, long toId) {
        Map<String, String> expectedPhones = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtPhoneByLogisticsPointId phone = buildPhone(i);
            expectedPhones.put(phone.getId(), OBJECT_MAPPER.writeValueAsString(phone));
        }
        return expectedPhones;
    }

    @Nonnull
    public static YtPhoneByLogisticsPointId buildPhone(long id) {
        return new YtPhoneByLogisticsPointId()
            .setId(id)
            .setPhones(Optional.of(buildPhonesJson(id)));
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPointsAggMap(long fromId, long toId) {
        Map<String, String> expectedPointsAgg = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtLogisticsPointsAggModel pointAgg = buildPointAgg(i);
            expectedPointsAgg.put(pointAgg.getId(), OBJECT_MAPPER.writeValueAsString(pointAgg));
        }
        return expectedPointsAgg;
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPartnersMap(long fromId, long toId) {
        Map<String, String> expectedPartnersMap = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtPartner partner = buildPartner(i);
            expectedPartnersMap.put(partner.getId(), OBJECT_MAPPER.writeValueAsString(partner));
        }
        return expectedPartnersMap;
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPartnerExternalParamsMap(long fromId, long toId) {
        Map<String, String> expectedPartnerExternalParamMap = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtPartnerExternalParam partnerExternalParam = buildPartnerExternalParam(i);
            expectedPartnerExternalParamMap.put(
                partnerExternalParam.getId(),
                OBJECT_MAPPER.writeValueAsString(partnerExternalParam)
            );
        }
        return expectedPartnerExternalParamMap;
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPartnerRelationMap(long fromId, long toId) {
        Map<String, String> expectedPartnerRelationMap = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtPartnerRelation partnerRelation = buildPartnerRelation(i);
            expectedPartnerRelationMap.put(partnerRelation.getId(), OBJECT_MAPPER.writeValueAsString(partnerRelation));
        }
        return expectedPartnerRelationMap;
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPartnerRelationToMap(long fromId, long toId) {
        Map<String, String> expectedPartnerRelationToMap = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtPartnerRelationTo partnerRelationTo = buildPartnerRelationTo(i);
            expectedPartnerRelationToMap.put(
                partnerRelationTo.getId(),
                OBJECT_MAPPER.writeValueAsString(partnerRelationTo)
            );
        }
        return expectedPartnerRelationToMap;
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedInboundScheduleMap(long fromId, long toId) {
        Map<String, String> expectedInboundScheduleMap = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtInboundSchedule inboundSchedule = buildInboundSchedule(i);
            expectedInboundScheduleMap.put(inboundSchedule.getId(), OBJECT_MAPPER.writeValueAsString(inboundSchedule));
        }
        return expectedInboundScheduleMap;
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedScheduleDayByIdMap(long fromId, long toId) {
        Map<String, String> expectedScheduleDayByIdMap = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtScheduleDayById scheduleDayById = buildScheduleDayById(i);
            expectedScheduleDayByIdMap.put(scheduleDayById.getId(), OBJECT_MAPPER.writeValueAsString(scheduleDayById));
        }
        return expectedScheduleDayByIdMap;
    }

    @Nonnull
    @SneakyThrows
    private static Map<String, String> expectedPartnerApiSettings(long fromId, long toId) {
        Map<String, String> expectedPartnerApiSettingsMap = new HashMap<>();

        for (long i = fromId; i <= toId; i++) {
            YtPartnerApiSettings partnerApiSettings = buildPartnerApiSettings(i);
            expectedPartnerApiSettingsMap.put(
                partnerApiSettings.getId(),
                OBJECT_MAPPER.writeValueAsString(partnerApiSettings)
            );
        }
        return expectedPartnerApiSettingsMap;
    }

    @Nonnull
    private static YtLogisticsPointsAggModel buildPointAgg(long id) {
        return new YtLogisticsPointsAggModel()
            .setActive(true)
            .setPartnerId(Optional.of(id))
            .setType(PointType.WAREHOUSE)
            .setLogisticsPoints(buildLogisticsPointsJson(id));
    }

    @Nonnull
    public static YtLogisticsPointsAggModel buildPointAgg(long partnerId, List<Long> pointIds) {
        return new YtLogisticsPointsAggModel()
            .setActive(true)
            .setPartnerId(Optional.of(partnerId))
            .setType(PointType.WAREHOUSE)
            .setLogisticsPoints(buildLogisticsPointsJson(pointIds));
    }

    @Nonnull
    public static YtPartner buildPartner(long id) {
        return new YtPartner()
            .setId(id)
            .setMarketId(Optional.of(id))
            .setName(Optional.of("Name"))
            .setReadableName(Optional.of("Partner name"))
            .setType(PartnerType.DELIVERY)
            .setSubtypeId(Optional.of(id))
            .setBillingClientId(Optional.of(id))
            .setDomain(Optional.of("Domain"))
            .setExternalParams(buildExternalParamsJson());
    }

    @Nonnull
    private static YtPartnerExternalParam buildPartnerExternalParam(long id) {
        return new YtPartnerExternalParam()
            .setKey(String.valueOf(id))
            .setPartnerValues(buildPartnerValuesJson());
    }

    @Nonnull
    public static YtPartnerRelation buildPartnerRelation(long id) {
        return new YtPartnerRelation()
            .setPartnerFrom(id)
            .setPartnerTo(id + 1)
            .setCutoffs(Optional.of(buildCutoffsJson()));
    }

    @Nonnull
    public static YtPartnerRelationTo buildPartnerRelationTo(long id) {
        return new YtPartnerRelationTo()
            .setPartnerFrom(id)
            .setPartnersTo(buildPartnersToJson(id));
    }

    @Nonnull
    public static YtInboundSchedule buildInboundSchedule(long id) {
        return new YtInboundSchedule()
            .setPartnerFrom(id)
            .setPartnerTo(id + 1)
            .setDeliveryType(Optional.of("COURIER"))
            .setSchedules(Optional.of(buildInboundSchedulesJson()));
    }

    @Nonnull
    public static YtScheduleDayById buildScheduleDayById(long id) {
        return new YtScheduleDayById()
            .setId(id)
            .setTimeFrom("11:22:33.000000")
            .setTimeTo("22:33:11.000000");
    }

    @Nonnull
    public static YtPartnerApiSettings buildPartnerApiSettings(long id) {
        return new YtPartnerApiSettings()
            .setPartnerId(id)
            .setMethod("saveTheWorld")
            .setActive(true);
    }

    @Nonnull
    private static String buildLogisticsPointsJson(long id) {
        return "{\"logistics_points\":["
            + (id + 1) + "," + (id + 2) + "," + (id + 3)
            + "]}";
    }

    @Nonnull
    private static String buildLogisticsPointsJson(List<Long> ids) {
        return "{\"logistics_points\":["
            + ids.stream().map(String::valueOf).collect(Collectors.joining(","))
            + "]}";
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
            + "},"
            + "{\"day\":" + (id + 1) + ","
            + "\"id\":" + (37465940 + id + 1) + ","
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
    private static String buildExternalParamsJson() {
        return "{\"external_params\":["
            + "{\"key\":\"IS_COMMON\",\"value\":\"1\"},"
            + "{\"key\":\"IS_GLOBAL\",\"value\":\"1\"},"
            + "{\"key\":\"AVAILABLE_TO_ALL\",\"value\":\"0\"}"
            + "]}";
    }

    @Nonnull
    private static String buildPartnerValuesJson() {
        return "{\"partner_values\":["
            + "{\"partner_id\":1,\"value\":\"1\"},"
            + "{\"partner_id\":2,\"value\":\"0\"}"
            + "]}";
    }

    @Nonnull
    private static String buildCutoffsJson() {
        return "{\"cutoffs\":["
            + "{\"cutoff_time\":\"16:59:59.000000\",\"location_id\":1},"
            + "{\"cutoff_time\":\"17:59:59.000000\",\"location_id\":2}"
            + "]}";
    }

    @Nonnull
    private static String buildPartnersToJson(long idFrom) {
        return "{\"relations\":["
            + "{\"partner_to\":" + (idFrom + 1) + ",\"return_partner\":" + (idFrom + 1) + "},"
            + "{\"partner_to\":" + (idFrom + 2) + ",\"return_partner\":" + idFrom + "}"
            + "]}";
    }

    @Nonnull
    private static String buildInboundSchedulesJson() {
        return "{\"schedules\":[1,3]}";
    }

    @Nonnull
    private static Map<String, ?> buildLogisticsPointAggMap(long id) {
        return Map.ofEntries(
            Map.entry("active", true),
            Map.entry("partner_id", id),
            Map.entry("type", PointType.WAREHOUSE.toString()),
            Map.entry("logistics_points", buildLogisticsPointsJson(id))
        );
    }

    @Nonnull
    private static Map<String, ?> buildPartnerMap(long id) {
        return Map.ofEntries(
            Map.entry("id", id),
            Map.entry("market_id", id),
            Map.entry("name", "Name"),
            Map.entry("readable_name", "Partner name"),
            Map.entry("type", PartnerType.DELIVERY.name()),
            Map.entry("subtype_id", id),
            Map.entry("billing_client_id", id),
            Map.entry("domain", "Domain"),
            Map.entry("external_params", buildExternalParamsJson())
        );
    }

    @Nonnull
    private static Map<String, ?> buildPartnerExternalParamsMap(long id) {
        return Map.ofEntries(
            Map.entry("key", String.valueOf(id)),
            Map.entry("partner_values", buildPartnerValuesJson())
        );
    }

    @Nonnull
    private static Map<String, ?> buildPartnerRelationMap(long id) {
        return Map.ofEntries(
            Map.entry("partner_from", id),
            Map.entry("partner_to", id + 1),
            Map.entry("cutoffs", buildCutoffsJson())
        );
    }

    @Nonnull
    private static Map<String, ?> buildPartnerRelationToMap(long id) {
        return Map.ofEntries(
            Map.entry("partner_from", id),
            Map.entry("partners_to", buildPartnersToJson(id))
        );
    }

    @Nonnull
    private static Map<String, ?> buildInboundScheduleMap(long id) {
        return Map.ofEntries(
            Map.entry("delivery_type", "COURIER"),
            Map.entry("partner_from", id),
            Map.entry("partner_to", id + 1),
            Map.entry("schedules", buildInboundSchedulesJson())
        );
    }

    @Nonnull
    private static Map<String, ?> buildScheduleDayByIdMap(long id) {
        return Map.ofEntries(
            Map.entry("id", id),
            Map.entry("time_from", "11:22:33.000000"),
            Map.entry("time_to", "22:33:11.000000")
        );
    }

    @Nonnull
    private static Map<String, ?> buildPartnerApiSettingsMap(long id) {
        return Map.ofEntries(
            Map.entry("partner_id", id),
            Map.entry("method", "saveTheWorld"),
            Map.entry("active", true)
        );
    }
}
