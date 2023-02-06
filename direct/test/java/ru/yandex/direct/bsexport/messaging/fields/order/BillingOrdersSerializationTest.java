package ru.yandex.direct.bsexport.messaging.fields.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.testing.data.TestBillingAggregates;
import ru.yandex.direct.bsexport.testing.data.TestOrder;

import static org.assertj.core.api.Assertions.assertThat;

public class BillingOrdersSerializationTest extends BaseSerializationTest {

    private Order.Builder builder;

    @BeforeEach
    void prepare() {
        builder = TestOrder.text1Base.toBuilder();
    }

    private void serialize() {
        super.serialize(builder.build());
    }

    @Test
    void notSet_NotSerialized() {
        serialize();

        assertThat(json).doesNotContain("\"BillingOrders\"");
        assertThat(soap).doesNotContain("<BillingOrders");
    }

    @Test
    void smokeTest_serializedInJson() {
        builder.setBillingOrders(TestBillingAggregates.billingAggregates1);
        serialize();

        assertThat(json).contains("\"BillingOrders\":{"
                + "\"Default\":41603880,\"Rules\":["
                + "{\"ProductTypes\":[\"VideoCreativeReach\"],\"Result\":41603887},"
                + "{\"ProductTypes\":[\"VideoCreativeReachOutdoor\"],\"Result\":41603894},"
                + "{\"ProductTypes\":[\"VideoCreativeReachIndoor\"],\"Result\":43921364},"
                + "{\"ProductTypes\":[\"AudioCreativeReach\"],\"Result\":44798409}]}");
    }

    @Test
    void smokeTest_SkippedInSoap() {
        builder.setBillingOrders(TestBillingAggregates.billingAggregates1);
        serialize();

        assertThat(soap).contains("<BillingOrders xsi:type=\"xsd:string\">skipped in SOAP</BillingOrders>");
    }
}
