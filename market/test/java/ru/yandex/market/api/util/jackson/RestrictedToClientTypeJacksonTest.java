package ru.yandex.market.api.util.jackson;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import ru.yandex.market.api.controller.annotations.RestrictedToClientType;
import ru.yandex.market.api.controller.jackson.CapiObjectMapperFactory;
import ru.yandex.market.api.controller.jackson.ObjectMapperFactory;
import ru.yandex.market.api.controller.serialization.StringCodecStorage;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ClientTestUtil;

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
public class RestrictedToClientTypeJacksonTest extends UnitTestBase {

    private static final String INPUT_JSON = "input_client.json";
    private static final String INPUT_XML = "input_client.xml";

    private static final Output OUTPUT = new Output("123", "secret");

    @XmlRootElement(name = "input")
    private static class Input {
        @XmlElement(name = "a")
        private String a;

        @XmlElement(name = "secret")
        @RestrictedToClientType(Client.Type.INTERNAL)
        private String secret;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
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
        @XmlElement(name = "a")
        private String a;

        @XmlElement(name = "secret")
        @RestrictedToClientType(Client.Type.INTERNAL)
        private String secret;

        public Output() {
        }

        public Output(String a, String secret) {
            this.a = a;
            this.secret = secret;
        }

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        json = new Json(objectMapperFactory);
        xml = new Xml(objectMapperFactory);
    }

    @Test
    public void visibleForClient_deserialize_json() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.a);
        assertEquals("secret", input.secret);
    }

    @Test
    public void invisibleForClient_deserialize_json() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.a);
        assertNull(input.secret);
    }

    @Test
    public void visibleForFirstClient_invisibleForSecondClient_deserialize_json() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.a);
        assertEquals("secret", input.secret);

        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.a);
        assertNull(input.secret);
    }

    @Test
    public void invisibleForFirstClient_visibleForSecondClient_deserialize_json() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        Input input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.a);
        assertNull(input.secret);

        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        input = json.read(INPUT_JSON, Input.class);
        assertEquals("123", input.a);
        assertEquals("secret", input.secret);
    }

    @Test
    public void visibleForClient_serialize_json() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("a", "123"));
        assertThat(output, json.match("secret","secret"));
    }

    @Test
    public void invisibleForClient_serialize_json() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("a", "123"));
        assertThat(output, not(json.match("secret","secret")));
    }

    @Test
    public void visibleForFirstClient_invisibleForSecondClient_serialize_json() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("a", "123"));
        assertThat(output, json.match("secret","secret"));

        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        output = json.write(OUTPUT);
        assertThat(output, json.match("a", "123"));
        assertThat(output, not(json.match("secret","secret")));
    }

    @Test
    public void invisibleForFirstClient_visibleForSecondClient_serialize_json() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        String output = json.write(OUTPUT);
        assertThat(output, json.match("a", "123"));
        assertThat(output, not(json.match("secret","secret")));

        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        output = json.write(OUTPUT);
        assertThat(output, json.match("a", "123"));
        assertThat(output, json.match("secret","secret"));
    }

    @Test
    public void visibleForClient_deserialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.a);
        assertEquals("secret", input.secret);
    }

    @Test
    public void invisibleForClient_deserialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.a);
        assertNull(input.secret);
    }

    @Test
    public void visibleForFirstClient_invisibleForSecondClient_deserialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.a);
        assertEquals("secret", input.secret);

        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.a);
        assertNull(input.secret);
    }

    @Test
    public void invisibleForFirstClient_visibleForSecondClient_deserialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        Input input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.a);
        assertNull(input.secret);

        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        input = xml.read(INPUT_XML, Input.class);
        assertEquals("123", input.a);
        assertEquals("secret", input.secret);
    }

    @Test
    public void visibleForClient_serialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("a", "123"));
        assertThat(output, xml.match("secret", "secret"));
    }

    @Test
    public void invisibleForClient_serialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("a", "123"));
        assertThat(output, not(xml.match("secret", "secret")));
    }

    @Test
    public void visibleForFirstClient_invisibleForSecondClient_serialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("a", "123"));
        assertThat(output, xml.match("secret", "secret"));

        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        output = xml.write(OUTPUT);
        assertThat(output, xml.match("a", "123"));
        assertThat(output, not(xml.match("secret", "secret")));
    }


    @Test
    public void invisibleForFirstClient_visibleForSecondClient_serialize_xml() {
        ClientTestUtil.clientOfType(Client.Type.EXTERNAL);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match("a", "123"));
        assertThat(output, not(xml.match("secret", "secret")));

        ClientTestUtil.clientOfType(Client.Type.INTERNAL);

        output = xml.write(OUTPUT);
        assertThat(output, xml.match("a", "123"));
        assertThat(output, xml.match("secret", "secret"));
    }


}
