package ru.yandex.direct.bsexport.messaging.fields.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.bsexport.model.ContentType;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.testing.data.TestOrder;

import static org.assertj.core.api.Assertions.assertThat;

class ContentTypeSerializationTest extends BaseSerializationTest {
    private Order.Builder builder;

    @BeforeEach
    void prepare() {
        builder = TestOrder.text1Base.toBuilder();
    }

    private void serialize() {
        super.serialize(builder.build());
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void serializedInSoapAsString(ContentType type) {
        builder.setContentType(type);

        serialize();

        assertThat(soap).contains("<ContentType xsi:type=\"xsd:string\">" + type.name() + "</ContentType>");
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void serializedInJsonAsString(ContentType type) {
        builder.setContentType(type);

        serialize();

        assertThat(json).contains(",\"ContentType\":\"" + type.name() + "\"");
    }

    @Test
    void textExplicitlyCheck() {
        builder.setContentType(ContentType.text);

        serialize();

        assertThat(soap).contains("<ContentType xsi:type=\"xsd:string\">text</ContentType>");
        assertThat(json).contains(",\"ContentType\":\"text\"");
    }

    @Test
    void internalDistribExplicitlyCheck() {
        builder.setContentType(ContentType.internal_distrib);

        serialize();

        assertThat(soap).contains("<ContentType xsi:type=\"xsd:string\">internal_distrib</ContentType>");
        assertThat(json).contains(",\"ContentType\":\"internal_distrib\"");
    }

}
