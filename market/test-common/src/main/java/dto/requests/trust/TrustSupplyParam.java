package dto.requests.trust;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrustSupplyParam {
    private String token;
    @JsonProperty("purchase_token")
    private String purchaseToken;
    @JsonProperty("payment_method")
    private String paymentMethod;
    @JsonProperty("card_number")
    private Long cardNumber;
    @JsonProperty("expiration_month")
    private Integer expirationMonth;
    @JsonProperty("expiration_year")
    private Integer expirationYear;
    private String cardholder;
    private Integer cvn;

}
