package dto.responses.lgw.message.get_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GetCourierResponse {

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("orderId")
    private OrderId orderId;

    @JsonProperty("courier")
    private Courier courier;

    //Не уверена, что это поле тут по структуре. Пихнула вручную, пока нет примера.
    @JsonProperty("electronicAcceptanceCertificate")
    private ElectronicAcceptanceCertificate electronicAcceptanceCertificate;

}
