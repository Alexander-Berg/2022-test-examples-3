package ru.yandex.market.mbi.jaxb.jackson;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EmptyCollectionDeserializationTest {

    private static final String DATA = "<test-collection-holder><ids/></test-collection-holder>";

    private ObjectMapper xmlMapper = new ApiObjectMapperFactory().createXmlMapper();

    @Test
    public void testEmptyListDeserialization() throws IOException {
        TestListHolder testListHolder = xmlMapper.readValue(DATA, TestListHolder.class);

        assertTrue(testListHolder.getList() != null);
        assertEquals(0, testListHolder.getList().size());
    }

    @Test
    public void testEmptySetDeserialization() throws IOException {
        TestSetHolder testSetHolder = xmlMapper.readValue(DATA, TestSetHolder.class);

        assertTrue(testSetHolder.getSet() != null);
        assertEquals(0, testSetHolder.getSet().size());
    }

    @Test
    public void testEmptyCollectionDeserialization() throws IOException {
        TestCollectionHolder testCollectionHolder = xmlMapper.readValue(DATA, TestCollectionHolder.class);

        assertTrue(testCollectionHolder.getCollection() != null);
        assertEquals(0, testCollectionHolder.getCollection().size());
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "testCollectionHolder")
    private static class TestListHolder {

        @XmlElementWrapper(name = "ids")
        @XmlElement(name = "id")
        private List<BigInteger> list;

        public TestListHolder() {
        }

        public List<BigInteger> getList() {
            return list;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "testCollectionHolder")
    private static class TestSetHolder {

        @XmlElementWrapper(name = "ids")
        @XmlElement(name = "id")
        private Set<BigInteger> set;

        public TestSetHolder() {
        }

        public Set<BigInteger> getSet() {
            return set;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "testCollectionHolder")
    private static class TestCollectionHolder {

        @XmlElementWrapper(name = "ids")
        @XmlElement(name = "id")
        private Collection<BigInteger> collection;

        public TestCollectionHolder() {
        }

        public Collection<BigInteger> getCollection() {
            return collection;
        }
    }

}
