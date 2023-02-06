package ru.yandex.market.core.tanker.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMessageTest {
    @Test
    void defaultValues() {
        // given-when
        var msg = new UserMessage.Builder()
                .setMessageCode("code")
                .setDefaultTranslation(null)
                .setMustacheArguments(null)
                .build();

        // then
        assertThat(msg.messageCode()).isEqualTo("code");
        assertThat(msg.defaultTranslation()).isEmpty();
        assertThat(msg.mustacheArguments()).isEqualTo("{}");
    }

    @Test
    void simpleValues() {
        // given-when
        var msg = new UserMessage.Builder()
                .setMessageCode("code")
                .setDefaultTranslation("tr")
                .setMustacheArguments("{\"some\":1}")
                .build();

        // then
        assertThat(msg.defaultTranslation()).isEqualTo("tr");
        assertThat(msg.mustacheArguments()).isEqualTo("{\"some\":1}");
    }

    @Test
    void multipleSets() {
        // given-when
        var msg = new UserMessage.Builder()
                .setMessageCode("code")
                .setDefaultTranslation("tr")
                .setMustacheArguments("{\"some\":1}")
                .setMessageCode("code2")
                .setDefaultTranslation(null)
                .setMustacheArguments(null)
                .build();

        // then
        assertThat(msg.messageCode()).isEqualTo("code2");
        assertThat(msg.defaultTranslation()).isEmpty();
        assertThat(msg.mustacheArguments()).isEqualTo("{}");
    }

    @Test
    void fromException() {
        // given-when
        var msg = UserMessage.fromException(
                "code",
                "msg",
                new RuntimeException("something-private")
        );

        // then
        assertThat(msg.messageCode()).isEqualTo("code");
        assertThat(msg.defaultTranslation()).isEqualTo("msg: {{details}}");
        assertThat(msg.mustacheArguments()).isEqualTo("{\"details\":\"Валидация окончилась ошибкой: RuntimeException\"}");
    }
}
