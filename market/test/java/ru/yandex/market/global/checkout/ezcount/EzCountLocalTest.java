package ru.yandex.market.global.checkout.ezcount;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.mj.generated.client.ezcount.api.EzcountApiClient;
import ru.yandex.mj.generated.client.ezcount.model.CreateDocRequest;
import ru.yandex.mj.generated.client.ezcount.model.CreateDocRequestItem;
import ru.yandex.mj.generated.client.ezcount.model.CreateDocResponse;

import static ru.yandex.mj.generated.client.ezcount.model.CreateDocRequest.TypeEnum.TAX_INVOICE;
import static ru.yandex.mj.generated.client.ezcount.model.CreateDocRequestItem.VatTypeEnum.INC;

@Disabled
public class EzCountLocalTest extends BaseLocalTest {
    private static final CreateDocRequest NORMAL_REQUEST = new CreateDocRequest()
            .developerEmail("gm-support@yandex-team.ru")
            .apiKey("f1c85d16fc1acd369a93f0489f4615d93371632d97a9b0a197de6d4dc0da51bf")
            .type(TAX_INVOICE)
            .transactionId("99999")
            .customerName("ShwetsAV")
            .vat("17.0")
            .priceTotal("22.00")
            .dontSendEmail(true)
            .showItemsIncludingVat(true)

            .addItemItem(new CreateDocRequestItem()
                    .catalogNumber("45496452599")
                    .details("Nintendo Switch")
                    .price("10.00")
                    .amount("3")
                    .vatType(INC)
            );

    @Autowired
    private EzcountApiClient ezcountApiClient;

    @Test
    public void testNormalRequest() {
        CreateDocResponse result = ezcountApiClient.apiCreateDocPost(NORMAL_REQUEST).schedule().join();
        Assertions.assertThat(result).isNotNull();
    }
}
