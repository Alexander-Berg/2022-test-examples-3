package ru.yandex.market.clickphite.config.storage;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.health.configs.clickphite.mongo.SplitEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistSettingsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceSplitOrFieldEntity;
import ru.yandex.market.statface.StatfaceField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClickphiteConfigCodeToMongoCopierOrikaTest {

    private static final String SPLIT_NAME = "split-name";
    private static final String SPLIT_EXPRESSION = "split-expression";
    private static final SplitWhitelistSettingsEntity WHITELIST_SETTINGS = new SplitWhitelistSettingsEntity(
        Arrays.asList("a", "b", "c"),
        null,
        false
    );

    @Test
    public void testSplitEntity() {
        final SplitEntity split1 = new SplitEntity(
            SPLIT_NAME,
            SPLIT_EXPRESSION,
            WHITELIST_SETTINGS
        );
        final SplitEntity split2 = ClickphiteConfigCodeToMongoCopier.MAPPER_FACADE.map(split1, SplitEntity.class);
        assertEquals(SPLIT_NAME, split2.getName());
        assertEquals(SPLIT_EXPRESSION, split2.getExpression());
        assertNull(split2.getWhitelistSettings());
    }

    @Test
    public void testStatfaceSplitOrFieldEntity() {
        final String splitTitle = "split-title";
        final boolean splitTree = true;
        final StatfaceField.ViewType splitViewType = StatfaceField.ViewType.String;
        final int splitPrecision = 10;
        final StatfaceSplitOrFieldEntity split1 = new StatfaceSplitOrFieldEntity(
            SPLIT_NAME,
            SPLIT_EXPRESSION,
            splitTitle,
            splitTree,
            splitViewType,
            splitPrecision,
            WHITELIST_SETTINGS
        );
        final StatfaceSplitOrFieldEntity split2 = ClickphiteConfigCodeToMongoCopier.MAPPER_FACADE.map(split1,
            StatfaceSplitOrFieldEntity.class);
        assertEquals(SPLIT_NAME, split2.getName());
        assertEquals(SPLIT_EXPRESSION, split2.getExpression());
        assertEquals(splitTitle, split2.getTitle());
        assertEquals(splitTree, split2.getTree());
        assertEquals(splitViewType, split2.getViewType());
        assertEquals(splitPrecision, (int) split2.getPrecision());
        assertNull(split2.getSplitWhitelistSettings());
    }

}
