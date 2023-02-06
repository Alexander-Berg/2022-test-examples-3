package ru.yandex.direct.common.jackson.jaxbmodule;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.Before;

@SuppressWarnings("checkstyle:visibilitymodifier")
public class JaxbModuleTestBase {
    protected XmlMapper xmlMapper;
    protected ObjectMapper objectMapper;

    @Before
    public void setUp() {
        JaxbModule jaxbModule = new JaxbModule();

        JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(false);
        xmlMapper = new XmlMapper(module);
        xmlMapper.registerModule(jaxbModule);
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(jaxbModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static class ClassForInteger {
        public JAXBElement<Integer> jint;
        public Integer jint2;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "nillableBean",
            "selectionCriteria",
            "fieldNames",
            "nillableBean"
    })
    @XmlRootElement(name = "GetRequest")
    public static class GetRequest {
        @XmlElementRef(name = "NillableBean", type = JAXBElement.class, required = false)
        public JAXBElement<SelectionCriteria> nillableBean;

        @XmlElement(name = "SelectionCriteria", required = true)
        public SelectionCriteria selectionCriteria;

        @XmlElement(name = "FieldNames", required = true)
        public List<SomeEnum> fieldNames;

        @XmlElement(name = "SomeString", required = false)
        public String someString;

        @XmlElement(name = "NillableString", required = true, nillable = true)
        public String nillableString;
    }

    @XmlType(name = "SomeEnum")
    @XmlEnum
    public enum SomeEnum {
        @XmlEnumValue("Id")
        ID
    }

    public static class SelectionCriteria {
        @XmlElement(name = "SomeX", required = true)
        public Integer someX;

        @XmlElement(name = "Ids", required = true)
        public List<Integer> ids;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "Company")
    public static class Company {
        @XmlElement(name = "Employees", required = true)
        List<Employee> employees;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Employee {
        @XmlAttribute
        @XmlID
        public String id;

        @XmlIDREF
        public Employee linked;
    }
}
