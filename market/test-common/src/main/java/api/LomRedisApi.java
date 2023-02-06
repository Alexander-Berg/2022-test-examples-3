package api;

import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

public interface LomRedisApi {

    @GET("/lms/test-redis/logistics-point/get/{id}")
    Call<LogisticsPointResponse> getLogisticsPointFromRedis(@Path("id") Long id);

    @POST("/lms/test-redis/logistics-point/get-by-filter")
    Call<List<LogisticsPointResponse>> getLogisticsPointsFromRedis(@Body LogisticsPointFilter filter);

    @GET("/lms/test-redis/partner/get/{id}")
    Call<PartnerResponse> getPartnerFromRedis(@Path("id") Long id);

    @POST("/lms/test-redis/partner/get-by-ids")
    Call<List<PartnerResponse>> getPartnersFromRedis(@Body Set<Long> ids);

    @POST("/lms/test-redis/inbound-schedule/get-by-filter")
    Call<List<ScheduleDayResponse>> searchInboundSchedule(@Body LogisticSegmentInboundScheduleFilter filter);

    @POST("/lms/test-redis/partner-relation/cutoffs/get-by-filter")
    Call<List<PartnerRelationEntityDto>> searchPartnerRelationWithCutoffsFromRedis(@Body PartnerRelationFilter filter);

    @POST("/lms/test-redis/partner-relation/return-partners/get-by-filter")
    Call<List<PartnerRelationEntityDto>> searchPartnerRelationWithReturnPartnersFromRedis(
        @Body PartnerRelationFilter filter
    );

    @POST("/lms/test-redis/partner/params")
    Call<List<PartnerExternalParamGroup>> getPartnerExternalParamValues(
        @Body Set<PartnerExternalParamType> paramTypes
    );

    @POST("/lms/test-redis/partner-settings-methods/get-by-filter")
    Call<List<SettingsMethodDto>> searchPartnerSettingsMethods(@Body SettingsMethodFilter filter);

    @GET("/lms/test-redis/actual-version")
    Call<String> getActualDataVersion();
}
