package dto.requests.checkouter.cart;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

@Data
@AllArgsConstructor
public class CartRequest {

    private Long buyerRegionId;
    private Boolean isBooked;
    private String buyerCurrency;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private List<CartItem> carts;
}
