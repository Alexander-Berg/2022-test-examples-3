package ru.yandex.travel.cpa.data_processing.flow.model.labels;

import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.travel.avia.proto.cpa.TLabel;

import static org.assertj.core.api.Assertions.assertThat;

class LabelJsonDecoderAviaTest {

    @org.junit.jupiter.api.Test
    void decodeItem() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        LabelJsonDecoderAvia decoder = new LabelJsonDecoderAvia();

        var root = mapper.createObjectNode();
        root.put("adult_seats", 1);
        root.put("children_seats", 1);
        root.put("infant_seats", 1);
        root.put("national_version", 1);
        root.put("offer_currency", "RUB");
        root.put("offer_price", 1000);
        root.put("pp", 100);
        root.put("price", 1000);
        root.put("when", "2022-01-01");
        root.put("return_date", "2022-01-01");
        root.put("klass", "");
        root.put("ytp_referer", "ytp_referer");

        var label = decoder.decodeItem(root.toString());
        var bytes = Base64.getDecoder().decode(label.getData());
        var l = TLabel.parseFrom(bytes);
        assertThat(l.getYtpReferer()).isEqualTo("ytp_referer");

        root.putObject("ytp_referer");
        label = decoder.decodeItem(root.toString());
        bytes = Base64.getDecoder().decode(label.getData());
        l = TLabel.parseFrom(bytes);
        assertThat(l.getYtpReferer()).isEqualTo("");
    }
}
