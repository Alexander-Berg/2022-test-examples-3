package api;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface DsFfMockApi {

    @POST
    @Headers({"Content-Type: application/xml; charset=utf-8"})
    Call<ResponseBody> mockRequest(
        @Url String path,
        @Body RequestBody body
    );

    @DELETE
    @Headers({"Content-Type: application/xml; charset=utf-8"})
    Call<ResponseBody> deletedMock(
            @Url String path
    );
}
