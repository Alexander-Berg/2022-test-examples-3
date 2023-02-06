package ru.yandex.market.logistics.lom.lms.converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.lom.AbstractTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация расписаний заборов из моделей yt в модели lms")
class InboundScheduleYtToLmsConverterTest extends AbstractTest {

    private final InboundScheduleYtToLmsConverter converter = new InboundScheduleYtToLmsConverter(objectMapper);

    @Test
    @DisplayName("Конвертация списка расписаний")
    void convertSchedulesList() {
        softly.assertThat(converter.extractScheduleIds("{\"schedules\":[123,456,789]}"))
            .isEqualTo(List.of(123L, 456L, 789L));
    }

    @Test
    @DisplayName("Конвертация пустого списка расписаний")
    void convertEmptySchedulesList() {
        softly.assertThat(converter.extractScheduleIds("{\"schedules\":[]}")).isEmpty();
    }

    @Test
    @DisplayName("Конвертация из YT итератора")
    void convertFromYtTreeMapNodeAndColumn() {

        String column = "schedules";
        Iterator<YTreeMapNode> iterator = Arrays.asList(
            getYTreeMapNode("{\"schedules\":[123,456,789]}", column),
            getYTreeMapNode("{\"schedules\":[17]}", column)
        ).iterator();

        softly.assertThat(converter.convert(iterator, column))
            .containsExactlyInAnyOrderElementsOf(List.of(123L, 456L, 789L, 17L));
    }

    @Test
    @DisplayName("Конвертация из пустого YT итератора")
    void convertFromYtTreeMapNodeEmptyAndColumn() {
        softly.assertThat(converter.convert(Collections.emptyIterator(), "any"))
            .isEmpty();
    }

    @Nonnull
    private YTreeMapNode getYTreeMapNode(String value, String column) {
        YTreeMapNode yTreeMapNode = mock(YTreeMapNode.class);
        doReturn(value).when(yTreeMapNode).getString(eq(column));
        return yTreeMapNode;
    }
}
