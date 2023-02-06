package api;

import java.math.BigDecimal;

import dto.responses.combinator.Ycombo;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CombinatorApi {
    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET("/ycombo/debug")
    Call<Ycombo> getPaths(
        @Query("warehouse") Long warehouse,
        @Query("region") Integer region,
        @Query("weight") Integer weight,
        @Query("dimensions") String dimensions,
        @Query("lat") BigDecimal lat,
        @Query("lon") BigDecimal lon,
        @Query("rearr") String rearr,
        @Query("format") String format
    );
}
