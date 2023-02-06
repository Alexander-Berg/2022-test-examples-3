package ru.yandex.market.mbi.jaxb.jackson;

import java.io.IOException;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.json.JSONException;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author m-bazhenov
 * @date 19/10/2017
 */
public class EnumSerializationTest extends BaseJacksonTest {

    public EnumSerializationTest() {
        ApiObjectMapperFactory objectMapperFactory = new ApiObjectMapperFactory();
        jsonMapper = objectMapperFactory.createJsonMapper();
        xmlMapper = objectMapperFactory.createXmlMapper();
    }

    @Test
    public void shouldSerializeEnumWithFieldAnnotations() throws JSONException, SAXException, IOException {
        Container c = new Container();
        c.setFirstEnumField(Container.EnumWithFieldAnnotations.FIRST);
        c.setSecondEnumField(Container.EnumWithFieldAnnotations.SECOND);

        test(
                c,
                "{\"first\":\"0\",\"second\":\"1\"}",
                "<container><first>0</first><second>1</second></container>"
        );
    }

    @Test(expected = JsonMappingException.class)
    public void shouldNotSerializeEnumWithoutFieldAnnotations() throws JSONException, SAXException, IOException {
        Container1 c = new Container1();
        c.setFirstEnumField(Container1.EnumWithoutFieldAnnotations.FIRST);
        c.setSecondEnumField(Container1.EnumWithoutFieldAnnotations.SECOND);
        test(c, "anything", "anything");

    }

    @Test(expected = JsonMappingException.class)
    public void shouldNotSerializeEnumWithoutTypeAnnotation() throws JSONException, SAXException, IOException {
        Container2 c = new Container2();
        c.setFirstEnumField(Container2.EnumWithoutTypeAnnotation.FIRST);
        c.setSecondEnumField(Container2.EnumWithoutTypeAnnotation.SECOND);
        test(c, "anything", "anything");

    }

    @Test
    public void shouldSerializeEnumWithoutFieldAnnotationsButWithOneMethodAnnotation() throws JSONException, SAXException, IOException {
        Container3 c = new Container3();
        c.setFirstEnumField(Container3.EnumWithoutFieldAnnotationsButWithOneMethodAnnotation.FIRST);
        c.setSecondEnumField(Container3.EnumWithoutFieldAnnotationsButWithOneMethodAnnotation.SECOND);

        test(
                c,
                "{\"first\":\"0\",\"second\":\"1\"}",
                "<container><first>0</first><second>1</second></container>"
        );
    }
}

@XmlType
@XmlRootElement(name="container")
class Container {
    @XmlEnum
    public enum EnumWithFieldAnnotations {
        @XmlEnumValue("0")
        FIRST,
        @XmlEnumValue("1")
        SECOND
    }

    @XmlElement(name = "first")
    private EnumWithFieldAnnotations firstEnumField;
    @XmlElement(name = "second")
    private EnumWithFieldAnnotations secondEnumField;

    void setFirstEnumField(EnumWithFieldAnnotations firstEnumField) {
        this.firstEnumField = firstEnumField;
    }

    void setSecondEnumField(EnumWithFieldAnnotations secondEnumField) {
        this.secondEnumField = secondEnumField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Container container = (Container) o;
        return firstEnumField == container.firstEnumField && secondEnumField == container.secondEnumField;
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstEnumField, secondEnumField);
    }
}

@XmlType
@XmlRootElement(name = "container")
class Container1 {
    @XmlEnum
    enum EnumWithoutFieldAnnotations {
        FIRST, SECOND
    }

    @XmlElement(name = "first")
    private EnumWithoutFieldAnnotations firstEnumField;
    @XmlElement(name = "second")
    private EnumWithoutFieldAnnotations secondEnumField;

    void setFirstEnumField(EnumWithoutFieldAnnotations firstEnumField) {
        this.firstEnumField = firstEnumField;
    }

    void setSecondEnumField(EnumWithoutFieldAnnotations secondEnumField) {
        this.secondEnumField = secondEnumField;
    }
}

@XmlType
@XmlRootElement(name = "container")
class Container2 {

    enum EnumWithoutTypeAnnotation {
        @XmlEnumValue("0")
        FIRST,
        @XmlEnumValue("1")
        SECOND
    }

    @XmlElement(name = "first")
    private EnumWithoutTypeAnnotation firstEnumField;
    @XmlElement(name = "second")
    private EnumWithoutTypeAnnotation secondEnumField;

    void setFirstEnumField(EnumWithoutTypeAnnotation firstEnumField) {
        this.firstEnumField = firstEnumField;
    }

    void setSecondEnumField(EnumWithoutTypeAnnotation secondEnumField) {
        this.secondEnumField = secondEnumField;
    }
}

@XmlType
@XmlRootElement(name = "container")
class Container3 {

    @XmlEnum
    enum EnumWithoutFieldAnnotationsButWithOneMethodAnnotation {
        FIRST("0"),
        SECOND("1");

        final String key;

        EnumWithoutFieldAnnotationsButWithOneMethodAnnotation(String key) {
            this.key = key;
        }

        @XmlValue
        public String getKey() {
            return key;
        }
    }

    @XmlElement(name = "first")
    private EnumWithoutFieldAnnotationsButWithOneMethodAnnotation firstEnumField;
    @XmlElement(name = "second")
    private EnumWithoutFieldAnnotationsButWithOneMethodAnnotation secondEnumField;

    void setFirstEnumField(EnumWithoutFieldAnnotationsButWithOneMethodAnnotation firstEnumField) {
        this.firstEnumField = firstEnumField;
    }

    void setSecondEnumField(EnumWithoutFieldAnnotationsButWithOneMethodAnnotation secondEnumField) {
        this.secondEnumField = secondEnumField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Container3 that = (Container3) o;
        return firstEnumField == that.firstEnumField && secondEnumField == that.secondEnumField;
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstEnumField, secondEnumField);
    }
}
