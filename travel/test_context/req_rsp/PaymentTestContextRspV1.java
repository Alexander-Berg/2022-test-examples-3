package ru.yandex.travel.api.endpoints.test_context.req_rsp;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaymentTestContextRspV1 {
    private String token;
    private String paymentTestContextToken;
}
