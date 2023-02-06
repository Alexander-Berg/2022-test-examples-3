package ru.yandex.market.mbi.jaxb.jackson;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.google.common.base.MoreObjects;
import org.json.JSONException;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author zoom
 */
public class XmlNamingStrategyTest extends BaseJacksonTest {

    public XmlNamingStrategyTest() {
        ApiObjectMapperFactory objectMapperFactory = new ApiObjectMapperFactory();
        jsonMapper = objectMapperFactory.createJsonMapper();
        xmlMapper = objectMapperFactory.createXmlMapper(new XmlNamingStrategy());
    }

    @XmlRootElement(name = "base")
    public static class Base {

        public Base() {
        }

        public Base(Object id) {
            this.id = String.valueOf(id);
        }

        @XmlAttribute(name = "elementId")
        public String id;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Base base = (Base) o;
            return id != null ? id.equals(base.id) : base.id == null;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .toString();
        }
    }

    @XmlRootElement(name = "data")
    public static class Container {
        @XmlElementWrapper(name = "superElements")
        @XmlElement(name = "superElement")
        private List<Integer> list;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Container container = (Container) o;
            return !(list != null ? !list.equals(container.list) : container.list != null);

        }

        @Override
        public int hashCode() {
            return list != null ? list.hashCode() : 0;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("list", list)
                    .toString();
        }
    }

    @XmlRootElement(name = "rootNameTest")
    public static class RootNameTest {

        private Object nil;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RootNameTest that = (RootNameTest) o;
            return Objects.equals(nil, that.nil);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nil);
        }
    }

    @XmlRootElement(name = "data")
    public static class UnwrappedElementsTest {
        @XmlElement(name = "eleMent")
        @JsonProperty("eleMents")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Integer> list;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UnwrappedElementsTest that = (UnwrappedElementsTest) o;
            return Objects.equals(list, that.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(list);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("list", list)
                    .toString();
        }
    }


    @Test
    public void shouldRenameAttribute() throws IOException, JSONException, SAXException {
        Base obj = new Base(123);
        test(obj, "{\"elementId\":\"123\"}", "<base element-id=\"123\"/>");
    }

    @Test
    public void shouldRenameWrapperAndWrapped() throws IOException, JSONException, SAXException {
        Container obj = new Container();
        obj.list = Arrays.asList(1,2,3);
        test(obj, "{\"superElements\":[1,2,3]}", "<data><super-elements><super-element>1</super-element><super-element>2</super-element><super-element>3</super-element></super-elements></data>");
    }

    @Test
    public void shouldRenameRootName() throws IOException, JSONException, SAXException {
        RootNameTest obj = new RootNameTest();
        test(obj, "{}", "<root-name-test/>");
    }

    @Test
    public void shouldRenameUnwrappedCollectionElements() throws IOException, JSONException, SAXException {
        UnwrappedElementsTest obj = new UnwrappedElementsTest();
        obj.list = Arrays.asList(2, 3, 1);
        test(obj, "{\"eleMents\":[2,3,1]}", "<data><ele-ment>2</ele-ment><ele-ment>3</ele-ment><ele-ment>1</ele-ment></data>");
    }
}
