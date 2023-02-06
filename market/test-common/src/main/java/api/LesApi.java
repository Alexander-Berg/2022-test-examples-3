package api;

import dto.requests.les.AddEventDto;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LesApi {

    @POST("/events/add")
    Call<ResponseBody> addEvent(@Body AddEventDto addEventDto);
}
