package api;

import java.util.Set;

import dto.requests.lms.ActionDto;
import dto.requests.lms.CargoTypeDto;
import dto.requests.lms.CreateScheduleDayRequest;
import dto.requests.lms.HolidayNewDto;
import dto.requests.lms.LogisticPointIdsRequest;
import dto.responses.lms.LogisticSegmentDto;
import dto.responses.lms.PartnerCapacityDto;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

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
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

public interface LmsApi {

    @GET("/externalApi/partner-capacities")
    Call<ListWrapper<PartnerCapacityDto>> getPartnerCapacities();

    @GET("/externalApi/partners/{partnerId}/capacity")
    Call<ListWrapper<PartnerCapacityDto>> getPartnerCapacities(
        @Path("partnerId") Long partnerId
    );

    @POST("/externalApi/partner-capacities")
    Call<PartnerCapacityDto> createPartnerCapacity(
        @Body PartnerCapacityDto partnerCapacityDto
    );

    @DELETE("/qa/lms/partner-capacity/{id}")
    Call<ResponseBody> deletePartnerCapacity(
        @Path("id") Long id
    );

    @PUT("/externalApi/partnerRelation/switchOn")
    Call<ResponseBody> switchOnPartnerRelation(
        @Query("from") Integer partnerFromId,
        @Query("to") Integer partnerToId
    );

    @PUT("/externalApi/partnerRelation/switchOff")
    Call<ResponseBody> switchOffPartnerRelation(
        @Query("from") Integer partnerFromId,
        @Query("to") Integer partnerToId
    );

    @GET("qa/lms/schedule")
    Call<Long> getScheduleDay(
        @Query("weekDay") Integer weekDay,
        @Query("scheduleId") Long scheduleId
    );

    @GET("externalApi/schedule/{scheduleDayId}")
    Call<ScheduleDayResponse> getScheduleDayById(@Path("scheduleDayId") Long id);

    @POST("qa/lms/schedule")
    Call<ResponseBody> createScheduleDay(
        @Body CreateScheduleDayRequest createScheduleDayRequest
    );

    @POST("/qa/lms/schedule/delete/{scheduleDayId}")
    Call<ResponseBody> deleteScheduleDay(
        @Path("scheduleDayId") Long scheduleDayId
    );

    @POST("/qa/lms/logistics-point/activate")
    Call<ResponseBody> activateLogisticsPoint(
        @Body LogisticPointIdsRequest logisticPointIdsRequest
    );

    @POST("/qa/lms/logistics-point/deactivate")
    Call<ResponseBody> deactivateLogisticsPoint(
        @Body LogisticPointIdsRequest logisticPointIdsRequest
    );

    @POST("/qa/lms/logistics-point/freeze")
    Call<ResponseBody> freezeLogisticsPoint(
        @Body LogisticPointIdsRequest logisticPointIdsRequest
    );

    @POST("/qa/lms/logistics-point/unfreeze")
    Call<ResponseBody> unfreezeLogisticsPoint(
        @Body LogisticPointIdsRequest logisticPointIdsRequest
    );

    @POST("/qa/lms/logistic-services/logistics-point-holiday")
    Call<Void> createHolidaysForLogisticsPoint(
        @Query("logisticsPointId") Long logisticsPointId,
        @Body HolidayNewDto logisticPointIdsRequest
    );

    @GET("/qa/lms/logistic-services/logistics-point-holiday")
    Call<ResponseBody> getHolidaysForLogisticsPoint(@Query("logisticsPointId") Long logisticsPointId);

    @POST("/qa/lms/logistic-services/logistics-point-holiday/delete")
    Call<Void> deleteHolidaysForLogisticsPoint(
        @Query("logisticsPointId") Long parentId,
        @Body ActionDto dto
    );

    @GET("/externalApi/warehouse-handling-duration/{partnerId}")
    Call<ResponseBody> getWarehouseHandlingDuration(
        @Path("partnerId") Integer partnerId
    );

    @PATCH("/externalApi/warehouse-handling-duration/{partnerId}")
    Call<ResponseBody> changeWarehouseHandlingDuration(
        @Path("partnerId") Integer partnerId,
        @Body String duration
    );

    @POST("qa/lms/logistic-services/{serviceId}/cargo-type")
    Call<ResponseBody> createLogisticServiceCargoType(@Path("serviceId") Long serviceId, @Body CargoTypeDto newDto);

    @POST("qa/lms/logistic-services/{serviceId}/cargo-type/allow")
    Call<ResponseBody> removeLogisticServiceCargoTypes(@Path("serviceId") Long serviceId, @Body ActionDto actionDto);

    @PUT("/externalApi/logistic-segments/search")
    Call<ListWrapper<LogisticSegmentDto>> searchLogisticSegments(@Body LogisticSegmentFilter filter);

    @POST("/logistic-segments/cargo-type/update")
    Call<ResponseBody> updateCargoTypesFromLmsToYt();

    @GET("/externalApi/logisticsPoints/{id}")
    Call<LogisticsPointResponse> getLogisticsPoint(@Path("id") Long id);

    @PUT("/externalApi/logisticsPoints")
    Call<ListWrapper<LogisticsPointResponse>> getLogisticsPoints(@Body LogisticsPointFilter filter);

    @GET("/externalApi/partners/{id}")
    Call<PartnerResponse> getPartner(@Path("id") Long id);

    @PUT("/externalApi/partners/search")
    Call<ListWrapper<PartnerResponse>> searchPartners(@Body SearchPartnerFilter filter);

    @PUT("/logistic-segments/search/schedule/inbound")
    Call<ListWrapper<ScheduleDayResponse>> getInboundSchedule(@Body LogisticSegmentInboundScheduleFilter filter);

    @PUT("/externalApi/partner-relation/search")
    Call<ListWrapper<PartnerRelationEntityDto>> searchPartnerRelation(@Body PartnerRelationFilter filter);

    @GET("/externalApi/partner/externalParam")
    Call<ListWrapper<PartnerExternalParamGroup>> getPartnerExternalParams(
        @Query("paramTypes") Set<PartnerExternalParamType> paramTypes
    );

    @PUT("/partners/settings/methods/search")
    Call<ListWrapper<SettingsMethodDto>> searchPartnerSettingsMethods(@Body SettingsMethodFilter filter);

    @POST("/externalApi/logistic-segments/warehouse")
    Call<LogisticSegmentDto> createWarehouseSegment(@Body CreateWarehouseSegmentRequest request);
}
