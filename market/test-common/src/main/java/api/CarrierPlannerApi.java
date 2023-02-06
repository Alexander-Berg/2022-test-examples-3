package api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.tpl.carrier.planner.manual.run.ManualCreateRunDto;
import ru.yandex.market.tpl.carrier.planner.manual.run.ManualRunDto;

public interface CarrierPlannerApi {

    @POST("/manual/runs")
    Call<ManualRunDto> createRun(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body ManualCreateRunDto manualCreateRunDto
    );

    @POST("/manual/runs/{id}/confirm")
    Call<ResponseBody> confirmRun(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("id") long runId
    );

    @POST("/manual/runs/{id}/assign-transport")
    Call<ResponseBody> assignTransport(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("id") long runId,
        @Query("transportId") long transportId
    );

    @POST("/manual/runs/{id}/assign-user")
    Call<ResponseBody> assignUser(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("id") long runId,
        @Query("userId") long userId
    );

}
