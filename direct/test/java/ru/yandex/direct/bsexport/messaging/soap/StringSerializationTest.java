package ru.yandex.direct.bsexport.messaging.soap;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.testing.model.TestMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.testing.Util.getFromClasspath;

class StringSerializationTest extends BaseSerializationTest {

    private TestMessage.Builder builder;

    @BeforeEach
    void prepare() {
        builder = TestMessage.newBuilder();
    }

    private void serialize() {
        super.serialize(builder.build());
    }

    @Test
    void stringIsNotSet_NotSerialized() {
        serialize();

        String expected = getFromClasspath("soap/update_data2_empty.xml");
        assertThat(soap).isXmlEqualTo(expected);
    }

    @Test
    void emptyString_SerializedAsIs() {
        builder.setStringField("");

        serialize();

        assertThat(soap).contains("<StringField xsi:type=\"xsd:string\"/>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"qwerty", "0123456789", "abc, 987! -- ZWY", "?", ":", ";", "\\", "/", "|", "~", "`",
            "!", "@", "_", "-", "+", "=", "()", "[]", "{}", "^", "*", "$", "%", ",.", "#", "'", "\"", "\n"})
    void latinString_SerializedAsIs(String str) {
        builder.setStringField(str);

        serialize();

        assertThat(soap).contains("<StringField xsi:type=\"xsd:string\">" + str + "</StringField>");
    }

    /**
     * в SOAP::Lite указанные примеры сериализовались as-is (но стандарт допускает принудительную сериализацию)
     */
    @ParameterizedTest
    @CsvSource({">, &gt;", "]>, ]&gt;"})
    void latinString_serializedEscaped_notAsInPerl(String source, String escaped) {
        builder.setStringField(source);

        serialize();

        assertThat(soap).contains("<StringField xsi:type=\"xsd:string\">" + escaped + "</StringField>");
    }

    @Test
    void carriageReturnString_serializedEscaped_notAsInPerl() {
        builder.setStringField("before\rafter");

        serialize();

        assertThat(soap).contains("<StringField xsi:type=\"xsd:string\">before&#13;after</StringField>");
    }

    @ParameterizedTest
    @CsvSource({"<, &lt;", "&, &amp;"})
    void latinString_serializedEscaped(String source, String escaped) {
        builder.setStringField(source);

        serialize();

        assertThat(soap).contains("<StringField xsi:type=\"xsd:string\">" + escaped + "</StringField>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"строка", "Шлакоблоки по 300", "запчасть №2", "абвгд abcdef"})
    void nationalString_SerializedEncodedToBase64(String str) {
        builder.setStringField(str);

        serialize();

        String encoded = Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        assertThat(soap).contains("<StringField xsi:type=\"SOAP-ENC:base64\">" + encoded + "</StringField>");
    }
}
