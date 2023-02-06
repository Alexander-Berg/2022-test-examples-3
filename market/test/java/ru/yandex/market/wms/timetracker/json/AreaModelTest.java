package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.model.AreaModel;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class AreaModelTest {

    @Autowired
    private JacksonTester<AreaModel> tester;

    @Test
    public void canSerialize() throws IOException {
        final AreaModel expected = AreaModel.builder()
                .name("test")
                .build();

        final JsonContent<AreaModel> content = tester.write(expected);

        assertEquals(
                JsonParser.parseString(
                        FileContentUtils.getFileContent("json/area-dto/model.json")),
                JsonParser.parseString(content.getJson()));
    }

    @Test
    public void canDeserialize() throws IOException {
        final AreaModel expected = AreaModel.builder()
                .name("test")
                .build();

        final AreaModel result =
                tester.readObject("/json/area-dto/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }

}
