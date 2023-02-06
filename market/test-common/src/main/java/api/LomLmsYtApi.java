package api;

import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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

public interface LomLmsYtApi {

    @GET("/lms/test-yt/logistics-point/get/{id}")
    Call<LogisticsPointResponse> getLogisticsPoint(@Path("id") Long id);

    @POST("/lms/test-yt/logistics-point/get-by-filter")
    Call<List<LogisticsPointResponse>> getLogisticsPoints(@Body LogisticsPointFilter filter);

    @GET("/lms/test-yt/schedule/{id}")
    Call<ScheduleDayResponse> getScheduleDayById(@Path("id") Long id);

    @POST("/lms/test-yt/partner/params")
    Call<List<PartnerExternalParamGroup>> getPartnerExternalParamValues(
        @Body Set<PartnerExternalParamType> paramTypes
    );

    @GET("/lms/test-yt/partner/get/{id}")
    Call<PartnerResponse> getPartner(@Path("id") Long id);

    @PUT("/lms/test-yt/partner/get-by-ids")
    Call<List<PartnerResponse>> getPartners(@Body Set<Long> ids);

    @POST("/lms/test-yt/partner-settings-methods/get-by-filter")
    Call<List<SettingsMethodDto>> searchPartnerSettingsMethods(@Body SettingsMethodFilter filter);

    @POST("/lms/test-yt/inbound-schedule/get-by-filter")
    Call<List<ScheduleDayResponse>> searchInboundSchedule(@Body LogisticSegmentInboundScheduleFilter filter);

    @POST("/lms/test-yt/partner-relation/cutoffs/get-by-filter")
    Call<List<PartnerRelationEntityDto>> searchPartnerRelationWithCutoffs(@Body PartnerRelationFilter filter);

    @POST("/lms/test-yt/partner-relation/return-partners/get-by-filter")
    Call<List<PartnerRelationEntityDto>> searchPartnerRelationWithReturnPartners(
        @Body PartnerRelationFilter filter
    );

    @GET("/lms/test-yt/actual-version")
    Call<String> getActualDataVersion();
}
