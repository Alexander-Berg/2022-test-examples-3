package api;

import java.time.LocalDate;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;
import ru.yandex.market.tsup.controller.dto.NewPipelineDto;
import ru.yandex.market.tsup.service.data_provider.entity.route.dto.RouteShortcutsResponse;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleListDto;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.RunWithAddress;
import ru.yandex.market.tsup.service.data_provider.entity.run.dto.RunsWithAddresses;

public interface TsupApi {

    @Headers(
        {
            "x-user-login: autotests",
            "action: get"
        }
    )
    @GET("/runs")
    Call<RunsWithAddresses> getRuns(
        @Query("fromLogisticPointId") Long fromLogisticPointId,
        @Query("toLogisticPointId") Long toLogisticPointId,
        @Query("startDateFrom") LocalDate startDateFrom,
        @Query("startDateTo") LocalDate startDateTo,
        @Query("transportationType") TransportationType transportationType,
        @Query("pageSize") int pageSize
    );

    @Headers(
        {
            "x-user-login: autotests",
            "action: get"
        }
    )
    @GET("/runs/{runId}")
    Call<RunWithAddress> getRunById(
        @Path("runId") long runId
    );

    @Headers(
        {
            "x-user-login: autotests",
            "action: post"
        }
    )
    @POST("/pipelines")
    Call<Long> createPipeline(
        @Body NewPipelineDto dto
    );

    @Headers(
        {
            "x-user-login: autotests",
            "action: delete"
        }
    )
    @DELETE("/routes/{id}")
    Call<ResponseBody> deleteRoute(
        @Path("id") long id
    );

    @Headers(
        {
            "x-user-login: autotests",
            "action: get"
        }
    )
    @GET("/routes")
    Call<RouteShortcutsResponse> getRoutes(
        @Query("startPartnerId") Long startPartnerId,
        @Query("endPartnerId") Long endPartnerId
    );

    @Headers(
        {
            "x-user-login: autotests",
            "action: get"
        }
    )
    @GET("/routes/schedule/byRoute/{routeId}")
    Call<RouteScheduleListDto> searchSchedulesByRouteId(
        @Path("routeId") long routeId
    );

    @Headers(
        {
            "x-user-login: autotests",
            "action: post"
        }
    )
    @POST("/routes/schedule/{scheduleId}/turnOff")
    Call<ResponseBody> turnOffScheduled(
        @Path("scheduleId") long scheduleId,
        @Query("lastDate") String lastDate
    );

}
