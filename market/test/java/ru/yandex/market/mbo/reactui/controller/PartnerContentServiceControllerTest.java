package ru.yandex.market.mbo.reactui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.reactui.dto.partner.ImageVerdictRequest;

import java.io.IOException;

public class PartnerContentServiceControllerTest {
    @Test
    public void testWhenDeserializingImageVerdictThenNoException()
        throws IOException {
        String json = "{\"urls\":[\"http://files.digit-style.ru/files/images_square/BG22085/6934065500080/10.jpg\"]}";
        ObjectMapper mapper = new ObjectMapper();

        ImageVerdictRequest imageVerdictRequest = mapper.reader().forType(ImageVerdictRequest.class).readValue(json);
        Assertions.assertThat(imageVerdictRequest.getUrls()).hasSize(1);
    }
}
