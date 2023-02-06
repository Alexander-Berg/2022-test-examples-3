package ru.yandex.market.tpl.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
@CoreTest
class TplCoreContextTest {

    @Autowired
    private UserRepository testBean;

    @Test
    void shouldStart() {
        assertThat(testBean).isNotNull();
    }

}
