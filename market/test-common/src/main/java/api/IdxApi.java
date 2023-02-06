package api;

import dto.responses.idxapi.OtraceResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IdxApi {

    @GET("v1/otrace")
    Call<OtraceResponse> otrace(
        @Query("waremd5") String waremd5
    );
}
