package ru.yandex.direct.bsexport.messaging.fields.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.testing.data.TestDirectDeals;
import ru.yandex.direct.bsexport.testing.data.TestOrder;

import static org.assertj.core.api.Assertions.assertThat;

class DirectDealSerializationTest extends BaseSerializationTest {

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

        assertThat(json).doesNotContain("\"DirectDeals\"");
        assertThat(soap).doesNotContain("<DirectDeals");
    }

    @Test
    void singleDeal_serializedInJson() {
        builder.addDirectDeals(TestDirectDeals.directDeal1);

        serialize();

        assertThat(json).contains(",\"DirectDeals\":[{\"ID\":\"8394282\"}]");
    }

    @Test
    void threeDeals_serializedInJson() {
        builder.addDirectDeals(TestDirectDeals.directDeal1);
        builder.addDirectDeals(TestDirectDeals.directDeal2);
        builder.addDirectDeals(TestDirectDeals.directDeal3);

        serialize();

        assertThat(json).contains(",\"DirectDeals\":[{\"ID\":\"8394282\"},{\"ID\":\"6281542\"},{\"ID\":\"7643643\"}]");
    }

    @Disabled("после ОК от БК и/или выпиливания в perl, DIRECT-105453#5de79d156808074705817882")
    @Test
    void singleDeal_skippedInSoap() {
        builder.addDirectDeals(TestDirectDeals.directDeal1);

        serialize();

        assertThat(soap).contains("<DirectDeals xsi:type=\"xsd:string\">skipped in SOAP</DirectDeals>");
    }

    @Disabled("после ОК от БК и/или выпиливания в perl, DIRECT-105453#5de79d156808074705817882")
    @Test
    void twoDeals_skippedInSoap() {
        builder.addDirectDeals(TestDirectDeals.directDeal1);
        builder.addDirectDeals(TestDirectDeals.directDeal2);

        serialize();

        assertThat(soap).contains("<DirectDeals xsi:type=\"xsd:string\">skipped in SOAP</DirectDeals>");
    }

    @Test
    void singleDeal_serializedInSOAP() {
        builder.addDirectDeals(TestDirectDeals.directDeal1);

        serialize();

        String expected = "<DirectDeals SOAP-ENC:arrayType=\"namesp2:SOAPStruct[1]\" xsi:type=\"SOAP-ENC:Array\">"
                + "<item xsi:type=\"namesp2:SOAPStruct\"><ID xsi:type=\"xsd:int\">8394282</ID></item>"
                + "</DirectDeals>";
        assertThat(soap).contains(expected);
    }

    @Test
    void threeDeals_serializedInSoap() {
        builder.addDirectDeals(TestDirectDeals.directDeal1);
        builder.addDirectDeals(TestDirectDeals.directDeal2);
        builder.addDirectDeals(TestDirectDeals.directDeal3);

        serialize();

        String expected = "<DirectDeals SOAP-ENC:arrayType=\"namesp2:SOAPStruct[3]\" xsi:type=\"SOAP-ENC:Array\">"
                + "<item xsi:type=\"namesp2:SOAPStruct\"><ID xsi:type=\"xsd:int\">8394282</ID></item>"
                + "<item xsi:type=\"namesp2:SOAPStruct\"><ID xsi:type=\"xsd:int\">6281542</ID></item>"
                + "<item xsi:type=\"namesp2:SOAPStruct\"><ID xsi:type=\"xsd:int\">7643643</ID></item>"
                + "</DirectDeals>";

        assertThat(soap).contains(expected);
    }
}
