package api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ReporterApi {

    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET("/yandsearch")
    Call<ResponseBody> offerInfo(
        @Query("ip") String ip,
        @Query("place") String place,
        @Query("feed_shoffer_id") String feedShofferId,
        @Query("fesh") Long fesh,
        @Query("rids") Integer rids,
        @Query("regset") Integer regset,
        @Query("pp") Integer pp,
        @Query("show-booking-outlets") Integer showBookingOutlets,
        @Query("adult") Integer adult,
        @Query("numdoc") Integer numdoc,
        @Query("show-model-card-params") Integer showModelCardParams,
        @Query("showdiscounts") Integer showDiscounts,
        @Query("cpa-category-filter") Integer cpaCategoryFilter,
        @Query("strip_query_language") Integer stripQueryLanguage,
        @Query("show-promoted") Integer showPromoted,
        @Query("show-min-quantity") Integer showMinQuantity,
        @Query("show-urls") String showUrls,
        @Query("client") String client,
        @Query("co-from") String coFrom,
        @Query("show-filter-mark") String showFilterMark,
        @Query("show-preorder") Integer showPreorder,
        @Query("rgb") String rgb,
        @Query("rearr-factors") String rearrFactors,
        @Query("use-virt-shop") Integer useVirtualShop,
        @Query("available-for-business") Integer availableForBusiness
    );

    @GET("/yandsearch")
    Call<ResponseBody> actualDelivery(
        @Query("place") String place,
        @Query("rids") Long rids,
        @Query("offers-list") String offersList,
        @Query("rearr-factors") String rearrFactors
    );

    @GET("/yandsearch")
    Call<ResponseBody> deliveryRoute(
        @Query("place") String place,
        @Query("rids") Long rids,
        @Query("offers-list") String offersList,
        @Query("point_id") Long pointId,
        @Query("delivery-interval") String interval,
        @Query("delivery-type") String deliveryType,
        @Query("rearr-factors") String rearrFactors
    );
}
