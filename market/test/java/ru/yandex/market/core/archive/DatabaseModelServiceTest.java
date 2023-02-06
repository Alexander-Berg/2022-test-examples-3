package ru.yandex.market.core.archive;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.archive.model.DatabaseModel;
import ru.yandex.market.core.archive.model.Key;
import ru.yandex.market.core.archive.model.KeyPart;
import ru.yandex.market.core.archive.model.Relation;
import ru.yandex.market.core.archive.model.RelationConfig;
import ru.yandex.market.core.archive.model.TableModel;
import ru.yandex.market.core.archive.type.KeyColumnType;
import ru.yandex.market.core.archive.type.RelationRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DatabaseModelServiceTest extends ArchivingFunctionalTest {
    @Autowired
    private DatabaseModelService databaseModelService;

    @Test
    @DisplayName("Невалидная точка входа: null")
    @DbUnitDataSet
    void testModelNullEntryPoint() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> databaseModelService.getDatabaseModel(null, new RelationConfig()));
    }

    @Test
    @DisplayName("Получение DatabaseModel для таблицы без связей (единственная таблица в схеме)")
    @DbUnitDataSet
    void testModel01() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch01.single_table", new RelationConfig());
        List<TableModel> expected = List.of(
                makeTableModel("sch01.single_table", getDefaultPk("sch01.single_table", KeyColumnType.NUMBER), List.of())
        );
        checkDatabaseModel(expected, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для таблицы без связей (несколько таблиц в схеме)")
    @DbUnitDataSet
    void testModel02() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch02.first_table", new RelationConfig());
        List<TableModel> expected = List.of(
                makeTableModel("sch02.first_table", getDefaultPk("sch02.first_table", KeyColumnType.NUMBER), List.of())
        );
        checkDatabaseModel(expected, model);

        model = databaseModelService.getDatabaseModel("sch02.second_table", new RelationConfig());
        expected = List.of(
                makeTableModel("sch02.second_table", getDefaultPk("sch02.second_table", KeyColumnType.NUMBER), List.of())
        );
        checkDatabaseModel(expected, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для двух связанных таблиц")
    @DbUnitDataSet
    void testModel03() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch03.first_table", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch03.first_table", getDefaultPk("sch03.first_table", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch03.second_table", getDefaultPk("sch03.second_table", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch03.first_table", "fk_sch_sectab_firtabid_sch_firtab_id", "id", "first_table_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);

        model = databaseModelService.getDatabaseModel("sch03.second_table", new RelationConfig());
        expectedModel = List.of(
                makeTableModel("sch03.second_table", getDefaultPk("sch03.second_table", KeyColumnType.NUMBER), List.of())
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для таблицы, которая связана с таблицей с двумя fk")
    @DbUnitDataSet
    void testModel04() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch04.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch04.table01", getDefaultPk("sch04.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch04.table03", getDefaultPk("sch04.table03", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch04.table01", "fk_sch_tab1_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);

        model = databaseModelService.getDatabaseModel("sch04.table02", new RelationConfig());
        expectedModel = List.of(
                makeTableModel("sch04.table02", getDefaultPk("sch04.table02", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch04.table03", getDefaultPk("sch04.table03", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch04.table02", "fk_sch_tab2_tabid_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для таблицы с вложенностью fk > 1")
    @DbUnitDataSet
    void testModel05() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch05.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch05.table01", getDefaultPk("sch05.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch05.table02", getDefaultPk("sch05.table02", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch05.table01", "fk_sch_tab2_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch05.table03", getDefaultPk("sch05.table03", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch05.table02", "fk_sch_tab3_tabid_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch05.table04", getDefaultPk("sch05.table04", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch05.table03", "fk_sch_tab4_tabid_sch_tab_id", "id", "table03_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch05.table05", getDefaultPk("sch05.table05", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch05.table02", "fk_sch_tab5_tabid_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для таблицы с двумя fk на нее из разных таблиц")
    @DbUnitDataSet
    void testModel06() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch06.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch06.table01", getDefaultPk("sch06.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch06.table02", getDefaultPk("sch06.table02", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch06.table01", "fk_sch_tab_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch06.table03", getDefaultPk("sch06.table03", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch06.table01", "fk_sch_tab3_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для таблицы с двумя fk на нее из одной таблицы")
    @DbUnitDataSet
    void testModel07() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch07.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch07.table01", getDefaultPk("sch07.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch07.table02", getDefaultPk("sch07.table02", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch07.table01", "fk_sch_tab_tabid1_sch_tab_id", "id", "table01_id1", RelationRule.NO_ACTION),
                                Relation.of("sch07.table01", "fk_sch_tab_tabid2_sch_tab_id", "id", "table01_id2", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для двух связанных таблиц. Fk: string type")
    @DbUnitDataSet
    void testModel10() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch10.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch10.table01", getDefaultPk("sch10.table01", KeyColumnType.STRING), List.of()),
                makeTableModel("sch10.table02", getDefaultPk("sch10.table02", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch10.table01", "fk_sch_tab2_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);

        model = databaseModelService.getDatabaseModel("sch10.table03", new RelationConfig());
        expectedModel = List.of(
                makeTableModel("sch10.table03", getDefaultPk("sch10.table03", KeyColumnType.STRING), List.of()),
                makeTableModel("sch10.table04", getDefaultPk("sch10.table04", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch10.table03", "fk_sch_tab4_tabid_sch_tab_id", "id", "table03_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для двух связанных таблиц. Fk: int type")
    @DbUnitDataSet
    void testModel11() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch11.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch11.table01", getDefaultPk("sch11.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch11.table02", getDefaultPk("sch11.table02", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch11.table01", "fk_sch_tab2_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);

        model = databaseModelService.getDatabaseModel("sch11.table03", new RelationConfig());
        expectedModel = List.of(
                makeTableModel("sch11.table03", getDefaultPk("sch11.table03", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch11.table04", getDefaultPk("sch11.table04", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch11.table03", "fk_sch_tab4_tabid_sch_tab_id", "id", "table03_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для составного pk + таблицы без pk")
    @DbUnitDataSet
    void testModel12() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch12.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch12.table01", key("sch12.table01", new KeyPart("id1", KeyColumnType.NUMBER),
                        new KeyPart("id2", KeyColumnType.STRING)), List.of()),
                makeTableModel("sch12.table02", key("sch12.table02", new KeyPart("id", KeyColumnType.STRING)),
                        List.of(
                                Relation.of("sch12.table01", "fk_sch_tab_tabid1_sch_tab_id1", "id1", "table01_id1", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch12.table03", Key.of("sch12.table03", List.of(new KeyPart("table02_id",
                        KeyColumnType.STRING)), true),
                        List.of(
                                Relation.of("sch12.table02", "fk_sch_tab_tabid_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для составного fk")
    @DbUnitDataSet
    void testModel13() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch13.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch13.table01", key("sch13.table01", new KeyPart("id1", KeyColumnType.STRING), new KeyPart("id2", KeyColumnType.NUMBER)), List.of()),
                makeTableModel("sch13.table02", key("sch13.table02", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                new Relation.Builder("sch13.table01", "fk_sch_tab_tabid1_tabid2_sch_tab_id2_id1", RelationRule.NO_ACTION)
                                        .addColumns("id1", "table01_id1")
                                        .addColumns("id2", "table01_id2")
                                        .build()
                        )
                ),
                makeTableModel("sch13.table03", key("sch13.table03", new KeyPart("id1", KeyColumnType.NUMBER), new KeyPart("id2", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch13.table02", "fk_sch_tab_tabid_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION),
                                new Relation.Builder("sch13.table01", "fk_sch_tab_tabid2_tabid1_sch_tab_id2_id1", RelationRule.NO_ACTION)
                                        .addColumns("id1", "table01_id1")
                                        .addColumns("id2", "table01_id2")
                                        .build()
                        )
                ),
                makeTableModel("sch13.table04", Key.of("sch13.table04", List.of(new KeyPart("table03_id1", KeyColumnType.NUMBER), new KeyPart("table03_id2", KeyColumnType.NUMBER)), true),
                        List.of(
                                new Relation.Builder("sch13.table03", "fk_sch_tab_tabid2_tabid1_sch_tab_id1_id2", RelationRule.NO_ACTION)
                                        .addColumns("id1", "table03_id1")
                                        .addColumns("id2", "table03_id2")
                                        .build()
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для составного fk/нескольких fk между таблицами")
    @DbUnitDataSet
    void testModel09() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("SCH09.TABLE01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch09.table01", key("sch09.table01", new KeyPart("id1", KeyColumnType.NUMBER), new KeyPart("\"ID2\"", KeyColumnType.NUMBER)), List.of()),
                makeTableModel("sch09.table02", key("sch09.table02", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                new Relation.Builder("sch09.table01", "fk_sch_tab_tabid2_tabid1_sch_tab_id2_id1", RelationRule.NO_ACTION)
                                        .addColumns("id1", "table01_id1")
                                        .addColumns("\"ID2\"", "table01_id2")
                                        .build(),
                                Relation.of("sch09.table01", "fk_sch_tab_tabid1_sch_tab_id1", "id1", "table01_id1", RelationRule.NO_ACTION),
                                Relation.of("sch09.table01", "fk_sch_tab_tabid2_sch_tab_id2", "\"ID2\"", "table01_id2", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для цикла (сам на себя). Первая таблица")
    @DbUnitDataSet
    void testModel14() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch14.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch14.table01", key("sch14.table01", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch14.table01", "fk_sch_tab_parid_sch_tab_id", "id", "parent_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для цикла (сам на себя). Последняя таблица")
    @DbUnitDataSet
    void testModel15() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch15.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch15.table01", key("sch15.table01", new KeyPart("id", KeyColumnType.NUMBER)), List.of()),
                makeTableModel("sch15.table02", key("sch15.table02", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch15.table02", "fk_sch_tab_parid_sch_tab_id", "id", "parent_id", RelationRule.NO_ACTION),
                                Relation.of("sch15.table01", "fk_sch_tab_tabid_sch_tab_id1", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для цикла (сам на себя). Средняя таблица")
    @DbUnitDataSet
    void testModel16() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch16.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch16.table01", key("sch16.table01", new KeyPart("id", KeyColumnType.NUMBER)), List.of()),
                makeTableModel("sch16.table02", key("sch16.table02", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch16.table02", "fk_sch_tab_parid_sch_tab_id", "id", "parent_id", RelationRule.NO_ACTION),
                                Relation.of("sch16.table01", "fk_sch_tab_tabid_sch_tab_id1", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch16.table03", Key.of("sch16.table03", List.of(new KeyPart("table02_id", KeyColumnType.NUMBER)), true),
                        List.of(
                                Relation.of("sch16.table02", "fk_sch_tab_tabid_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel. Исключение таблицы из модели")
    @DbUnitDataSet
    void testModel05Exclude() throws Exception {
        RelationConfig config = new RelationConfig(Map.of(), Set.of("sch05.table03"));
        DatabaseModel model = databaseModelService.getDatabaseModel("sch05.table01", config);
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch05.table01", getDefaultPk("sch05.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch05.table02", getDefaultPk("sch05.table02", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch05.table01", "fk_sch_tab2_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch05.table05", getDefaultPk("sch05.table05", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch05.table02", "fk_sch_tab5_tabid_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel. Пользовательская связь")
    @DbUnitDataSet
    void testModel17Include() throws Exception {
        Relation customRel = Relation.of("sch17.table01", "custom", "id", "entity_id", RelationRule.NO_ACTION);
        RelationConfig relationConfig = new RelationConfig(List.of(Pair.of("sch17.param_value", customRel)), Set.of());

        DatabaseModel model = databaseModelService.getDatabaseModel("sch17.table01", relationConfig);
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch17.table01", getDefaultPk("sch17.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch17.param_value", key("sch17.param_value", new KeyPart("param_value_id", KeyColumnType.NUMBER)),
                        List.of(
                                customRel
                        )
                ),
                makeTableModel("sch17.table04", Key.of("sch17.table04", List.of(new KeyPart("param_value_id", KeyColumnType.NUMBER)), true),
                        List.of(
                                Relation.of("sch17.param_value", "fk_sch_tab_parvalid_sch_parval_parvalid", "param_value_id", "param_value_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel. Порядок таблиц")
    @DbUnitDataSet
    void testModel18Order() throws Exception {
        RelationConfig config = new RelationConfig();
        DatabaseModel model = databaseModelService.getDatabaseModel("sch18.table01", config);
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch18.table01", getDefaultPk("sch18.table01", KeyColumnType.NUMBER), List.of()),
                makeTableModel("sch18.table03", getDefaultPk("sch18.table03", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch18.table01", "fk_sch_tab3_tabid_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch18.table02", getDefaultPk("sch18.table02", KeyColumnType.NUMBER),
                        List.of(
                                Relation.of("sch18.table01", "fk_sch_tab_tabid_sch_tab_id1", "id", "table01_id", RelationRule.NO_ACTION),
                                Relation.of("sch18.table03", "fk_sch_tab_tabid_sch_tab_id", "id", "table03_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);

        // Дополнительно проверяем порядок таблиц. Тут он не может быть другим
        for (int i = 0; i < expectedModel.size(); ++i) {
            var ee = expectedModel.get(i);
            assertThat(model.getTableModels())
                    .element(i)
                    .satisfies(e -> assertThat(e.getTableName()).isEqualTo(ee.getTableName()));
        }
    }

    @Test
    @DisplayName("Получение DatabaseModel для цикла #1")
    @DbUnitDataSet
    void testModel20() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch20.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch20.table01", key("sch20.table01", new KeyPart("id", KeyColumnType.NUMBER)), List.of()),
                makeTableModel("sch20.table02", key("sch20.table02", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch20.table01", "fk_sch_tab_tabid_sch_tab_id1", "id", "table01_id", RelationRule.NO_ACTION),
                                Relation.of("sch20.table03", "fk_sch_tab_tabid_sch_tab_id", "id", "table03_id", RelationRule.NO_ACTION)
                        )
                ),
                makeTableModel("sch20.table03", key("sch20.table03", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch20.table01", "fk_sch_tab_tab1id_sch_tab_id", "id", "table01_id", RelationRule.NO_ACTION),
                                Relation.of("sch20.table02", "fk_sch_tab_tab2id_sch_tab_id", "id", "table02_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel для fk на unique")
    @DbUnitDataSet
    void testModel21() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch21.table01", new RelationConfig());
        List<TableModel> expectedModel = List.of(
                makeTableModel("sch21.table01", key("sch21.table01", new KeyPart("id", KeyColumnType.NUMBER)), List.of()),
                makeTableModel("sch21.table02", key("sch21.table02", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch21.table01", "fk_sch_tab_tabid_sch_tab_id1", "id", "table01_id", RelationRule.NO_ACTION)
                        )
                )
        );
        checkDatabaseModel(expectedModel, model);
    }

    @Test
    @DisplayName("Получение DatabaseModel. Исключение таблицы из модели. Каскадный fk")
    @DbUnitDataSet
    void testModel22Exclude() {
        RelationConfig config = new RelationConfig(Map.of(), Set.of("sch22.table02"));
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> databaseModelService.getDatabaseModel("sch22.table01", config))
                .withMessage("Could not exclude table [sch22.table02] with [CASCADE] delete rule");
    }

    @Test
    @DisplayName("Кастомный конфиг. Ссылка на себя")
    @DbUnitDataSet
    void testModel23() throws Exception {
        Relation relation1 = new Relation.Builder("sch23.partner", "custom_rel1", RelationRule.NO_ACTION)
                .addColumns("id", "key_numeric_value")
                .setWhereClause("key_type = 2")
                .build();

        Relation relation2 = new Relation.Builder("sch23.calendar_owner_keys", "custom_rel2", RelationRule.NO_ACTION)
                .addColumns("calendar_owner_id", "calendar_owner_id")
                .build();

        Relation relation3 = Relation.of("sch23.calendar_owner_keys", "custom_3", "calendar_owner_id", "owner_id", RelationRule.NO_ACTION);

        Relation relation4 = Relation.of("sch23.partner", "custom_4", "id", "owner_id", RelationRule.NO_ACTION);

        RelationConfig relationConfig = new RelationConfig(List.of(
                Pair.of("sch23.calendar_owner_keys", relation1),
                Pair.of("sch23.calendar_owner_keys", relation2),
                Pair.of("sch23.calendars", relation3),
                Pair.of("sch23.calendars", relation4)
        ), Set.of());
        final DatabaseModel model = databaseModelService.getDatabaseModel("sch23.partner", relationConfig);

        List<TableModel> expectedModel = List.of(
                makeTableModel("sch23.partner", key("sch23.partner", new KeyPart("id", KeyColumnType.NUMBER)), List.of()),
                makeTableModel("sch23.calendar_owner_keys", key("sch23.calendar_owner_keys", new KeyPart("calendar_owner_id", KeyColumnType.NUMBER), new KeyPart("key_type", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch23.partner", "custom_rel1", "id", "key_numeric_value", RelationRule.NO_ACTION, "key_type = 2"),
                                Relation.of("sch23.calendar_owner_keys", "custom_rel2", "calendar_owner_id", "calendar_owner_id", RelationRule.NO_ACTION))
                ),
                makeTableModel("sch23.calendars", key("sch23.calendars", new KeyPart("id", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("sch23.partner", "custom_4", "id", "owner_id", RelationRule.NO_ACTION),
                                Relation.of("sch23.calendar_owner_keys", "custom_3", "calendar_owner_id", "owner_id", RelationRule.NO_ACTION))
                )
        );

        checkDatabaseModel(expectedModel, model);
    }

    private static Key getDefaultPk(String tableName, KeyColumnType type) {
        return key(tableName, new KeyPart("id", type));
    }

    private static void checkDatabaseModel(List<TableModel> expectedTables, DatabaseModel actual) {
        assertThat(actual).isNotNull();
        if (CollectionUtils.isEmpty(expectedTables)) {
            assertThat(actual.getTableModels()).isEmpty();
            return;
        }

        Map<String, TableModel> tables = expectedTables.stream()
                .collect(Collectors.toMap(TableModel::getTableName, e -> e));

        for (TableModel tableModel : actual.getTableModels()) {
            TableModel expectedTableModel = tables.get(tableModel.getTableName());
            assertThat(expectedTableModel).isNotNull();
            assertThat(tableModel.getKey().getTable()).isEqualTo(expectedTableModel.getKey().getTable());
            assertThat(tableModel.getKey().isAllColumns()).isEqualTo(expectedTableModel.getKey().isAllColumns());
            checkEquality(expectedTableModel.getKey().getColumns(), tableModel.getKey().getColumns(), null);
            checkEquality(expectedTableModel.getRelations(), tableModel.getRelations(), (exp, act) ->
                    assertThat(act.getKeyColumnToFkColumn())
                            .as("Should have same key>fk as " + exp)
                            .isEqualTo(exp.getKeyColumnToFkColumn())
            );
            tables.remove(tableModel.getTableName());
        }

        assertThat(tables).isEmpty();
    }

    /**
     * Сравнивание двух коллекций без учета порядка
     */
    private static <E> void checkEquality(
            Collection<? extends E> expected,
            Collection<? extends E> actual,
            BiConsumer<? super E, ? super E> checker
    ) {
        if (CollectionUtils.isEmpty(expected) && CollectionUtils.isEmpty(actual)) {
            return;
        }

        Map<E, E> expectedSet = expected.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
        for (E obj : actual) {
            assertThat(expectedSet).hasEntrySatisfying(obj, expectedObj -> {
                expectedSet.remove(obj);
                if (checker != null) {
                    checker.accept(expectedObj, obj);
                }
            });
        }
        assertThat(expectedSet).isEmpty();
    }
}
