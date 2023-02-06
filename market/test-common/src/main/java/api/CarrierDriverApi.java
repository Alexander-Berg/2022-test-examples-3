package api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

import ru.yandex.market.tpl.carrier.driver.api.model.shift.UserShiftDto;

public interface CarrierDriverApi {

    @GET("/api/shift")
    Call<List<UserShiftDto>> getShifts(
        @Header("Authorization") String token
    );
}
