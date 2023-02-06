package api;

import delivery.client.lrm.client.model.CreateReturnRequest;
import delivery.client.lrm.client.model.CreateReturnResponse;
import delivery.client.lrm.client.model.SearchReturnsRequest;
import delivery.client.lrm.client.model.SearchReturnsResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface LrmApi {
    @POST("/returns")
    Call<CreateReturnResponse> returns(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body CreateReturnRequest request
    );

    @POST("/returns/{returnId}/commit")
    Call<ResponseBody> commitReturns(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Path("returnId") Long returnId
    );

    @PUT("/returns/search")
    Call<SearchReturnsResponse> searchReturns(
        @Header("X-Ya-Service-Ticket") String tvmTicket,
        @Body SearchReturnsRequest request
    );
}
