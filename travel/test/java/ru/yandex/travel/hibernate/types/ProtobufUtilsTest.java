package ru.yandex.travel.hibernate.types;


import org.junit.Before;
import org.junit.Test;
import ru.yandex.travel.hibernate.types.ProtobufUtils;
import ru.yandex.travel.test.fake.proto.TTestMethodReq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.*;

public class ProtobufUtilsTest {

    private TTestMethodReq testMessage;

    @Before
    public void setUp() {
        testMessage = createMessage();
    }

    @Test
    public void testMessageDeserialize() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        testMessage.writeTo(bos);
        byte[] bytes = bos.toByteArray();
        Object parsedObject = ProtobufUtils.parseFrom(TTestMethodReq.class, bytes);
        assertThat(parsedObject).isEqualTo(testMessage);
    }


    private TTestMethodReq createMessage() {
        return TTestMethodReq.newBuilder().setTestValue("Test error").build();
    }

}
