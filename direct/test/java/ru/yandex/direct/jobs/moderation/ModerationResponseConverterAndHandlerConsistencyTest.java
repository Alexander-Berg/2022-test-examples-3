package ru.yandex.direct.jobs.moderation;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.moderation.processor.handlers.ModerationResponseHandler;
import ru.yandex.direct.jobs.moderation.reader.support.ModerationResponseConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@JobsTest
@ExtendWith(SpringExtension.class)
class ModerationResponseConverterAndHandlerConsistencyTest {

    @Autowired
    private List<ModerationResponseConverter> moderationSupports;

    @Autowired
    private List<ModerationResponseHandler> handlers;

    @Test
    void checkSupportAndHandlerConsistency() {
        Set<ModerationObjectType> moderationSupportTypes = listToSet(moderationSupports,
                ModerationResponseConverter::getType);
        List<ModerationObjectType> handlerTypes = mapList(handlers, ModerationResponseHandler::getType);
        assertThat(handlerTypes).containsExactlyInAnyOrder(moderationSupportTypes.toArray(new ModerationObjectType[]{}));
    }

    @Test
    void checkSupportNotHaveMultipleTypes() {
        List<ModerationObjectType> supportTypes = mapList(moderationSupports, ModerationResponseConverter::getType);
        assertThat(supportTypes).doesNotHaveDuplicates();
    }

    @Test
    void checkHandlersNotHaveMultipleTypes() {
        List<ModerationObjectType> handlerTypes = mapList(handlers, ModerationResponseHandler::getType);
        assertThat(handlerTypes).doesNotHaveDuplicates();
    }
}
