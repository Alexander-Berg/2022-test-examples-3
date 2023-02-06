package ru.yandex.market.api.controller.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.controller.annotations.AsString;
import ru.yandex.market.api.controller.annotations.XmlPolymorphicElement;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;

@WithContext
public class StringSerializationModuleTest extends BaseJacksonTest {

    @JsonTypeName(value = "embeddedValue")
    public static class EmbeddedValue {
        @JsonProperty(value = "v1")
        public String value1;
        @JsonProperty(value = "v2")
        public String value2;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            EmbeddedValue that = (EmbeddedValue) o;

            if (value1 != null ? !value1.equals(that.value1) : that.value1 != null) {
                return false;
            }
            if (value2 != null ? !value2.equals(that.value2) : that.value2 != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = value1 != null ? value1.hashCode() : 0;
            result = 31 * result + (value2 != null ? value2.hashCode() : 0);
            return result;
        }
    }

    @XmlRootElement(name = "container")
    public static class Container {
        @AsString
        @XmlAttribute(name = "value")
        public EmbeddedValue value;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Container container = (Container) o;

            if (value != null ? !value.equals(container.value) : container.value != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    @XmlRootElement(name = "container")
    public static class ContainerWithPolymorphicElement {
        @AsString
        @XmlAttribute(name = "value")
        public EmbeddedPolymorphicValue value;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ContainerWithPolymorphicElement that = (ContainerWithPolymorphicElement) o;

            if (value != null ? !value.equals(that.value) : that.value != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    @XmlPolymorphicElement
    @JsonTypeName(value = "embeddedPolymorphicValue")
    public static class EmbeddedPolymorphicValue {
        @JsonProperty(value = "v1")
        public String value1;

        @JsonProperty(value = "v2")
        public String value2;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            EmbeddedPolymorphicValue that = (EmbeddedPolymorphicValue) o;

            if (value1 != null ? !value1.equals(that.value1) : that.value1 != null) {
                return false;
            }
            if (value2 != null ? !value2.equals(that.value2) : that.value2 != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = value1 != null ? value1.hashCode() : 0;
            result = 31 * result + (value2 != null ? value2.hashCode() : 0);
            return result;
        }
    }

    @XmlRootElement(name = "container")
    public static class ContainerWithPropInBase64 {
        @AsString(codec = "base64")
        @XmlAttribute(name = "value")
        public EmbeddedValue value;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ContainerWithPropInBase64 that = (ContainerWithPropInBase64) o;

            if (value != null ? !value.equals(that.value) : that.value != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    @XmlRootElement(name = "container")
    public static class ContainerWithReverseProp {
        @AsString(codec = "reverse")
        @XmlAttribute(name = "value")
        public EmbeddedValue value;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ContainerWithReverseProp that = (ContainerWithReverseProp) o;

            if (value != null ? !value.equals(that.value) : that.value != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    @XmlRootElement(name = "container")
    public static class ContainerWithUnknownCodec {
        @AsString(codec = "unknown")
        @XmlAttribute(name = "value")
        public EmbeddedValue value;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ContainerWithUnknownCodec that = (ContainerWithUnknownCodec) o;

            if (value != null ? !value.equals(that.value) : that.value != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ContextHolder.get().setVersion(Version.V2_0_0);
    }

    @Test
    public void shouldFailOnUnknownStringCodec() throws IOException, JSONException {
        expectMessage("Codec", "unknown", "not found");
        ContainerWithUnknownCodec container = new ContainerWithUnknownCodec();
        EmbeddedValue embeddedValue = new EmbeddedValue();
        embeddedValue.value1 = "vl1";
        embeddedValue.value2 = "vl2";
        container.value = embeddedValue;
        test(container, "", "");
    }

    @Test
    public void shouldSerializeEmbeddedPolymorphicValueToString() throws IOException, JSONException {
        ContainerWithPolymorphicElement container = new ContainerWithPolymorphicElement();
        EmbeddedPolymorphicValue embeddedValue = new EmbeddedPolymorphicValue();
        embeddedValue.value1 = "vl1";
        embeddedValue.value2 = "vl2";
        container.value = embeddedValue;
        test(container, "{\"value\":\"{\\\"__type\\\":\\\"embeddedPolymorphicValue\\\",\\\"v1\\\":\\\"vl1\\\",\\\"v2\\\":\\\"vl2\\\"}\"}",
            "<container value=\"{&quot;__type&quot;:&quot;embeddedPolymorphicValue&quot;,&quot;v1&quot;:&quot;vl1&quot;,&quot;v2&quot;:&quot;vl2&quot;}\"/>");
    }

    @Test
    public void shouldSerializeEmbeddedValueToBase64String() throws IOException, JSONException {
        ContainerWithPropInBase64 container = new ContainerWithPropInBase64();
        EmbeddedValue embeddedValue = new EmbeddedValue();
        embeddedValue.value1 = "vl1";
        embeddedValue.value2 = "vl2";
        container.value = embeddedValue;
        test(container, "{\"value\":\"eyJ2MSI6InZsMSIsInYyIjoidmwyIn0\"}",
            "<container value=\"eyJ2MSI6InZsMSIsInYyIjoidmwyIn0\"/>");
    }

    @Test
    public void shouldSerializeEmbeddedValueToReverseString() throws IOException, JSONException {
        ContainerWithReverseProp container = new ContainerWithReverseProp();
        EmbeddedValue embeddedValue = new EmbeddedValue();
        embeddedValue.value1 = "vl1";
        embeddedValue.value2 = "vl2";
        container.value = embeddedValue;
        test(container, "{\"value\":\"}\\\"2lv\\\":\\\"2v\\\",\\\"1lv\\\":\\\"1v\\\"{\"}",
            "<container value=\"}&quot;2lv&quot;:&quot;2v&quot;,&quot;1lv&quot;:&quot;1v&quot;{\"/>");
    }

    @Test
    public void shouldSerializeEmbeddedValueToString() throws IOException, JSONException {
        Container container = new Container();
        EmbeddedValue embeddedValue = new EmbeddedValue();
        embeddedValue.value1 = "vl1";
        embeddedValue.value2 = "vl2";
        container.value = embeddedValue;
        test(container, "{\"value\":\"{\\\"v1\\\":\\\"vl1\\\",\\\"v2\\\":\\\"vl2\\\"}\"}",
            "<container value=\"{&quot;v1&quot;:&quot;vl1&quot;,&quot;v2&quot;:&quot;vl2&quot;}\"/>");
    }

}
