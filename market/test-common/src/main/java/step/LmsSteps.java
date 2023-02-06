package step;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import client.LMSClient;
import dto.requests.lms.ActionDto;
import dto.requests.lms.HolidayDto;
import dto.requests.lms.HolidayNewDto;
import dto.responses.lms.LogisticSegmentDto;
import dto.responses.lms.PartnerCapacityDto;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import toolkit.Retrier;

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

@Slf4j
public class LmsSteps {

    private static final LMSClient LMS = new LMSClient();

    @Step("Получаем schedule_day по id расписания и дню недели")
    public Long getScheduleDay(int weekDay, Long scheduleId) {
        log.debug("get schedule day by weekday and schedule_id...");
        return Retrier.clientRetry(() -> LMS.getScheduleDay(weekDay, scheduleId));
    }

    @Step("Получаем schedule_day по идентификатору")
    public ScheduleDayResponse getScheduleDayById(Long scheduleDayId) {
        log.debug("get schedule day by id...");
        return Retrier.clientRetry(() -> LMS.getScheduleDayById(scheduleDayId));
    }

    @Step("Добавляем день в расписание")
    public void createScheduleDay(
        String scheduleType,
        Long partnerRelationId,
        int weekDay,
        String timeInterval
    ) {
        log.debug("create schedule day...");
        LMS.createScheduleDay(scheduleType, partnerRelationId, weekDay, timeInterval);
    }

    @Step("Удаляем schedule_day")
    public void deleteScheduleDay(Long scheduleDayId) {
        log.debug("delete schedule day...");
        LMS.deleteScheduleDay(scheduleDayId);
    }

    @Step("Aктивируем ПВЗ")
    public void activateLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("activate logistics point...");
        LMS.activateLogisticsPoint(logisticsPointIds);
    }

    @Step("Деактивируем ПВЗ")
    public void deactivateLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("deactivate logistics point...");
        LMS.deactivateLogisticsPoint(logisticsPointIds);
    }

    @Step("Фризим ПВЗ")
    public void freezeLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("freeze logistics point...");
        LMS.freezeLogisticsPoint(logisticsPointIds);
    }

    @Step("Анфризим ПВЗ")
    public void unfreezeLogisticsPoint(List<Long> logisticsPointIds) {
        log.debug("unfreeze logistics point...");
        LMS.unfreezeLogisticsPoint(logisticsPointIds);
    }

    @Step("Получаем из LMS список capacity партнера")
    public List<PartnerCapacityDto> getCapacityFromLms(Long partnerId) {
        return LMS.getPartnerCapacities(partnerId);
    }

    @Step("Удаляем капасити в LMS")
    public void deleteCapacityInLms(Long id) {
        LMS.deletePartnerCapacity(id);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Step("Получаем из LMS строку с капасити с нужным регионом и значением")
    public PartnerCapacityDto getCapacityFromLms(
        CapacityService capacityService,
        CountingType countingType,
        Integer locationTo,
        Integer locationFrom,
        Long partnerId,
        DeliveryType deliveryType,
        Long value,
        LocalDate day
    ) {
        List<PartnerCapacityDto> partnerCapacities = LMS.getPartnerCapacities(partnerId);
        return partnerCapacities.stream().filter(
                partnerCapacityDto ->
                    partnerCapacityDto.getCapacityService().equals(capacityService) &&
                        partnerCapacityDto.getCountingType().equals(countingType) &&
                        partnerCapacityDto.getLocationFrom().equals(locationFrom) &&
                        partnerCapacityDto.getLocationTo().equals(locationTo) &&
                        partnerCapacityDto.getPartnerId().equals(partnerId) &&
                        (partnerCapacityDto.getDeliveryType() == null ||
                            partnerCapacityDto.getDeliveryType().equals(deliveryType)) &&
                        partnerCapacityDto.getValue().equals(value) &&
                        (partnerCapacityDto.getDay() == null || partnerCapacityDto.getDay().equals(day))

            )
            .findAny()
            .orElse(null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Step("Добавляем новую настройку капасити в LMS на определенную дату")
    public PartnerCapacityDto createCapacityInLms(
        CapacityService capacityService,
        CountingType countingType,
        Integer locationTo,
        Integer locationFrom,
        Long partnerId,
        DeliveryType deliveryType,
        Long value,
        LocalDate day
    ) {
        log.debug(
            "Create capacity in LMS, capacityService: {}, countingType: {}," +
                " capacityRegion: {}, partnerId: {}, value: {}, day: {}",
            capacityService,
            countingType,
            locationTo,
            partnerId,
            value,
            day
        );
        return LMS.createPartnerCapacity(
            capacityService,
            countingType,
            locationTo,
            locationFrom,
            partnerId,
            deliveryType,
            value,
            day,
            CapacityType.REGULAR
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Step("Проверяем, что в LMS присутствует нужная нам настрйка капасити, и если её нет - создаём")
    public PartnerCapacityDto createCapacityInLmsIfItIsNotAvailable(
        CapacityService capacityService,
        CountingType countingType,
        Integer locationTo,
        Integer locationFrom,
        Long partnerId,
        DeliveryType deliveryType,
        Long value,
        LocalDate day
    ) {
        PartnerCapacityDto capacityFromLms = getCapacityFromLms(
            capacityService,
            countingType,
            locationTo,
            locationFrom,
            partnerId,
            deliveryType,
            value,
            day
        );
        if (capacityFromLms == null) {
            return createCapacityInLms(
                capacityService,
                countingType,
                locationTo,
                locationFrom,
                partnerId,
                deliveryType,
                value,
                day
            );
        }
        return capacityFromLms;
    }

    @Step("Изменяем срок обработки на сегменте склада")
    public void changeWarehouseHandlingDuration(Integer partnerId, String duration) {
        log.debug("changing warehouse handling duration...");
        LMS.changeWarehouseHandlingDuration(partnerId, duration);
    }

    @Step("Добавить карго-тип в черный список сервиса")
    public void denyCargoTypeForService(Long serviceId, Integer cargoType) {
        log.debug("Deny cargo type");
        LMS.createLogisticServiceCargoType(serviceId, cargoType);
    }

    @Step("Удалить карго-типы из черного списка сервиса")
    public void allowCargoTypesForService(Long serviceId, Set<Long> cargoTypeIds) {
        log.debug("Allow cargo types");
        LMS.removeLogisticServiceCargoTypes(serviceId, cargoTypeIds);
    }

    @Step("Найти логистические сегменты, удовлетворяющие фильтру")
    public List<LogisticSegmentDto> searchLogisticSegments(LogisticSegmentFilter filter) {
        log.debug("Search logistic segments");
        return LMS.searchLogisticSegments(filter);
    }

    @Step("Обновить карго-типы из графа в YT")
    public void updateCargoTypesFromLmsToYt() {
        LMS.updateCargoTypesFromLmsToYt();
    }

    @Step("Получить логистическую точку по id из LMS")
    public LogisticsPointResponse getLogisticsPoint(Long id) {
        return LMS.getLogisticsPoint(id);
    }

    @Step("Получить логистические точки по фильтру из LMS")
    public List<LogisticsPointResponse> getLogisticsPoints(LogisticsPointFilter filter) {
        return LMS.getLogisticsPoints(filter).unwrap();
    }

    @Step("Получить партнера по id из LMS")
    public PartnerResponse getPartner(Long id) {
        return LMS.getPartner(id);
    }

    @Step("Создать/обновить складской сегмент лог. точки")
    public void createOrUpdateWarehouseSegmentForLogisticsPoint(
        Long logisticsPoint,
        @Nullable Long returnLogisticsPoint
    ) {
        //Обновляет существующий сегмент, либо создает новый(если сегмента не существует)
        LMS.createWarehouseSegment(
            CreateWarehouseSegmentRequest.builder()
                .logisticPointId(logisticsPoint)
                .returnWarehousePartnerId(returnLogisticsPoint)
                .build()
        );
    }

    @Step("Получить партнеров по фильтру из LMS")
    public List<PartnerResponse> searchPartners(SearchPartnerFilter filter) {
        return LMS.searchPartners(filter).unwrap();
    }

    @Step("Получить расписания заборов по фильтру из LMS")
    public ListWrapper<ScheduleDayResponse> getInboundSchedule(LogisticSegmentInboundScheduleFilter filter) {
        return LMS.getInboundSchedule(filter);
    }

    @Step("Найти связки партнеров по фильтру из LMS")
    public ListWrapper<PartnerRelationEntityDto> searchPartnerRelation(PartnerRelationFilter filter) {
        return LMS.searchPartnerRelation(filter);
    }

    @Step("Найти параметры партнеров по типу из LMS")
    public ListWrapper<PartnerExternalParamGroup> getPartnerExternalParam(Set<PartnerExternalParamType> paramTypes) {
        return LMS.getPartnerExternalParam(paramTypes);
    }

    @Step("Найти методы партнеров по фильтру из LMS")
    public ListWrapper<SettingsMethodDto> searchPartnerSettingsMethods(SettingsMethodFilter filter) {
        return LMS.searchPartnerSettingsMethods(filter);
    }

    @Step("Создать выходные дни для лог. точки")
    public void createDayoffsForLogisticsPoint(Long logisticPointId, List<HolidayNewDto> holidayNewDtos) {
        holidayNewDtos.forEach(holidayNewDto -> createHolidaysForLogisticsPoint(
            logisticPointId,
            holidayNewDto
        ));
    }

    @Step("Создать выходной для лог. точки ")
    public void createHolidaysForLogisticsPoint(long logisticsPointId, HolidayNewDto holidayNewDto) {
        LMS.createHolidaysForLogisticsPoint(logisticsPointId, holidayNewDto);
    }

    @Step("Получить выходные для лог. точки ")
    public List<HolidayDto> getHolidaysForLogisticsPoint(long logisticsPointId) {
        return LMS.getHolidaysForLogisticsPoint(logisticsPointId);
    }

    @Step("Удалить выходной для лог. точки ")
    public void deleteHolidaysForLogisticsPoint(long logisticsPointId, Set<Long> logisticsPointIds) {
        LMS.deleteHolidaysOfLogisticsPoint(logisticsPointId, new ActionDto(logisticsPointIds));
    }
}
