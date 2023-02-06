package ru.yandex.market.tpl.core.external.mbi;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TplCustomMbiObjectMapperBuilderTest {
    @Test
    void mappingWithMbiNameStrategy() throws IOException {
        //given
        ObjectMapper objectMapper = TplCustomMbiObjectMapperBuilder.build();
        var request = new TestDtoForMapper();
        request.setTestProperty("property");

        //when
        StringWriter sw = new StringWriter();
        objectMapper.writeValue(sw, request);

        //then
        String expected = "<test-dto-for-mapper><test-property>property</test-property></test-dto-for-mapper>";
        Assertions.assertEquals(expected, sw.toString());
    }

    @XmlRootElement(name = "testDtoForMapper")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class TestDtoForMapper {
        @XmlElement(name = "testProperty")
        private String testProperty;

        public String getTestProperty() {
            return testProperty;
        }
        public void setTestProperty(String testProperty) {
            this.testProperty = testProperty;
        }
    }
}
