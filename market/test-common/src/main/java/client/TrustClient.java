package client;

import api.TrustApi;
import dto.requests.trust.TrustSupplyDataRequest;
import dto.requests.trust.TrustSupplyParam;
import dto.responses.trust.TrustResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath("delivery/trust.properties")
@Slf4j
public class TrustClient {
    private final TrustApi trustApi;

    @Property("trust.host")
    private String host;
    @Property("trust.token")
    private String token;
    @Property("trust.paymentMethod")
    private String paymentMethod;
    @Property("trust.cardNumber")
    private Long cardNumber;
    @Property("trust.expirationMonth")
    private Integer expirationMonth;
    @Property("trust.expirationYear")
    private Integer expirationYear;
    @Property("trust.cardholder")
    private String cardholder;
    @Property("trust.cvn")
    private Integer cvn;

    public TrustClient() {
        PropertyLoader.newInstance().populate(this);
        trustApi = RETROFIT.getRetrofit(host).create(TrustApi.class);
    }

    @SneakyThrows
    public void supplyPaymentData(String purchaseToken) {
        log.debug("Supplying payment data...");
        Response<TrustResponse> bodyResponse = trustApi
            .supplyPaymentData(new TrustSupplyDataRequest(
                new TrustSupplyParam(
                    token,
                    purchaseToken,
                    paymentMethod,
                    cardNumber,
                    expirationMonth,
                    expirationYear,
                    cardholder,
                    cvn
                )))
            .execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Не удалось оплатить заказ");
        Assertions.assertNotNull(bodyResponse.body(), "Пустой ответ на оплату заказа");
        Assertions.assertEquals("success", bodyResponse.body().getStatus(), "Не удалось оплатить заказ");
    }
}
