package ru.yandex.market.partner.mvc.controller.datacamp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.googlecode.protobuf.format.JsonFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.Magics;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.mvc.controller.datacamp.dto.CategoryDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.protobuf.tools.MagicChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;

class DataCampParametersControllerTest extends FunctionalTest {

    @Autowired
    private CategoryParametersParser parser;

    @DisplayName("Выдача ручки параметров категории по hid")
    @Test
    void getParamsController() throws IOException {
        doReturn(getProtobufStream("proto/categoryParameters.15727562.json")).when(parser).pathToStream(any());

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/v1/categories/15727562/fields");
        JsonTestUtil.assertEquals(response, getClass(), "CategoryParameters.Gurulight.json");

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () ->
                FunctionalTestHelper.get(baseUrl + "/categories/-1/fields"));
        assertEquals(exception.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @DisplayName("Парсинг параметров категории из протобуфа")
    @Test
    void checkParser() throws IOException {
        CategoryDTO guru = parser.parse(getProtobufStream("proto/categoryParameters.90534.json"));
        assertEquals(2, guru.getParameters().size());
        assertEquals(90534L, guru.getId().longValue());
        assertEquals("Очки", guru.getName());
        assertEquals("Назначение", guru.getParameters().get(0).getName());
        assertEquals("для водителей", guru.getParameters().get(0).getRestrictions().get(0));
        assertEquals("Материал линз", guru.getParameters().get(1).getName());
        assertEquals(2, guru.getParameters().get(1).getRestrictions().size());

        CategoryDTO gurulight = parser.parse(getProtobufStream("proto/categoryParameters.15727562.json"));
        assertEquals(3, gurulight.getParameters().size());
        assertEquals(15727562L, gurulight.getId().longValue());
        assertEquals("Полуфабрикаты из птицы", gurulight.getName());
        assertEquals("Производитель", gurulight.getParameters().get(0).getName());
        assertEquals("ENUM", gurulight.getParameters().get(0).getType());
        assertEquals(3, gurulight.getParameters().get(0).getRestrictions().size());
        assertEquals("Птица", gurulight.getParameters().get(2).getName());
    }

    private ByteArrayInputStream getProtobufStream(String path) throws IOException {
        String json = StringTestUtil.getString(this.getClass(), path);

        MboParameters.Category.Builder category = MboParameters.Category.newBuilder();
        JsonFormat.merge(json, category);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(MagicChecker.magicToBytes(Magics.MagicConstants.MBOC.name()));
        category.build().writeDelimitedTo(baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }
}
