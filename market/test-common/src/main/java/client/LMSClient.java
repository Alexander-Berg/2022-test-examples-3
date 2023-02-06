package client;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import api.LmsApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import dto.requests.lms.ActionDto;
import dto.requests.lms.CargoTypeDto;
import dto.requests.lms.CreateScheduleDayRequest;
import dto.requests.lms.HolidayDto;
import dto.requests.lms.HolidayNewDto;
import dto.requests.lms.HolidayPageDto;
import dto.requests.lms.LogisticPointIdsRequest;
import dto.responses.lms.LogisticSegmentDto;
import dto.responses.lms.PartnerCapacityDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import toolkit.Mapper;

import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateWarehouseSegmentRequest;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.ListWrapper;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static toolkit.Retrofits.RETROFIT_LMS;

@Resource.Classpath("delivery/lms.properties")
@Slf4j
public class LMSClient {

    private final ObjectMapper mapper;
    private final LmsApi lmsApi;
    @Property("lms.host")
    private String host;

    public LMSClient() {
        mapper = Mapper.getDefaultMapper();
        PropertyLoader.newInstance().populate(this);
        lmsApi = RETROFIT_LMS.getRetrofit(host).create(LmsApi.class);
    }

    @SneakyThrows
    public List<PartnerCapacityDto> getPartnerCapacities() {
        log.debug("Calling LMS get partner capacities...");
        Response<ListWrapper<PartnerCapacityDto>> execute = lmsApi.getPartnerCapacities().execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не успешный запрос к partner capacities");
        Assertions.assertNotNull(execute.body(), "Пустой ответ от partner capacities");
        return execute.body().unwrap();
    }

    @SneakyThrows
    public List<PartnerCapacityDto> getPartnerCapacities(Long partnerId) {
        log.debug("Calling LMS get partner capacities...");
        Response<ListWrapper<PartnerCapacityDto>> execute = lmsApi.getPartnerCapacities(partnerId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не успешный запрос к partner capacities");
        Assertions.assertNotNull(execute.body(), "Пустой ответ от partner capacities");
        return execute.body().unwrap();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @SneakyThrows
    public PartnerCapacityDto createPartnerCapacity(
        CapacityService capacityService,
        CountingType countingType,
        Integer locationTo,
        Integer locationFrom,
        Long partnerId,
        DeliveryType deliveryType,
        Long value,
        LocalDate day,
        CapacityType type
    ) {
        log.debug("Calling LMS create partner capacity...");
        PartnerCapacityDto partnerCapacityDto = new PartnerCapacityDto();
        partnerCapacityDto.setCapacityService(capacityService);
        partnerCapacityDto.setCountingType(countingType);
        partnerCapacityDto.setDeliveryType(deliveryType);
        partnerCapacityDto.setLocationFrom(locationFrom);
        partnerCapacityDto.setLocationTo(locationTo);
        partnerCapacityDto.setPartnerId(partnerId);
        partnerCapacityDto.setPlatformClientId(1L);
        partnerCapacityDto.setType(type);
        partnerCapacityDto.setValue(value);
        partnerCapacityDto.setDay(day);
        Response<PartnerCapacityDto> execute = lmsApi.createPartnerCapacity(partnerCapacityDto).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не успешный запрос создания partner capacities");
        Assertions.assertNotNull(execute.body(), "Пустой ответ на запрос создания partner capacities");
        return execute.body();
    }

    @SneakyThrows
    public void deletePartnerCapacity(Long id) {
        log.debug("Calling LMS delete partner capacity...");
        Response<ResponseBody> execute = lmsApi.deletePartnerCapacity(id).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не успешный запрос удаления partner capacities");
    }

    @SneakyThrows
    public Long getScheduleDay(int weekDay, Long scheduleId) {
        log.debug("Calling LMS to get schedule day by weekday and schedule_id...");
        Response<Long> execute = lmsApi.getScheduleDay(weekDay, scheduleId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить запланированный день");
        return execute.body();
    }

    @Nullable
    @SneakyThrows
    public ScheduleDayResponse getScheduleDayById(Long scheduleDayId) {
        log.debug("Calling LMS to get schedule day by id...");
        Response<ScheduleDayResponse> bodyResponse = lmsApi.getScheduleDayById(scheduleDayId).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось получить запланированный день");
        return bodyResponse.body();
    }

    @SneakyThrows
    public void createScheduleDay(
        String scheduleType,
        Long partnerRelationId,
        int weekDay,
        String timeInterval
    ) {
        log.debug("Calling LMS to create schedule day...");
        Response<ResponseBody> execute = lmsApi.createScheduleDay(new CreateScheduleDayRequest(
            scheduleType, partnerRelationId, weekDay, timeInterval
        )).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось создать день в расписании");
    }

    @SneakyThrows
    public void deleteScheduleDay(Long scheduleDayId) {
        log.debug("Calling LMS to delete schedule day...");
        Response<ResponseBody> execute = lmsApi.deleteScheduleDay(scheduleDayId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось удалить день в расписании");
    }

    // Метод активации (включения) логистической точки
    @SneakyThrows
    public void activateLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("Calling LMS to activate logistics point...");
        Response<ResponseBody> execute = lmsApi.activateLogisticsPoint(new LogisticPointIdsRequest(logisticsPointIds))
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось активировать логистическую точку "
            + Joiner.on(",").join(logisticsPointIds));
    }

    // Метод деактивации логистической точки
    @SneakyThrows
    public void deactivateLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("Calling LMS to activate logistics point...");
        Response<ResponseBody> execute = lmsApi.deactivateLogisticsPoint(new LogisticPointIdsRequest(logisticsPointIds))
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось деактивировать логистическую точку "
            + Joiner.on(",").join(logisticsPointIds));
    }

    // Метод фриза логистической точки (запрет обновления по api)
    @SneakyThrows
    public void freezeLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("Calling LMS to activate logistics point...");
        Response<ResponseBody> execute = lmsApi.freezeLogisticsPoint(new LogisticPointIdsRequest(logisticsPointIds))
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось зафризить логистическую точку "
            + Joiner.on(",").join(logisticsPointIds));
    }

    // Метод анфриза логистической точки
    @SneakyThrows
    public void unfreezeLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("Calling LMS to activate logistics point...");
        Response<ResponseBody> execute = lmsApi.unfreezeLogisticsPoint(new LogisticPointIdsRequest(logisticsPointIds))
            .execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось анфризить логистическую точку "
            + Joiner.on(",").join(logisticsPointIds));
    }

    // Метод получения срока обработки на сегменте склада, работает для ДШ и КД
    @SneakyThrows
    public String getWarehouseHandlingDuration(Integer partnerId) {
        log.debug("Calling LMS to get warehouse handling duration...");
        Response<ResponseBody> execute = lmsApi.getWarehouseHandlingDuration(partnerId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось получить срок обработки на складе " + partnerId);
        Assertions.assertNotNull(execute.body(), "Пустое тело ответа");
        return execute.body().string();
    }

    // Метод изменения срока обработки на сегменте склада, работает для ДШ и КД. Принимает duration в формате "PT24H"
    @SneakyThrows
    public void changeWarehouseHandlingDuration(Integer partnerId, String duration) {
        log.debug("Calling LMS to change warehouse handling duration...");
        Response<ResponseBody> execute = lmsApi.changeWarehouseHandlingDuration(partnerId, duration).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось изменить срок обработки на складе " + partnerId);
    }

    @SneakyThrows
    public void createLogisticServiceCargoType(Long serviceId, Integer cargoType) {
        log.debug("Calling LMS to deny cargo type for service...");
        Response<ResponseBody> execute = lmsApi.createLogisticServiceCargoType(
            serviceId,
            new CargoTypeDto(cargoType)
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось добавить карго-тип в черный список сервиса");
    }

    @SneakyThrows
    public void removeLogisticServiceCargoTypes(Long serviceId, Set<Long> cargoTypeIds) {
        log.debug("Calling LMS to allow cargo types for service...");
        Response<ResponseBody> execute = lmsApi.removeLogisticServiceCargoTypes(
            serviceId,
            new ActionDto(cargoTypeIds)
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось удалить карго-типы из черного списка сервиса");
    }

    @SneakyThrows
    public List<LogisticSegmentDto> searchLogisticSegments(LogisticSegmentFilter filter) {
        log.debug("Calling LMS to search logistics segments...");
        Response<ListWrapper<LogisticSegmentDto>> execute = lmsApi.searchLogisticSegments(filter).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось выполнить запрос поиска логистических сегментов");
        Assertions.assertNotNull(execute.body(), "Пустое тело ответа");
        return execute.body().unwrap();
    }

    @SneakyThrows
    public void updateCargoTypesFromLmsToYt() {
        log.debug("Calling LMS to update cargo types in YT...");
        Response<ResponseBody> execute = lmsApi.updateCargoTypesFromLmsToYt().execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось обновить карго-типы");
    }

    @SneakyThrows
    public LogisticsPointResponse getLogisticsPoint(Long id) {
        log.debug("Calling LMS to get logistics point...");
        Response<LogisticsPointResponse> bodyResponse = lmsApi.getLogisticsPoint(id).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения логистической точки по id неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения логистической точки по id");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ListWrapper<LogisticsPointResponse> getLogisticsPoints(LogisticsPointFilter filter) {
        log.debug("Calling LMS to get logistics points...");
        Response<ListWrapper<LogisticsPointResponse>> bodyResponse = lmsApi.getLogisticsPoints(filter).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения логистических точек по фильтру неуспешен"
        );
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения логистических точек по фильтру");
        return bodyResponse.body();
    }

    @SneakyThrows
    public PartnerResponse getPartner(Long id) {
        log.debug("Calling LMS to get partner...");
        Response<PartnerResponse> bodyResponse = lmsApi.getPartner(id).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения партнера по id неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения партнера по id");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ListWrapper<PartnerResponse> searchPartners(SearchPartnerFilter filter) {
        log.debug("Calling LMS to search partners...");
        Response<ListWrapper<PartnerResponse>> bodyResponse = lmsApi.searchPartners(filter).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения партнеров по фильтру неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения партнеров по фильтру");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ListWrapper<ScheduleDayResponse> getInboundSchedule(LogisticSegmentInboundScheduleFilter filter) {
        log.debug("Calling LMS to get inbound schedule...");
        Response<ListWrapper<ScheduleDayResponse>> bodyResponse = lmsApi.getInboundSchedule(filter).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос поиска расписания заборов по фильтру неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа при поиске расписаний заборов по фильтру");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ListWrapper<PartnerRelationEntityDto> searchPartnerRelation(PartnerRelationFilter filter) {
        log.debug("Calling LMS to get partner relation...");
        Response<ListWrapper<PartnerRelationEntityDto>> bodyResponse = lmsApi.searchPartnerRelation(filter).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос поиска связок партнеров по фильтру неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа при поиске связок партнеров по фильтру");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ListWrapper<PartnerExternalParamGroup> getPartnerExternalParam(Set<PartnerExternalParamType> paramTypes) {
        log.debug("Calling LMS to get partner external params...");
        Response<ListWrapper<PartnerExternalParamGroup>> bodyResponse =
            lmsApi.getPartnerExternalParams(paramTypes).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос поиска параметров партнеров по типу неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа при поиске параметров партнеров по типу");
        return bodyResponse.body();
    }

    @SneakyThrows
    public ListWrapper<SettingsMethodDto> searchPartnerSettingsMethods(SettingsMethodFilter filter) {
        log.debug("Calling LMS to get partner settings methods...");
        Response<ListWrapper<SettingsMethodDto>> bodyResponse =
            lmsApi.searchPartnerSettingsMethods(filter).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос поиска методов партнеров по фильтру неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа при поиске методов партнеров по фильтру");
        return bodyResponse.body();
    }

    @SneakyThrows
    public LogisticSegmentDto createWarehouseSegment(CreateWarehouseSegmentRequest request) {
        log.debug("Calling LMS to create warehouse segment...");
        Response<LogisticSegmentDto> bodyResponse = lmsApi.createWarehouseSegment(request).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос создания/обновления складского сегмента неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа при создании/обновлении сегмента");
        return bodyResponse.body();
    }

    @SneakyThrows
    public void createHolidaysForLogisticsPoint(Long logisticsPointId, HolidayNewDto holidayNewDto) {
        log.debug("Calling LMS to create holidays...");
        Response<Void> bodyResponse = lmsApi.createHolidaysForLogisticsPoint(logisticsPointId, holidayNewDto).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос создания дейоффа неуспешен");
    }

    @SneakyThrows
    public List<HolidayDto> getHolidaysForLogisticsPoint(Long logisticsPointId) {
        log.debug("Calling LMS to get holidays...");
        Response<ResponseBody> bodyResponse = lmsApi.getHolidaysForLogisticsPoint(logisticsPointId).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения дейоффов неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа при получении дейоффов");
        String jsonContent = bodyResponse.body().string();
        return mapper.readValue(jsonContent, HolidayPageDto.class).getContent();
    }

    @SneakyThrows
    public void deleteHolidaysOfLogisticsPoint(Long partnerId, ActionDto actionDto) {
        log.debug("Calling LMS to delete holidays...");
        Response<Void> bodyResponse = lmsApi.deleteHolidaysForLogisticsPoint(partnerId, actionDto).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос удаления дейоффа неуспешен");
    }
}
