package ru.yandex.market.adv.content.manager.mapper.template;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.adv.content.manager.model.template.TemplateKey;

/**
 * Date: 07.10.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
class TemplateKeyMapperImplTest extends AbstractContentManagerTest {

    @Autowired
    private TemplateKeyMapper templateKeyMapper;

    @DisplayName("Преобразование из ключа в строку-ключ прошло успешно.")
    @Test
    void map_correctKey_stringKey() {
        Assertions.assertThat(templateKeyMapper.map(TemplateKey.builder()
                        .businessId(421L)
                        .id(32194L)
                        .revisionId(969432L)
                        .type("EXPRESS")
                        .build()))
                .isEqualTo("421_32194_969432_EXPRESS");
    }

    @DisplayName("Преобразование из строки-ключа в ключ завершилось исключением.")
    @Test
    void map_wrongStringKey_exception() {
        Assertions.assertThatThrownBy(() -> templateKeyMapper.map("421_32194_969432EXPRESS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown key value - 421_32194_969432EXPRESS");
    }

    @DisplayName("Преобразование из строки-ключа в ключ прошло успешно.")
    @Test
    void map_correctStringKey_key() {
        Assertions.assertThat(templateKeyMapper.map("421_32194_969432_EXPRESS"))
                .isEqualTo(TemplateKey.builder()
                        .businessId(421L)
                        .id(32194L)
                        .revisionId(969432L)
                        .type("EXPRESS")
                        .build());
    }
}
