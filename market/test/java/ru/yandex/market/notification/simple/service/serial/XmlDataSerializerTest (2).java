package ru.yandex.market.notification.simple.service.serial;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import ru.yandex.market.notification.exception.ObjectSerializationException;
import ru.yandex.market.notification.simple.service.serial.XmlDataSerializer;
import ru.yandex.market.notification.test.util.DataSerializerUtils;
import ru.yandex.market.notification.simple.util.CollectionUtils;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link XmlDataSerializer}.
 *
 * @author Vladislav Bauer
 */
public class XmlDataSerializerTest {

    private static final XmlDataSerializer SERIALIZER = new XmlDataSerializer();

    private static final String TEST_VALUE = "test";
    private static final String CONTENT_EMPTY = "<data/>";
    private static final String CONTENT_SIMPLE = "<data><value>" + TEST_VALUE + "</value></data>";


    @Test
    public void testSerializeNullValue() {
        assertThat(serialize(TestData.create(null, emptySet())), equalTo(CONTENT_EMPTY));
    }

    @Test
    public void testSerializeNonNullValue() {
        assertThat(serialize(TestData.create(TEST_VALUE, emptySet())), equalTo(CONTENT_SIMPLE));
    }

    @Test
    public void testDeserializeEmpty() {
        final TestData actual = deserialize(CONTENT_EMPTY, TestData.class);
        final TestData expected = TestData.create(null, emptySet());

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testDeserializeNotEmpty() {
        final TestData actual = deserialize(CONTENT_SIMPLE, TestData.class);
        final TestData expected = TestData.create(TEST_VALUE, emptySet());

        assertThat(actual, equalTo(expected));
    }

    @Test(expected = ObjectSerializationException.class)
    public void testSerializeNegative() {
        final XmlDataSerializer serializer = new BadXmlDataSerializer();
        serializer.serialize(new Object());
    }

    @Test(expected = ObjectSerializationException.class)
    public void testDeserializeNegative() {
        final XmlDataSerializer serializer = new BadXmlDataSerializer();
        serializer.deserialize(new byte[] {}, Object.class);
    }


    private String serialize(final Object data) {
        final byte[] bytes = SERIALIZER.serialize(data);
        return DataSerializerUtils.toString(bytes);
    }

    private <T> T deserialize(final String content, final Class<T> objectClass) {
        final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return SERIALIZER.deserialize(bytes, objectClass);
    }


    @XmlRootElement(name = "data")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class TestData {

        private String value;
        private Collection<Integer> collection;


        @Nonnull
        static TestData create(@Nullable final String value, @Nullable final Collection<Integer> collection) {
            final TestData data = new TestData();
            data.setValue(value);
            data.setCollection(collection);
            return data;
        }


        @Nullable
        public String getValue() {
            return value;
        }

        public void setValue(@Nullable final String value) {
            this.value = value;
        }

        @Nonnull
        public Collection<Integer> getCollection() {
            return CollectionUtils.unmodifiableCollection(collection);
        }

        public void setCollection(@Nullable final Collection<Integer> collection) {
            this.collection = collection;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof TestData) {
                final TestData other = (TestData) obj;

                if (Objects.equals(other.getValue(), getValue())) {
                    final Collection<Integer> c1 = other.getCollection();
                    final Collection<Integer> c2 = getCollection();

                    return c1.containsAll(c2) && c2.containsAll(c1);
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(getValue());
        }

    }


    private static class BadXmlDataSerializer extends XmlDataSerializer {
        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        protected Marshaller createMarshaller(@Nonnull final Class<?> objectClass) throws JAXBException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        protected <T> Unmarshaller createUnmarshaller(@Nonnull final Class<T> objectClass) throws JAXBException {
            throw new UnsupportedOperationException();
        }
    }

}
