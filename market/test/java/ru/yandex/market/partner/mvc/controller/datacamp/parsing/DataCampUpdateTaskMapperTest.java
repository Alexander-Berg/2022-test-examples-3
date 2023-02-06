package ru.yandex.market.partner.mvc.controller.datacamp.parsing;

import Market.DataCamp.API.UpdateTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.datacamp.parsing.DataCampUpdateTaskMapper;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * Тесты для {@link DataCampUpdateTaskMapper}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DataCampUpdateTaskMapperTest extends FunctionalTest {

    @Autowired
    private DataCampUpdateTaskMapper dataCampUpdateTaskMapper;

    @Test
    @DisplayName("Для каждого типа фида в индексаторе есть соответствие в mbi")
    void testFeedContentType() {
        for (UpdateTask.FeedContentType type : UpdateTask.FeedContentType.values()) {
            FeedType actual = dataCampUpdateTaskMapper.mapFeedContentType(type, null);
            Assertions.assertNotNull(actual, type.toString());
        }
    }
}
