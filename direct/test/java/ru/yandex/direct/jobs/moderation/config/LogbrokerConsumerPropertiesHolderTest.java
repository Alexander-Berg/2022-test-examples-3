package ru.yandex.direct.jobs.moderation.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.direct.jobs.moderation.config.LogbrokerConsumerPropertiesHolder.getTopicAlias;

class LogbrokerConsumerPropertiesHolderTest {
    @Test
    void getTopicAliasTest() {
        assertEquals("topic-name", getTopicAlias("topic-name"));
        assertEquals("topic-name", getTopicAlias("/topic-name"));
        assertEquals("topic-name", getTopicAlias("folder/topic-name"));
        assertEquals("", getTopicAlias("folder/topic-name/"));
        assertEquals("", getTopicAlias(""));
    }
}
