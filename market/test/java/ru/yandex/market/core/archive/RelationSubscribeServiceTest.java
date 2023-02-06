package ru.yandex.market.core.archive;

import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.archive.model.DatabaseModel;
import ru.yandex.market.core.archive.model.Key;
import ru.yandex.market.core.archive.model.KeyPart;
import ru.yandex.market.core.archive.model.Relation;
import ru.yandex.market.core.archive.model.TableModel;
import ru.yandex.market.core.archive.type.KeyColumnType;
import ru.yandex.market.core.archive.type.RelationRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RelationSubscribeServiceTest extends ArchivingFunctionalTest {
    private RelationSubscribeService subscribeService;

    @BeforeEach
    void init() {
        subscribeService = new RelationSubscribeService();
    }

    @Test
    @DisplayName("Подписка без pk и с pk")
    void testStandard() {
        List<TableModel> tables = List.of(
                makeTableModel("SCH13.TABLE01", key("SCH13.TABLE01", new KeyPart("ID1", KeyColumnType.STRING), new KeyPart("ID2", KeyColumnType.NUMBER)), Collections.emptyList()),
                makeTableModel("SCH13.TABLE02", key("SCH13.TABLE02", new KeyPart("ID", KeyColumnType.NUMBER)),
                        List.of(
                                new Relation.Builder("SCH13.TABLE01", "FK02_01", RelationRule.NO_ACTION)
                                        .addColumns("ID1", "TABLE01_ID1")
                                        .addColumns("ID2", "TABLE01_ID2")
                                        .build()
                        )
                ),
                makeTableModel("SCH13.TABLE03", key("SCH13.TABLE03", new KeyPart("ID1", KeyColumnType.NUMBER), new KeyPart("ID2", KeyColumnType.NUMBER)),
                        List.of(
                                Relation.of("SCH13.TABLE02", "FK03_02", "ID", "TABLE02_ID", RelationRule.NO_ACTION),
                                new Relation.Builder("SCH13.TABLE01", "FK03_01", RelationRule.NO_ACTION)
                                        .addColumns("ID1", "TABLE01_ID1")
                                        .addColumns("ID2", "TABLE01_ID2")
                                        .build()
                        )
                ),
                makeTableModel("SCH13.TABLE04", Key.of("SCH13.TABLE04", List.of(new KeyPart("TABLE03_ID1", KeyColumnType.NUMBER), new KeyPart("TABLE03_ID2", KeyColumnType.NUMBER)), true),
                        List.of(
                                new Relation.Builder("SCH13.TABLE03", "FK04_03", RelationRule.NO_ACTION)
                                        .addColumns("ID1", "TABLE03_ID1")
                                        .addColumns("ID2", "TABLE03_ID2")
                                        .build()
                        )
                )
        );

        DatabaseModel model = new DatabaseModel(tables);

        subscribeService.addAll(model);


        List<String> subscribes = subscribeService.getSubscribesWithKey("SCH13.TABLE01");
        assertThat(subscribes, Matchers.containsInAnyOrder("SCH13.TABLE02", "SCH13.TABLE03"));

        subscribes = subscribeService.getSubscribesWithoutKey("SCH13.TABLE01");
        assertTrue(subscribes.isEmpty());

        subscribes = subscribeService.getSubscribesWithKey("SCH13.TABLE02");
        assertThat(subscribes, Matchers.containsInAnyOrder("SCH13.TABLE03"));

        subscribes = subscribeService.getSubscribesWithoutKey("SCH13.TABLE03");
        assertThat(subscribes, Matchers.containsInAnyOrder("SCH13.TABLE04"));
    }
}
