package ru.yandex.crypta.service.entity.extractor;

import java.io.IOException;
import java.net.URISyntaxException;

import com.google.protobuf.util.JsonFormat;
import org.junit.Test;

public class EntityExtractorTest {
    @Test
    public void testIsValid() throws IOException, URISyntaxException {
        System.out.println(JsonFormat.printer().print(YtEntityExtractorService.getExtractorConfig()));
    }
}
