package ru.yandex.market.api.util.jackson;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import ru.yandex.market.api.controller.annotations.Versioned;
import ru.yandex.market.api.controller.jackson.CapiObjectMapperFactory;
import ru.yandex.market.api.controller.jackson.ObjectMapperFactory;
import ru.yandex.market.api.controller.serialization.StringCodecStorage;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Тест является полумерой, потому как: будет вызыватся objectMapperFactory
 * для получение нового objectMapper из контекста или нет зависит от интеграци с SpringMVC
 *
 * См: {@link ru.yandex.market.api.controller.jackson.Abstract2HttpMessageConverter}
 * @author dimkarp93
 */
@WithContext
public class VersoinedJacksonTest extends UnitTestBase {

    private static final String INPUT_JSON = "input_version.json";
    private static final String INPUT_XML = "input_version.xml";

    private static final Output OUTPUT = new Output("123", "secret");

    @XmlRootElement(name = "input")
    private static class Input {
        @XmlElement(name = "b")
        private String b;

        @XmlElement(name = "secret")
        @Versioned("2.0.5-*")
        private String secret;

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    @XmlRootElement(name = "output")
    private static class Output {
        @XmlElement(name = "b")
        private String b;

        @XmlElement(name = "secret")
        @Versioned("2.0.5-*")
        private String secret;

        public Output() {
        }

        public Output(String b, String secret) {
            this.b = b;
            this.secret = secret;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    private ObjectMapperFactory objectMapperFactory = new CapiObjectMapperFactory(new StringCodecStorage());

    private Json json;
    private Xml xml;

    private static void version(Version version) {
        ContextHolder.update(ctx -> ctx.setVersion(version));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        json = new Json(objectMapperFactory);
        xml = new Xml(objectMapperFactory);
    }

    @Test
    public void visibleForVersion_deserialize_json() {
        version(Version.V2_0_5);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.b);
        assertEquals("secret", input.secret);
    }

    @Test
    public void invisibleForVersion_deserialize_json() {
        version(Version.V2_0_0);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.b);
        assertNull(input.secret);
    }

    @Test
    public void visibleForFirstVersion_invisibleForSecondVersion_deserialize_json() {
        version(Version.V2_0_5);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.b);
        assertEquals("secret", input.secret);

        version(Version.V2_0_0);

        input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.b);
        assertNull(input.secret);
    }

    @Test
    public void invisibleForFirstVersion_visibleForSecondVersion_deserialize_json() {
        version(Version.V2_0_0);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.b);
        assertNull(input.secret);

        version(Version.V2_0_5);

        input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.b);
        assertEquals("secret", input.secret);
    }

    @Test
    public void visibleForVersion_serialize_json() {
        version(Version.V2_0_5);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("b", "123"));
        assertThat(output, json.match("secret","secret"));
    }

    @Test
    public void invisibleForVersion_serialize_json() {
        version(Version.V2_0_0);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("b", "123"));
        assertThat(output, not(json.match("secret","secret")));
    }

    @Test
    public void visibleForFirstVersion_invisibleForSecondVersion_serialize_json() {
        version(Version.V2_0_5);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("b", "123"));
        assertThat(output, json.match("secret","secret"));

        version(Version.V2_0_0);

        output = json.write(OUTPUT);
        assertThat(output, json.match("b", "123"));
        assertThat(output, not(json.match("secret","secret")));
    }

    @Test
    public void invisibleForFirstVersion_visibleForSecondVersion_serialize_json() {
        version(Version.V2_0_0);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("b", "123"));
        assertThat(output, not(json.match("secret","secret")));

        version(Version.V2_0_5);

        output = json.write(OUTPUT);
        assertThat(output, json.match("b", "123"));
        assertThat(output, json.match("secret","secret"));
    }

    @Test
    public void visibleForVersion_deserialize_xml() {
        version(Version.V2_0_5);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.b);
        assertEquals("secret", input.secret);
    }

    @Test
    public void invisibleForVersion_deserialize_xml() {
        version(Version.V2_0_0);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.b);
        assertNull(input.secret);
    }

    @Test
    public void visibleForFirstVersion_invisibleForSecondVersion_deserialize_xml() {
        version(Version.V2_0_5);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.b);
        assertEquals("secret", input.secret);

        version(Version.V2_0_0);

        input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.b);
        assertNull(input.secret);
    }

    @Test
    public void invisibleForFirstVersion_visibleForSecondVersion_deserialize_xml() {
        version(Version.V2_0_0);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.b);
        assertNull(input.secret);

        version(Version.V2_0_5);

        input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.b);
        assertEquals("secret", input.secret);
    }

    @Test
    public void visibleForVersion_serialize_xml() {
        version(Version.V2_0_5);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("b", "123"));
        assertThat(output, xml.match("secret", "secret"));
    }

    @Test
    public void invisibleForVersion_serialize_xml() {
        version(Version.V2_0_0);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("b", "123"));
        assertThat(output, not(xml.match("secret", "secret")));
    }

    @Test
    public void visibleForFirstVersion_invisibleForSecondVersion_serialize_xml() {
        version(Version.V2_0_5);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("b", "123"));
        assertThat(output, xml.match("secret", "secret"));

        version(Version.V2_0_0);

        output = xml.write(OUTPUT);
        assertThat(output, xml.match("b", "123"));
        assertThat(output, not(xml.match("secret", "secret")));
    }


    @Test
    public void invisibleForFirstVersion_visibleForSecondVersion_serialize_xml() {
        version(Version.V2_0_0);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("b", "123"));
        assertThat(output, not(xml.match("secret", "secret")));

        version(Version.V2_0_5);

        output = xml.write(OUTPUT);
        assertThat(output, xml.match("b", "123"));
        assertThat(output, xml.match("secret", "secret"));
    }


}
