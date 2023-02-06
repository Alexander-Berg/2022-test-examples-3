package dto.requests.checkouter.checkout;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddressRequest {
    private String country;
    @JsonProperty("postcode")
    private String postcode;
    private String city;
    private String subway;
    private String street;
    private String house;
    private String block;
    private String entrance;
    private String entryphone;
    private String floor;
    private String apartment;
    private String recipient;
    private String phone;
}
