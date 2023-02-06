package dto.requests.checkouter;

import dto.requests.checkouter.checkout.AddressRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDtoRequest {

    private DeliveryDates dates;
    private Long regionId;
    private String hash;
    private AddressRequest address;
    private Long outletId;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeliveryDates {
        private String fromDate;
        private String toDate;
        private String fromTime;
        private String toTime;
    }

}
