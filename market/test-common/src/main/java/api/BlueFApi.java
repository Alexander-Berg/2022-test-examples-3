package api;

import dto.responses.bluefapi.ResolveLink;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BlueFApi {

    @Headers(
        {
            "Authorization: OAuth AgAAAADu4Rz0AAARr5AYNyijKkFYltWqBUOHEcs",
            "Content-Type: application/json"
        }
    )
    @POST("/api/v1")
    Call<ResolveLink> resolveOnDemandLink(
        @Query("name") String name,
        @Body Object resolveLinkBody
    );
}
