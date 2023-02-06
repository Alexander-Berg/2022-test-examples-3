package ru.yandex.market.adv.yt.test.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.market.adv.config.CommonBeanAutoconfiguration;
import ru.yandex.market.adv.config.YtDynamicClientAutoconfiguration;
import ru.yandex.market.adv.config.YtStaticClientAutoconfiguration;
import ru.yandex.market.adv.test.AbstractTest;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.adv.yt.test.configuration.TestConfiguration;
import ru.yandex.market.adv.yt.test.configuration.YtTestConfiguration;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Date: 11.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@ExtendWith(YtExtension.class)
@SpringBootTest(
        classes = {
                CommonBeanAutoconfiguration.class,
                YtDynamicClientAutoconfiguration.class,
                YtStaticClientAutoconfiguration.class,
                JacksonAutoConfiguration.class,
                YtTestConfiguration.class,
                TestConfiguration.class
        }
)
@TestPropertySource(locations = "/application.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class YtExtensionTest extends AbstractTest {

    private static final String STATIC_TABLE_PATH = "//tmp/test-table-static";
    private static final String DYNAMIC_TABLE_PATH = "//tmp/test-table-dynamic";
    private static final String STATIC_ANOTHER_TABLE_PATH = "//tmp/test-another-table-static";
    private static final String STATIC_ANOTHER_TABLE_ADD_PATH = "//tmp/test-another-table-add-static";
    private static final String DYNAMIC_ANOTHER_TABLE_PATH = "//tmp/test-another-table-dynamic";
    private static final String DYNAMIC_ANOTHER_TABLE_ADD_PATH = "//tmp/test-another-table-add-dynamic";

    private static final YTBinder<Table> DYNAMIC_TABLE_BINDER =
            YTBinder.getBinder(Table.class);
    private static final YTBinder<AnotherTable> DYNAMIC_ANOTHER_TABLE_BINDER =
            YTBinder.getBinder(AnotherTable.class);
    private static final YTBinder<Table> STATIC_TABLE_BINDER =
            YTBinder.getStaticBinder(Table.class);
    private static final YTBinder<AnotherTable> STATIC_ANOTHER_TABLE_BINDER =
            YTBinder.getStaticBinder(AnotherTable.class);

    @Autowired
    @Qualifier("ytStaticClient")
    private YtClientProxy ytStaticClient;
    @Autowired
    @Qualifier("ytDynamicClient")
    private YtClientProxy ytDynamicClient;

    @DisplayName("С аннотациями корректно создались и заполнились статические и динамические таблицы.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Table.class,
                    path = STATIC_TABLE_PATH,
                    isDynamic = false
            ),
            before = "YtExtensionTest/json/yt/table/" +
                    "ytStaticAndDynamic_createAndFillByAnnotation_tableWithDataExist.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = DYNAMIC_ANOTHER_TABLE_PATH
            )
    )
    @Order(1)
    void ytStaticAndDynamic_createAndFillByAnnotation_tableWithDataExist() {
        assertStaticTable(
                STATIC_TABLE_PATH,
                STATIC_TABLE_BINDER,
                List.of(
                        new Table(
                                123,
                                "EGO",
                                Map.of(
                                        "before", new YTreeBooleanNodeImpl(true, Map.of()),
                                        "after", new YTreeStringNodeImpl("String", Map.of()),
                                        "int", new YTreeIntegerNodeImpl(true, 423L, Map.of())
                                )
                        ),
                        new Table(154, "UOQ", Map.of())
                )
        );
        assertDynamicTable(DYNAMIC_ANOTHER_TABLE_PATH, DYNAMIC_ANOTHER_TABLE_BINDER, List.of());
    }

    @DisplayName("Без создания таблиц через аннотации проверка на существование отработала корректно.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Table.class,
                    path = DYNAMIC_TABLE_PATH
            ),
            create = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = STATIC_ANOTHER_TABLE_PATH,
                    isDynamic = false
            ),
            create = false
    )
    @Order(2)
    void ytStaticAndDynamic_annotationWithoutCreate_tableExist() {
        ytDynamicClient.createTable(DYNAMIC_TABLE_PATH, DYNAMIC_TABLE_BINDER, Map.of());
        ytDynamicClient.mountTable(DYNAMIC_TABLE_PATH);

        ytStaticClient.createTable(STATIC_ANOTHER_TABLE_PATH, STATIC_ANOTHER_TABLE_BINDER, Map.of());

        Assertions.assertThat(ytDynamicClient.isPathExists(DYNAMIC_TABLE_PATH))
                .isTrue();
        Assertions.assertThat(ytStaticClient.isPathExists(STATIC_ANOTHER_TABLE_PATH))
                .isTrue();
    }

    @DisplayName("Без создания таблиц через аннотации и проверки на их отсутствие, таблицы не были обнаружены.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Table.class,
                    path = DYNAMIC_TABLE_PATH
            ),
            create = false,
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = STATIC_ANOTHER_TABLE_PATH,
                    isDynamic = false
            ),
            create = false,
            exist = false
    )
    @Order(3)
    void ytStaticAndDynamic_annotationWithoutCreateAndExist_tableNotExist() {
        Assertions.assertThat(ytDynamicClient.isPathExists(DYNAMIC_TABLE_PATH))
                .isFalse();
        Assertions.assertThat(ytStaticClient.isPathExists(STATIC_ANOTHER_TABLE_PATH))
                .isFalse();
    }

    @DisplayName("При проверки на отсутствие таблиц, они не были обнаружены.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Table.class,
                    path = DYNAMIC_TABLE_PATH
            ),
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = STATIC_ANOTHER_TABLE_PATH,
                    isDynamic = false
            ),
            exist = false
    )
    @Order(4)
    void ytStaticAndDynamic_annotationWithoutExist_tableNotExist() {
        ytDynamicClient.deletePath(DYNAMIC_TABLE_PATH);
        ytStaticClient.deletePath(STATIC_ANOTHER_TABLE_PATH);

        Assertions.assertThat(ytDynamicClient.isPathExists(DYNAMIC_TABLE_PATH))
                .isFalse();
        Assertions.assertThat(ytStaticClient.isPathExists(STATIC_ANOTHER_TABLE_PATH))
                .isFalse();
    }

    @DisplayName("С аннотациями корректно создались и заполнились статические таблицы.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Table.class,
                    path = STATIC_TABLE_PATH,
                    isDynamic = false
            ),
            before = "YtExtensionTest/json/yt/table/" +
                    "ytStatic_fullTableAndEmptyTable_tableWithDataExist.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = STATIC_ANOTHER_TABLE_PATH,
                    isDynamic = false
            ),
            create = false,
            after = "YtExtensionTest/json/yt/anotherTable/" +
                    "ytStatic_fullTableAndEmptyTable_tableWithDataExist.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = STATIC_TABLE_PATH
            ),
            before = "YtExtensionTest/json/yt/table/" +
                    "ytStatic_fullTableAndEmptyTable_tableWithDataExist.before.json",
            exist = false
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = STATIC_ANOTHER_TABLE_ADD_PATH,
                    isDynamic = false
            )
    )
    @Order(5)
    void ytStatic_fullTableAndEmptyTable_tableWithDataExist() {
        ytStaticClient.createTable(STATIC_ANOTHER_TABLE_PATH, STATIC_ANOTHER_TABLE_BINDER, Map.of());

        List<AnotherTable> anotherTableRows = List.of(
                new AnotherTable(1, "test1", Map.of("1", new YTreeBooleanNodeImpl(false, Map.of()))),
                new AnotherTable(2, "test2", Map.of("2", new YTreeStringNodeImpl("Str", Map.of()))),
                new AnotherTable(3, "test3", Map.of("3", new YTreeIntegerNodeImpl(true, 40L, Map.of())))
        );
        ytStaticClient.write(STATIC_ANOTHER_TABLE_PATH, STATIC_ANOTHER_TABLE_BINDER, anotherTableRows);

        assertStaticTable(
                STATIC_ANOTHER_TABLE_PATH,
                STATIC_ANOTHER_TABLE_BINDER,
                anotherTableRows
        );
        assertStaticTable(
                STATIC_TABLE_PATH,
                STATIC_TABLE_BINDER,
                List.of(
                        new Table(
                                95,
                                "AGA",
                                Map.of(
                                        "before", new YTreeBooleanNodeImpl(false, Map.of())
                                )
                        ),
                        new Table(443, "1", Map.of())
                )
        );
        assertStaticTable(
                STATIC_ANOTHER_TABLE_ADD_PATH,
                STATIC_ANOTHER_TABLE_BINDER,
                List.of()
        );
    }

    @DisplayName("С аннотациями корректно создались и заполнились динамические таблицы.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Table.class,
                    path = DYNAMIC_TABLE_PATH
            ),
            before = "YtExtensionTest/json/yt/table/" +
                    "ytDynamic_fullTableAndEmptyTable_tableWithDataExist.before.json",
            after = "YtExtensionTest/json/yt/table/" +
                    "ytDynamic_fullTableAndEmptyTable_tableWithDataExist.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = DYNAMIC_ANOTHER_TABLE_PATH,
                    ignoreColumns = "id"
            ),
            create = false,
            after = "YtExtensionTest/json/yt/anotherTable/" +
                    "ytDynamic_fullTableAndEmptyTable_tableWithDataExist.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AnotherTable.class,
                    path = DYNAMIC_ANOTHER_TABLE_ADD_PATH
            ),
            before = "YtExtensionTest/json/yt/anotherTable/" +
                    "ytDynamic_fullTableAndEmptyTable_tableWithDataExist.add.before.json",
            after = "YtExtensionTest/json/yt/anotherTable/" +
                    "ytDynamic_fullTableAndEmptyTable_tableWithDataExist.add.after.json"
    )
    @Order(6)
    void ytDynamic_fullTableAndEmptyTable_tableWithDataExist() {
        ytDynamicClient.createTable(DYNAMIC_ANOTHER_TABLE_PATH, DYNAMIC_ANOTHER_TABLE_BINDER, Map.of());
        ytDynamicClient.mountTable(DYNAMIC_ANOTHER_TABLE_PATH);

        List<AnotherTable> anotherTableRows = List.of(
                new AnotherTable(111, "test1d", Map.of("1d", new YTreeBooleanNodeImpl(false, Map.of()))),
                new AnotherTable(222, "test2d", Map.of("2d", new YTreeStringNodeImpl("Strd", Map.of()))),
                new AnotherTable(332, "test3d", Map.of("3d", new YTreeIntegerNodeImpl(true, 42L, Map.of())))
        );
        ytDynamicClient.insertRows(DYNAMIC_ANOTHER_TABLE_PATH, DYNAMIC_ANOTHER_TABLE_BINDER, anotherTableRows);

        ytDynamicClient.insertRows(
                DYNAMIC_ANOTHER_TABLE_ADD_PATH,
                DYNAMIC_ANOTHER_TABLE_BINDER,
                List.of(
                        new AnotherTable(42, "new", Map.of(";", new YTreeBooleanNodeImpl(false, Map.of()))),
                        new AnotherTable(54, "NEW", Map.of())
                )
        );

        assertDynamicTable(
                DYNAMIC_ANOTHER_TABLE_PATH,
                DYNAMIC_ANOTHER_TABLE_BINDER,
                anotherTableRows
        );
        assertDynamicTable(
                DYNAMIC_TABLE_PATH,
                DYNAMIC_TABLE_BINDER,
                List.of(
                        new Table(
                                44,
                                "test44d",
                                Map.of(
                                        "before", new YTreeBooleanNodeImpl(true, Map.of())
                                )
                        ),
                        new Table(55, "1", Map.of())
                )
        );
        assertDynamicTable(
                DYNAMIC_ANOTHER_TABLE_ADD_PATH,
                DYNAMIC_ANOTHER_TABLE_BINDER,
                List.of(
                        new AnotherTable(964, "old", Map.of(".", new YTreeBooleanNodeImpl(true, Map.of()))),
                        new AnotherTable(885, "OLD", Map.of()),
                        new AnotherTable(42, "new", Map.of(";", new YTreeBooleanNodeImpl(false, Map.of()))),
                        new AnotherTable(54, "NEW", Map.of())
                )
        );
    }

    @DisplayName("Без аннотаций не создалось ни одной статической или динамической таблицы.")
    @Test
    @Order(7)
    void ytStaticAndDynamic_withoutAnnotation_tableNotExist() {
        Assertions.assertThat(ytStaticClient.isPathExists(STATIC_TABLE_PATH))
                .isFalse();
        Assertions.assertThat(ytDynamicClient.isPathExists(DYNAMIC_TABLE_PATH))
                .isFalse();
        Assertions.assertThat(ytStaticClient.isPathExists(STATIC_ANOTHER_TABLE_PATH))
                .isFalse();
        Assertions.assertThat(ytStaticClient.isPathExists(STATIC_ANOTHER_TABLE_ADD_PATH))
                .isFalse();
        Assertions.assertThat(ytDynamicClient.isPathExists(DYNAMIC_ANOTHER_TABLE_PATH))
                .isFalse();
        Assertions.assertThat(ytDynamicClient.isPathExists(DYNAMIC_ANOTHER_TABLE_ADD_PATH))
                .isFalse();
    }

    private <T> void assertStaticTable(String path, YTBinder<T> binder,
                                       List<T> expectedRows) {
        Assertions.assertThat(ytStaticClient.isPathExists(path))
                .isTrue();

        List<T> actualRows = new ArrayList<>();
        ytStaticClient.read(YPath.simple(path), binder, actualRows::add);

        Assertions.assertThat(actualRows)
                .containsExactlyInAnyOrderElementsOf(expectedRows);
    }

    private <T> void assertDynamicTable(String path, YTBinder<T> binder,
                                        List<T> expectedRows) {
        Assertions.assertThat(ytDynamicClient.isPathExists(path))
                .isTrue();
        Assertions.assertThat(
                        ytDynamicClient.selectRows(
                                "* from [" + path + "]",
                                binder
                        )
                )
                .containsExactlyInAnyOrderElementsOf(expectedRows);
    }
}
