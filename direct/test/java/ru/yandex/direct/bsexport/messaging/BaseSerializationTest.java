package ru.yandex.direct.bsexport.messaging;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import static org.assertj.core.api.Assertions.assertThatCode;

public abstract class BaseSerializationTest {

    private JsonFormat.Printer printer;
    private SoapSerializer soapSerializer;

    protected String json;
    protected String soap;

    public BaseSerializationTest() {
        assertThatCode(() -> {
            soapSerializer = new SoapSerializer();
            printer = FeedJsonSerializer.getPrinter();
        }).doesNotThrowAnyException();
    }

    protected void serialize(Message message) {
        assertThatCode(() -> {
            json = printer.print(message);
            soap = soapSerializer.serializeRoot(message);
        }).doesNotThrowAnyException();
    }
}
