package ru.yandex.canvas.service.video;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.video.VideoFiles;
import ru.yandex.canvas.service.SandBoxService;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@RunWith(SpringJUnit4ClassRunner.class)
public class EventJsonTest {

    public void checkBean(SandBoxService.SandboxConversionTaskOutput sandboxConversionTaskOutput) {
        assertThat(sandboxConversionTaskOutput.getFormats(), hasSize(1));

        assertThat(sandboxConversionTaskOutput.getFormats().get(0), allOf(
                instanceOf(VideoFiles.VideoFormat.class),
                hasProperty("id", is("112398")),
                hasProperty("mimeType", is("application/mpeg4")),
                hasProperty("url", is("http://youtube.com"))
        ));
    }

    @Test
    public void test() throws IOException {
        String json = "{"
                + "\"formats\": [{\"id\":112398, \"type\":\"application/mpeg4\",\"url\":\"http://youtube.com\"}]"
                + "}";

        ObjectMapper objectMapper = new ObjectMapper();

        SandBoxService.SandboxConversionTaskOutput sandboxConversionTaskOutput = objectMapper.readValue(json,
                SandBoxService.SandboxConversionTaskOutput.class);

        checkBean(sandboxConversionTaskOutput);
    }


    @Test
    public void testString() throws IOException {
        String json = "{"
                + "\"formats\": \"[{\\\"id\\\":112398, \\\"type\\\":\\\"application/mpeg4\\\","
                + "\\\"url\\\":\\\"http://youtube.com\\\"}]\""
                + "}";

        ObjectMapper objectMapper = new ObjectMapper();

        SandBoxService.SandboxConversionTaskOutput sandboxConversionTaskOutput = objectMapper.readValue(json,
                SandBoxService.SandboxConversionTaskOutput.class);

        checkBean(sandboxConversionTaskOutput);
    }

    @Test
    public void testNull() throws IOException {
        String json = "{"
                + "\"formats\": null"
                + "}";

        ObjectMapper objectMapper = new ObjectMapper();

        SandBoxService.SandboxConversionTaskOutput sandboxConversionTaskOutput = objectMapper.readValue(json,
                SandBoxService.SandboxConversionTaskOutput.class);

        assertThat(sandboxConversionTaskOutput.getFormats(), Matchers.nullValue());
    }

}
