package api;

import java.util.List;

import dto.responses.tracker.TracksResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrack;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackRequest;

public interface TrackerApi {

    @POST("/track/{trackerId}/checkpoints")
    Call<ResponseBody> addCheckpoint(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("trackerId") Long trackerId,
        @Body DeliveryTrackCheckpoint addCheckpointBody
    );

    @PUT("/track")
    Call<DeliveryTrackMeta> registerTrack(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body DeliveryTrackRequest request
    );

    @GET("/track")
    Call<TracksResponse> getTracksByEntityId(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Query("entityId") String entityId,
        @Query("entityType") Integer entityType
    );

    @GET("/track/{id}")
    Call<DeliveryTrack> getTrackById(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("id") long id
    );

    @POST("/track/instant-request")
    Call<ResponseBody> instantRequest(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body List<String> body
    );
}
