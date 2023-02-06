package ru.yandex.market.logistics.management.domain.converter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.node.POJONode;
import com.mysema.commons.lang.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.UserActivityDetailDto;
import ru.yandex.market.logistics.management.domain.entity.UserActivityLog;

class UserActivityDetailDtoConverterTest extends AbstractTest {
    private static final String REQUEST_PARAMS = "{\"first\":\"c\",\"second\":\"d\"}";
    private static final String TEXT = "{\"first\":\"a\",\"second\":\"b\"}";

    private UserActivityDetailDtoConverter converter;

    @BeforeEach
    void before() {
        this.converter = new UserActivityDetailDtoConverter();
    }

    @Test
    void converterTest() {
        LocalDateTime time = LocalDateTime.now();
        UserActivityLog entity = new UserActivityLog()
            .setId(1L)
            .setLogin("testLogin")
            .setProject("project")
            .setRequestBody(new POJONode(Pair.of("a", "b")))
            .setRequestParams(new POJONode(Pair.of("c", "d")))
            .setResponseStatus(200)
            .setRequestMethod("POST")
            .setRequestUri("/some/special/url/path")
            .setHandlerName(UserActivityDetailDtoConverterTest.class.toGenericString())
            .setCreated(time);

        UserActivityDetailDto expectedDto = new UserActivityDetailDto(
            1L,
            "Детали запроса",
            "testLogin",
            200,
            "project",
            "POST",
            "/some/special/url/path",
            FormattedTextObject.of(REQUEST_PARAMS),
            FormattedTextObject.of(TEXT),
            FormattedTextObject.of(UserActivityDetailDtoConverterTest.class.toGenericString()),
            time
        );

        UserActivityDetailDto actual = converter.fromEntity(entity);
        softly.assertThat(actual).as("Converted Entity").isEqualTo(expectedDto);
    }
}
