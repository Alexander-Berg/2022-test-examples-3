package api;

import java.time.LocalDate;
import java.util.List;

import dto.requests.tm.GetTransportationsByTagRequest;
import dto.requests.tm.RefreshTransportationRequest;
import dto.responses.tm.admin.movement.TmAdminMovementResponse;
import dto.responses.tm.admin.register_unit.RegisterUnitResponse;
import dto.responses.tm.admin.search.ItemsItem;
import dto.responses.tm.admin.search.TmAdminSearchResponse;
import dto.responses.tm.admin.status_history.TmAdminStatusHistoryResponse;
import dto.responses.tm.admin.task.TmAdminTaskResponse;
import dto.responses.tm.admin.transportation.TmTransportationResponse;
import dto.responses.tm.register.TmRegisterSearchResponse;
import dto.responses.tm.transportation_unit.TransportationUnitResponse;
import dto.responses.tm.transportations.TmTransportationsResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.dto.trip.TripShortcutDto;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;

public interface TmApi {

    @GET("/admin/transportations/search")
    Call<TmAdminSearchResponse> getTransportationForDay(
        @Query("outboundPartnerId") long outboundPartnerId,
        @Query("inboundPartnerId") long inboundPartnerId,
        @Query("planned") LocalDate planned,
        @Query("adminTransportationStatus") TransportationStatus status
    );

    @GET("/admin/transportations/search")
    Call<TmAdminSearchResponse> getTransportationById(
        @Query("id") long id
    );

    @GET("/admin/transportations/{id}")
    Call<TmTransportationResponse> getTransportation(
        @Path("id") long id
    );

    @GET("/transportations/{id}")
    Call<TmTransportationsResponse> getTransportations(
        @Path("id") long transportationId
    );

    @GET("/movement/{id}")
    Call<MovementDto> getMovement(
        @Path("id") long movementId
    );

    @GET("/admin/transportation-task/{id}")
    Call<TmAdminTaskResponse> getTransportationTask(
        @Path("id") Long transportationTaskId
    );

    @GET("/admin/transportations")
    Call<TmAdminSearchResponse> getTransportationForTask(
        @Query("taskId") Long transportationTaskId
    );

    @GET("/admin/register")
    Call<TmAdminSearchResponse> getTransportationRegister(
        @Query("id") Long transportationId
    );

    @GET("/admin/status-history/{entityType}")
    Call<TmAdminStatusHistoryResponse> getStatusHistory(
        @Path("entityType") String entityType,
        @Query("id") Long id
    );

    @GET("/admin/movement")
    Call<TmAdminMovementResponse> getTransportationMovement(
        @Query("id") Long transportationId
    );

    @GET("/admin/transportation-unit/outbound")
    Call<TmAdminMovementResponse> getTransportationOutbound(
        @Query("id") Long transportationId
    );

    @GET("/admin/transportation-unit/inbound")
    Call<TmAdminMovementResponse> getTransportationInbound(
        @Query("id") Long transportationId
    );

    @GET("/admin/register/units")
    Call<RegisterUnitResponse> getRegisterUnits(
        @Query("id") Long registerId
    );

    @GET("/transportation-unit/{id}")
    Call<TransportationUnitResponse> getTransportationUnit(
        @Path("id") Long transportationUnitId
    );

    @POST("/tms/jobs/run")
    Call<ResponseBody> refreshTransportation(
        @Body RefreshTransportationRequest refreshTransportationRequest
    );

    @POST("/transportation-task")
    Call<Long> createTransportationTask(
        @Body Object createTransportationTask
    );

    @PUT("/support/transportation/start-particular-scheduled/{transportationId}")
    Call<ResponseBody> startTransportation(
        @Path("transportationId") Long transportationId
    );

    @PUT("/register-units/search")
    Call<TmRegisterSearchResponse> getRegister(
        @Body Object register
    );

    @PUT("/transportations/search-by-tag")
    Call<List<ItemsItem>> getTransportationsByTag(
        @Body GetTransportationsByTagRequest getTransportationsByTagRequest
    );

    @GET("/qa/trip/search")
    Call<List<TripShortcutDto>> getTripSearch(
        @Query("transportationIds") Long transportationIds,
        @Query("tripId") Long tripId
    );

    @PUT("/transportations/search")
    Call<PageResult<TransportationSearchDto>> searchTransportations(
        @Body TransportationSearchFilter filter
    );

}
