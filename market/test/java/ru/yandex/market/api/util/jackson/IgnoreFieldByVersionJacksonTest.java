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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Тест является полумерой, потому как: будет вызыватся objectMapperFactory
 * для получение нового objectMapper из контекста или нет зависит от интеграци с SpringMVC
 *
 * См: {@link ru.yandex.market.api.controller.jackson.Abstract2HttpMessageConverter}
 * @author dimkarp93
 */
@WithContext
public class IgnoreFieldByVersionJacksonTest extends UnitTestBase {
    private static final String DUPLICATE = "dup";

    private static final Output OUTPUT = new Output("old", "new");

    @XmlRootElement(name = "output")
    private static class Output {
        @XmlElement(name = DUPLICATE)
        @Versioned("2.0.0-2.0.4")
        private String oldF;

        @XmlElement(name = DUPLICATE)
        @Versioned("2.0.5-*")
        private String newF;

        public Output(String oldF, String newF) {
            this.oldF = oldF;
            this.newF = newF;
        }

        public String getOldF() {
            return oldF;
        }

        public void setOldF(String oldF) {
            this.oldF = oldF;
        }

        public String getNewF() {
            return newF;
        }

        public void setNewF(String newF) {
            this.newF = newF;
        }
    }

    private static void version(Version version) {
        ContextHolder.update(ctx -> ctx.setVersion(version));
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
    public void invisibleOld_invisibleNew_serialize_json() {
        version(Version.V1_0_0);

        String output = json.write(OUTPUT);
        assertThat(output, not(containsString(DUPLICATE)));
    }

    @Test
    public void visibleOld_invisibleNew_seialize_json() {
        version(Version.V2_0_0);

        String output = json.write(OUTPUT);
        assertThat(output, json.match(DUPLICATE, "old"));
    }

    @Test
    public void invisibleOld_visibleNew_serialize_json() {
        version(Version.V2_0_5);

        String output = json.write(OUTPUT);
        assertThat(output, json.match(DUPLICATE, "new"));
    }

    @Test
    public void invisibleOld_invisibleNew_serialize_xml() {
        version(Version.V1_0_0);

        String output = xml.write(OUTPUT);
        assertThat(output, not(containsString(DUPLICATE)));
    }


    @Test
    public void visibleOld_invisibleNew_serialize_xml() {
        version(Version.V2_0_0);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match(DUPLICATE, "old"));
    }

    @Test
    public void invisibleOld_visibleNew_serialize_xml() {
        version(Version.V2_0_5);

        String output = xml.write(OUTPUT);
        assertThat(output, xml.match(DUPLICATE, "new"));
    }
}
