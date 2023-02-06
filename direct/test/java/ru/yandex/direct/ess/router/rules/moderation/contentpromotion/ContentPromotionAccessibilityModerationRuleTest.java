package ru.yandex.direct.ess.router.rules.moderation.contentpromotion;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.logicobjects.moderation.contentpromotion.ContentPromotionAccessibilityLogicObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.CONTENT_PROMOTION;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class ContentPromotionAccessibilityModerationRuleTest {

    @Autowired
    private ContentPromotionAccessibilityModerationRule rule;

    private static BinlogEvent.Row createBinlogRowWithMetadataChange(BigInteger contentPromotionId) {
        return new BinlogEvent.Row()
                .withPrimaryKey(
                        Map.of(CONTENT_PROMOTION.ID.getName(), contentPromotionId))
                .withBefore(
                        Map.of(CONTENT_PROMOTION.METADATA.getName(), "{\"data\": \"meta\"}"))
                .withAfter(
                        Map.of(CONTENT_PROMOTION.METADATA.getName(), "{\"data\": \"meta\"}"));
    }

    private static BinlogEvent.Row createBinlogRowWithAccessibilityChange(Long contentPromotionId,
                                                                          Long newIsInaccessible) {
        return new BinlogEvent.Row()
                .withPrimaryKey(
                        Map.of(CONTENT_PROMOTION.ID.getName(), contentPromotionId))
                .withBefore(
                        Map.of(CONTENT_PROMOTION.IS_INACCESSIBLE.getName(), newIsInaccessible ^ 1))
                .withAfter(
                        Map.of(CONTENT_PROMOTION.IS_INACCESSIBLE.getName(), newIsInaccessible));
    }

    @Test
    void mapBinlogEventTest_IsInaccessibleChangedToTrue() {
        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(CONTENT_PROMOTION.getName())
                .withOperation(UPDATE)
                .withEssTag("essTag")
                .withRows(ImmutableList.of(createBinlogRowWithAccessibilityChange(1L, 1L)));

        List<ContentPromotionAccessibilityLogicObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEqualTo(singletonList(
                new ContentPromotionAccessibilityLogicObject("essTag", null, 1L, 1L, false)));
    }

    @Test
    void mapBinlogEventTest_IsInaccessibleChangedToFalse() {
        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(CONTENT_PROMOTION.getName())
                .withOperation(UPDATE)
                .withEssTag("essTag")
                .withRows(ImmutableList.of(createBinlogRowWithAccessibilityChange(2L, 0L)));

        List<ContentPromotionAccessibilityLogicObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEqualTo(singletonList(
                new ContentPromotionAccessibilityLogicObject("essTag", null, 2L, 0L, false)));
    }

    @Test
    void mapBinlogEventTest_IsInaccessibleNotChanged() {
        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(CONTENT_PROMOTION.getName())
                .withOperation(UPDATE)
                .withRows(ImmutableList.of(createBinlogRowWithMetadataChange(TWO.add(ONE))));

        List<ContentPromotionAccessibilityLogicObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEqualTo(emptyList());
    }
}
