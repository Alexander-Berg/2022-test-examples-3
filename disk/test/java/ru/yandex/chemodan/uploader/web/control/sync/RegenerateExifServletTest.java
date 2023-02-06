package ru.yandex.chemodan.uploader.web.control.sync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import ru.yandex.chemodan.uploader.processor.SyncRequestProcessor.ExtractedExifData;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.commune.json.serialize.JsonParser;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class RegenerateExifServletTest {

    @Test
    public void validateWriteResponse() throws IOException {
        String mid = "123:ya.disk";
        ExtractedExifData data = new ExtractedExifData(
                MulcaId.fromSerializedString(mid), "[{\"data1\": 1, \"data2\" : \"str\"}]");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        RegenerateExifServlet.writeResponse(baos, data);
        JsonObject resp = (JsonObject) JsonParser.getInstance().parse(baos.toString());
        Assert.equals(mid, ((JsonString) resp.get("mid")).getValue());
        Assert.isTrue(resp.get("exif") instanceof JsonObject);
    }

}
