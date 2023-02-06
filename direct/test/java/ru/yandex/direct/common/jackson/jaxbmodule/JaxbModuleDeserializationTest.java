package ru.yandex.direct.common.jackson.jaxbmodule;


import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class JaxbModuleDeserializationTest extends JaxbModuleTestBase {
    @Test
    public void integerNullWhenXmlElementAbsent() throws IOException {
        String xml = "<ClassForInteger></ClassForInteger>";
        ClassForInteger obj = xmlMapper.readValue(xml, ClassForInteger.class);
        assertNull(obj.jint);
        assertNull(obj.jint2);
    }

    @Test
    public void integerJaxbElementValueWhenXmlElementPresentAndNotNil() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ClassForInteger>"
                + "<jint>11</jint>"
                + "<jint2>22</jint2>"
                + "</ClassForInteger>";
        ClassForInteger obj = xmlMapper.readValue(xml, ClassForInteger.class);
        assertFalse(obj.jint.isNil());
        assertThat(obj.jint.getValue(), is(11));
        assertThat(obj.jint2, is(22));
    }

    @Test
    public void integerJaxbElementNilWhenXmlElementPresentAndNil() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ClassForInteger xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<jint xsi:nil=\"true\" />"
                + "<jint2>22</jint2>"
                + "</ClassForInteger>";
        ClassForInteger obj = xmlMapper.readValue(xml, ClassForInteger.class);
        assertTrue(obj.jint.isNil());
        assertThat(obj.jint2, is(22));
    }

    @Test
    public void integerJaxbElementNilWhenXmlElementPresentAndNilWithValue() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ClassForInteger xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<jint xsi:nil=\"true\">xxx</jint>"
                + "<jint2>22</jint2>"
                + "</ClassForInteger>";
        ClassForInteger obj = xmlMapper.readValue(xml, ClassForInteger.class);
        assertTrue(obj.jint.isNil());
        assertThat(obj.jint2, is(22));
    }

    @Test
    public void integerNullWhenJsonElementAbsent() throws IOException {
        String json = "{}";
        ClassForInteger obj = objectMapper.readValue(json, ClassForInteger.class);
        assertNull(obj.jint);
    }

    @Test
    public void integerJaxbElementValueWhenJsonElementPresentAndNotNull() throws IOException {
        String json = "{\"jint\": 11, \"jint2\": 22}";
        ClassForInteger obj = objectMapper.readValue(json, ClassForInteger.class);
        assertFalse(obj.jint.isNil());
        assertThat(obj.jint.getValue(), is(11));
        assertThat(obj.jint2, is(22));
    }

    @Test
    public void integerJaxbElementNilWhenJsonElementPresentAndNul() throws IOException {
        String json = "{\"jint\": null, \"jint2\": 22}";
        ClassForInteger obj = objectMapper.readValue(json, ClassForInteger.class);
        assertTrue(obj.jint.isNil());
        assertThat(obj.jint2, is(22));
    }


    @Test
    public void beanNullWhenXmlElementAbsent() throws IOException {
        String xml = "<Xxx></Xxx>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertNull(obj.nillableBean);
    }

    @Test
    public void beanJaxbElementValueWhenXmlElementPresentAndNotNil() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><NillableBean><SomeX>11</SomeX></NillableBean></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertFalse(obj.nillableBean.isNil());
        assertThat(obj.nillableBean.getValue().someX, is(11));
    }

    @Test
    public void beanJaxbElementNilWhenXmlElementPresentAndNil() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<NillableBean xsi:nil=\"true\" />"
                + "</GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertTrue(obj.nillableBean.isNil());
    }

    @Test
    public void beanNullWhenJsonElementAbsent() throws IOException {
        String json = "{}";
        GetRequest obj = objectMapper.readValue(json, GetRequest.class);
        assertNull(obj.nillableBean);
    }

    @Test
    public void beanJaxbElementValueWhenJsonElementPresentAndNotNull() throws IOException {
        String json = "{\"NillableBean\": {\"SomeX\": 11}}";
        GetRequest obj = objectMapper.readValue(json, GetRequest.class);
        assertFalse(obj.nillableBean.isNil());
        assertThat(obj.nillableBean.getValue().someX, is(11));
    }

    @Test
    public void beanJaxbElementNilWhenJsonElementPresentAndNull() throws IOException {
        String json = "{\"NillableBean\": null}";
        GetRequest obj = objectMapper.readValue(json, GetRequest.class);
        assertTrue(obj.nillableBean.isNil());
    }

    @Test
    public void beanNullWhenXmlElementEmpty() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SelectionCriteria /></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertNull(obj.selectionCriteria);
    }

    @Test
    public void beanNullWhenXmlElementContainsEmptyString() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SelectionCriteria></SelectionCriteria></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertNull(obj.selectionCriteria);
    }

    @Test
    public void beanNullWhenXmlElementContainsWhitespacesOnly() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SelectionCriteria>  \t \n \r </SelectionCriteria></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertNull(obj.selectionCriteria);
    }

    @Test
    public void beanWithValues() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SelectionCriteria><Ids>3</Ids></SelectionCriteria>"
                + "<FieldNames>Id</FieldNames></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertThat(obj.selectionCriteria.ids, is(Collections.singletonList(3)));
    }

    @Test
    public void beanWithValues2() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SelectionCriteria><SomeX>3</SomeX></SelectionCriteria>"
                + "<FieldNames>Id</FieldNames></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertThat(obj.selectionCriteria.someX, is(3));
    }

    @Test
    public void maybeXmlParserBug() throws IOException {
        // внутри отсутсвующего бина должен быть список, и во вне должен быть список
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SelectionCriteria>  \t \n \r </SelectionCriteria>"
                + "<FieldNames>Id</FieldNames></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertNull(obj.selectionCriteria);
    }

    @Test
    public void beanWithStringPresent() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SomeString>xxx</SomeString></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertThat(obj.someString, is("xxx"));
    }

    @Test
    public void beanWithStringAbsent() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertNull(obj.someString);
    }

    @Test
    public void beanWithStringEmptyTag() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SomeString /></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertNull(obj.someString);
    }

    @Test
    public void beanWithStringEmptyTag2() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SomeString></SomeString></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertThat(obj.someString, is(""));
    }

    @Test
    public void beanWithStringWhitespaces() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<GetRequest><SomeString> </SomeString></GetRequest>";
        GetRequest obj = xmlMapper.readValue(xml, GetRequest.class);
        assertThat(obj.someString, is(" "));
    }

    @Test
    public void beanAsIdStringWithSpaces() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Company>"
                + "<Employees id=\" \"></Employees>"
                + "<Employees id=\"xx\"><linked> </linked></Employees>"
                + "</Company>";
        Company obj = xmlMapper.readValue(xml, Company.class);
        assertThat(obj.employees, hasSize(2));
        assertThat(obj.employees.get(1).linked, is(obj.employees.get(0)));
    }
}
