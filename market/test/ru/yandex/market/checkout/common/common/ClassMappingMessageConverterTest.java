package ru.yandex.market.checkout.common.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassMappingMessageConverterTest {

    private MyClassMappingMessageConverter converter;

    @BeforeEach
    public void setUp() throws Exception {
        converter = new MyClassMappingMessageConverter();
        converter.setDeserializers(
                new ClassMapping<Deserializer>() {{
                    setDefaultMapping(
                            new Deserializer() {{
                                message = "default";
                            }}
                    );
                    setMapping(
                            new HashMap<Class, Deserializer>() {{
                                put(String.class, new Deserializer() {{
                                    message = "string";
                                }});
                                put(SomeObject.class, new Deserializer() {{
                                    message = "someObject";
                                }});
                            }}
                    );
                }}
        );
        converter.setSerializers(
                new ClassMapping<Serializer>() {{
                    setDefaultMapping(
                            new Serializer() {{
                                message = "default";
                            }}
                    );
                    setMapping(
                            new HashMap<Class, Serializer>() {{
                                put(String.class, new Serializer() {{
                                    message = "string";
                                }});
                                put(SomeObject.class, new Serializer() {{
                                    message = "someObject";
                                }});
                            }}
                    );
                }}
        );

    }

    @Test
    public void testSerializeString() throws Exception {
        final HttpOutputMessage message = mock(HttpOutputMessage.class);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(message.getBody()).thenReturn(baos);
        converter.writeInternal("blahblah", message);

        assertEquals("string blahblah", new String(baos.toByteArray()));
    }

    @Test
    public void testSerializeSomeObject() throws Exception {
        final HttpOutputMessage message = mock(HttpOutputMessage.class);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(message.getBody()).thenReturn(baos);

        final SomeObject someObject = new SomeObject();
        someObject.message = "blah";
        converter.writeInternal(someObject, message);

        assertEquals("someObject blah", new String(baos.toByteArray()));
    }

    @Test
    public void testSerializeSomeObjectSubclass() throws Exception {
        final HttpOutputMessage message = mock(HttpOutputMessage.class);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(message.getBody()).thenReturn(baos);

        final SomeObject someObject = new SomeObject() {{
            message = "blah";
        }};
        converter.writeInternal(someObject, message);

        assertEquals("someObject blah", new String(baos.toByteArray()));
    }

    @Test
    public void testDeserializeString() throws Exception {
        final HttpInputMessage message = mock(HttpInputMessage.class);
        when(message.getBody()).thenReturn(new ByteArrayInputStream("blah".getBytes()));
        final Object result = converter.readInternal(String.class, message);

        assertEquals("string blah", result);
    }

    @Test
    public void testDeserializeSomeObject() throws Exception {
        final HttpInputMessage message = mock(HttpInputMessage.class);
        when(message.getBody()).thenReturn(new ByteArrayInputStream("omg".getBytes()));
        final Object result = converter.readInternal(SomeObject.class, message);

        assertEquals("someObject omg", result);
    }

    class SomeObject {

        String message;

        @Override
        public String toString() {
            return message;
        }
    }

    class Serializer {

        String message;

        void serialize(Object o, OutputStream outputStream) throws IOException {
            String result = message + " " + o.toString();
            outputStream.write(result.getBytes());
        }
    }

    class Deserializer {

        String message;

        String deserialize(InputStream inputStream) throws IOException {
            final byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);

            return message + " " + new String(bytes);
        }
    }

    class MyClassMappingMessageConverter extends ClassMappingMessageConverter<Serializer, Deserializer> {

        MyClassMappingMessageConverter() {
            super(Arrays.asList(MediaType.APPLICATION_JSON));
        }

        @Override
        public void serialize(Serializer serializer, Object o, OutputStream outputStream,
                              HttpOutputMessage outputMessage) throws IOException {
            serializer.serialize(o, outputStream);
        }

        @Override
        public Object deserialize(Deserializer deserializer, Class clazz, InputStream inputStream,
                                  HttpInputMessage inputMessage) throws IOException {
            return deserializer.deserialize(inputStream);
        }
    }

}
