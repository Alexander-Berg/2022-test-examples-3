package ru.yandex.market.partner.notification.api;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.self.api.ThemesApiClient;
import ru.yandex.mj.generated.client.self.model.GetThemesResponse;
import ru.yandex.mj.generated.client.self.model.NotificationThemeDTO;

public class ThemesApiServiceTest extends AbstractFunctionalTest {
    @Autowired
    ThemesApiClient themesApiClient;

    @Test
    void getAllThemes() throws ExecutionException, InterruptedException {
        GetThemesResponse body = themesApiClient.getActiveThemes().scheduleResponse().get().body();
        Assertions.assertThat(body).isNotNull();
        List<Long> ids = body.getItems().stream().map(NotificationThemeDTO::getId).collect(Collectors.toList());
        Assertions.assertThat(ids).containsExactly(1L, 8L, 10L, 5L, 11L, 3L, 12L, 7L, 13L);
    }
}
