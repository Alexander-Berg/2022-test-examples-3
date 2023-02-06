package ru.yandex.market.tpl.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.controller.api.UserController;
import ru.yandex.market.tpl.api.facade.UserFacade;

import static org.assertj.core.api.Assertions.assertThat;

@WebLayerTest(UserController.class)
class TplApiWebContextTest extends BaseShallowTest {

    @MockBean
    private UserFacade userFacade;

    @Autowired
    private UserController userController;

    @Test
    void shouldStart() {
        assertThat(userController).isNotNull();
    }

}
