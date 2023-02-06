package ru.yandex.market.adv.yt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeKeyField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.adv.config.YtDynamicClientAutoconfiguration;
import ru.yandex.market.adv.config.YtStaticClientAutoconfiguration;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {
                YtDynamicClientAutoconfiguration.class,
                YtStaticClientAutoconfiguration.class
        }
)
@TestPropertySource(locations = "/application.properties")
class YtClientTest {

    private static final String ROOT = "//tmp";

    @Autowired
    private YtStaticClientFactory ytStaticClientFactory;

    @Autowired
    private YtDynamicClientFactory ytDynamicClientFactory;

    @Nonnull
    private static String generateRandomTablePath() {
        return ROOT + "/" + UUID.randomUUID();
    }

    @DisplayName("Создание/удаление динамической таблицы с записями")
    @Test
    void dynamicClient_CRUD_success() {
        YtClientProxy ytClient = ytDynamicClientFactory.createClient();
        String path = generateRandomTablePath();

        YTBinder<Table> binder = YTBinder.getBinder(Table.class);
        ytClient.createTable(path, binder, Map.of());
        ytClient.mountTable(path);

        Assertions.assertThat(ytClient.isPathExists(path))
                .isTrue();

        List<Table> rows = List.of(
                new Table(1, "test1"),
                new Table(2, "test2"),
                new Table(3, "test3")

        );

        ytClient.insertRows(path, binder, rows);

        Assertions.assertThat(ytClient.selectRows("* from [" + path + "]", binder))
                .containsExactlyInAnyOrderElementsOf(rows);

        ytClient.deleteRows(path, binder, rows);

        Assertions.assertThat(ytClient.selectRows("* from [" + path + "]", binder))
                .isEmpty();

        ytClient.deletePath(path);

        Assertions.assertThat(ytClient.isPathExists(path))
                .isFalse();
    }

    @DisplayName("Создание/удаление статической таблицы с записями")
    @Test
    void staticClient_CRUD_success() {
        YtClientProxy ytClient = ytStaticClientFactory.createClient();
        String path = generateRandomTablePath();

        YTBinder<Table> binder = YTBinder.getStaticBinder(Table.class);
        ytClient.createTable(path, binder, Map.of());

        Assertions.assertThat(ytClient.isPathExists(path))
                .isTrue();

        List<Table> rows = List.of(
                new Table(1, "test1"),
                new Table(2, "test2"),
                new Table(3, "test3")

        );

        ytClient.write(path, binder, rows);

        List<Table> actualRows = new ArrayList<>();
        ytClient.read(YPath.simple(path), binder, actualRows::add);

        Assertions.assertThat(actualRows)
                .containsExactlyInAnyOrderElementsOf(rows);

        ytClient.deletePath(path);

        Assertions.assertThat(ytClient.isPathExists(path))
                .isFalse();
    }

    @Data
    @YTreeObject
    @AllArgsConstructor
    public static class Table {
        @YTreeKeyField
        private int id;
        private String name;
    }
}
