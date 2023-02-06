package ru.yandex.direct.bsexport.messaging.soap;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.testing.model.InnerMessage;
import ru.yandex.direct.testing.model.TestMessage;

import static org.assertj.core.api.Assertions.assertThat;

class RepeatedSerializationTest extends BaseSerializationTest {
    private TestMessage.Builder builder;

    @BeforeEach
    void prepare() {
        builder = TestMessage.newBuilder();
    }

    private void serialize() {
        super.serialize(builder.build());
    }

    @Test
    void singleMessageElement() {
        builder.addRepeatedMessageField(InnerMessage.newBuilder().setStringField("abc").setInt32Field(456));

        serialize();

        String expected =
                "<RepeatedMessageField SOAP-ENC:arrayType=\"namesp2:SOAPStruct[1]\" xsi:type=\"SOAP-ENC:Array\">"
                        + "<item xsi:type=\"namesp2:SOAPStruct\">"
                        + "<StringField xsi:type=\"xsd:string\">abc</StringField>"
                        + "<Int32Field xsi:type=\"xsd:int\">456</Int32Field>"
                        + "</item>"
                        + "</RepeatedMessageField>";
        assertThat(soap).contains(expected);
    }

    @Test
    void twoMessageElements() {
        builder.addRepeatedMessageField(InnerMessage.newBuilder().setStringField("abc").setInt32Field(456))
                .addRepeatedMessageField(InnerMessage.newBuilder().setStringField("qwerty").setInt32Field(987654));

        serialize();

        String expected = "<RepeatedMessageField"
                + " SOAP-ENC:arrayType=\"namesp2:SOAPStruct[2]\" xsi:type=\"SOAP-ENC:Array\">"
                + "<item xsi:type=\"namesp2:SOAPStruct\">"
                + "<StringField xsi:type=\"xsd:string\">abc</StringField>"
                + "<Int32Field xsi:type=\"xsd:int\">456</Int32Field>"
                + "</item>"
                + "<item xsi:type=\"namesp2:SOAPStruct\">"
                + "<StringField xsi:type=\"xsd:string\">qwerty</StringField>"
                + "<Int32Field xsi:type=\"xsd:int\">987654</Int32Field>"
                + "</item>"
                + "</RepeatedMessageField>";
        assertThat(soap).contains(expected);
    }

    @Test
    void threeStringElements() {
        builder.addRepeatedStringField("123, abcd!");
        builder.addRepeatedStringField("wow-test");
        builder.addRepeatedStringField("(-_-)");

        serialize();

        String expected = "<RepeatedStringField SOAP-ENC:arrayType=\"xsd:string[3]\" xsi:type=\"SOAP-ENC:Array\">"
                + "<item xsi:type=\"xsd:string\">123, abcd!</item>"
                + "<item xsi:type=\"xsd:string\">wow-test</item>"
                + "<item xsi:type=\"xsd:string\">(-_-)</item>"
                + "</RepeatedStringField>";
        assertThat(soap).contains(expected);
    }

    @Test
    void twoUnicodeStringsElements() {
        String s1 = "рыбалка";
        String s2 = "конь в яблоках";
        builder.addRepeatedStringField(s1);
        builder.addRepeatedStringField(s2);

        serialize();

        String expected = "<RepeatedStringField SOAP-ENC:arrayType=\"SOAP-ENC:base64[2]\" xsi:type=\"SOAP-ENC:Array\">"
                + "<item xsi:type=\"SOAP-ENC:base64\">" + Base64.getEncoder().encodeToString(s1.getBytes()) + "</item>"
                + "<item xsi:type=\"SOAP-ENC:base64\">" + Base64.getEncoder().encodeToString(s2.getBytes()) + "</item>"
                + "</RepeatedStringField>";
        assertThat(soap).contains(expected);
    }

    @Test
    void twoStringMixedTypeElements() {
        String s1 = "sunny";
        String s2 = "конь в яблоках";
        builder.addRepeatedStringField(s1);
        builder.addRepeatedStringField(s2);

        serialize();

        String expected = "<RepeatedStringField SOAP-ENC:arrayType=\"xsd:ur-type[2]\" xsi:type=\"SOAP-ENC:Array\">"
                + "<item xsi:type=\"xsd:string\">" + s1 + "</item>"
                + "<item xsi:type=\"SOAP-ENC:base64\">" + Base64.getEncoder().encodeToString(s2.getBytes()) + "</item>"
                + "</RepeatedStringField>";
        assertThat(soap).contains(expected);
    }

    @Test
    void noFields_NotSerialized() {
        serialize();

        assertThat(soap).doesNotContain("<RepeatedStringField");
    }
}
