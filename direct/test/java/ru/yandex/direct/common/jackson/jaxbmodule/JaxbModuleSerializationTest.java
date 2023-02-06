package ru.yandex.direct.common.jackson.jaxbmodule;

import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class JaxbModuleSerializationTest extends JaxbModuleTestBase {

    @Test
    public void serializeJsonElementAbsentWhenJaxbElementNull() throws IOException {
        ClassForInteger obj = new ClassForInteger();
        obj.jint2 = 22;
        String output = objectMapper.writeValueAsString(obj);
        String expected = "{\"jint2\":22}";
        assertThat(output, CoreMatchers.is(expected));
    }

    @Test
    public void serializeJsonFieldPresentAndNotNilWhenJaxbElementValue() throws IOException {
        ClassForInteger obj = new ClassForInteger();
        obj.jint = new JAXBElement<>(new QName(""), Integer.class, 11);
        obj.jint2 = 22;
        String output = objectMapper.writeValueAsString(obj);
        String expected = "{\"jint\":11,\"jint2\":22}";
        assertThat(output, CoreMatchers.is(expected));
    }

    @Test
    public void serializeJsonFieldPresentAndNullWhenJaxbElementNil() throws IOException {
        ClassForInteger obj = new ClassForInteger();
        obj.jint = new JAXBElement<>(new QName(""), Integer.class, null);
        obj.jint2 = 22;
        String output = objectMapper.writeValueAsString(obj);
        String expected = "{\"jint\":null,\"jint2\":22}";
        assertThat(output, CoreMatchers.is(expected));
    }

    @Test
    public void serializeJsonElementAbsentWhenAnnotatedJaxbElementNull() throws IOException {
        GetRequest obj = new GetRequest();
        String output = objectMapper.writeValueAsString(obj);
        String expected = "{\"NillableString\":null}";
        assertThat(output, CoreMatchers.is(expected));
    }

    @Test
    public void serializeJsonFieldPresentAndNotNilWhenAnnotatedJaxbElementValue() throws IOException {
        GetRequest obj = new GetRequest();
        obj.nillableBean =
                new JAXBElement<>(new QName("NillableBean"), SelectionCriteria.class, new SelectionCriteria());
        obj.nillableString = "xx";
        String output = objectMapper.writeValueAsString(obj);
        String expected = "{\"NillableBean\":{},\"NillableString\":\"xx\"}";
        assertThat(output, CoreMatchers.is(expected));
    }

    @Test
    public void serializeJsonFieldPresentAndNullWhenAnnotatedJaxbElementNil() throws IOException {
        GetRequest obj = new GetRequest();
        obj.nillableBean = new JAXBElement<>(new QName("xx"), SelectionCriteria.class, null);
        String output = objectMapper.writeValueAsString(obj);
        String expected = "{\"NillableBean\":null,\"NillableString\":null}";
        assertThat(output, CoreMatchers.is(expected));
    }

}
