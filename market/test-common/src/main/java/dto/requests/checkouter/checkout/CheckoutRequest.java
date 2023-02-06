package dto.requests.checkouter.checkout;

import java.util.List;

import dto.requests.checkouter.cart.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

@Data
@AllArgsConstructor
public class CheckoutRequest {

    private Long buyerRegionId;
    private Boolean isBooked;
    private String buyerCurrency;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private List<CartItem> orders;
    private Buyer buyer;

}
