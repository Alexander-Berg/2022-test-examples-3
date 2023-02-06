package ru.yandex.market.partner.notification.dao.theme;

import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.theme.model.NotificationTheme;

public class NotificationThemeDaoTest extends AbstractFunctionalTest {

    @Autowired
    NotificationThemeDao notificationThemeDao;

    @Test
    void testGetAllActiveThemes() {
        var themes = notificationThemeDao.getAllActiveSortedThemes();
        var ids = themes.stream().map(NotificationTheme::getId).collect(Collectors.toList());
        Assertions.assertThat(ids).containsExactly(1L, 8L, 10L, 5L, 11L, 3L, 12L, 7L, 13L);
    }
}
