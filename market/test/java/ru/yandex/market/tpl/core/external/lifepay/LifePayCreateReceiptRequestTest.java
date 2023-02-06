package ru.yandex.market.tpl.core.external.lifepay;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.util.JacksonUtil;
import ru.yandex.market.tpl.core.external.lifepay.request.LifePayCreateReceiptRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
class LifePayCreateReceiptRequestTest {

    @Test
    void testDeserialization() throws Exception {
        LifePayCreateReceiptRequest createReceiptRequest = JacksonUtil.fromString(
                IOUtils.toString(
                        this.getClass().getResourceAsStream("/lifepay/fiscal_request.json"),
                        StandardCharsets.UTF_8
                ),
                LifePayCreateReceiptRequest.class
        );
        assertThat(createReceiptRequest).isNotNull();
        LifePayCreateReceiptRequest.Product product = createReceiptRequest.getPurchase().getProducts().get(0);
        assertThat(product.getSupplierInn())
                .isEqualTo("1234567894");
        assertThat(product.getAgentItemType().getCommissioner().equals(1));
    }

}
