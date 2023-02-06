package ru.yandex.market.fintech.banksint.yt.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.fintech.banksint.mybatis.model.ScoringData;
import ru.yandex.market.fintech.banksint.util.YTreeMapNodeBuilder;

import static ru.yandex.market.fintech.banksint.yt.mapper.YsonScoringDataMapper.DATE;
import static ru.yandex.market.fintech.banksint.yt.mapper.YsonScoringDataMapper.ITEMS_ON_STOCK_NUMERIC;
import static ru.yandex.market.fintech.banksint.yt.mapper.YsonScoringDataMapper.mapYTreeMapNode;

class YsonScoringDataMapperTest {

    @Test
    void testAbsentQuantity() {
        YTreeMapNode testNode = new YTreeMapNodeBuilder()
                .addString(DATE, "2022-11-11")
                .addNull(ITEMS_ON_STOCK_NUMERIC)
                .build();

        ScoringData scoringData = mapYTreeMapNode(testNode);
        Assertions.assertNull(scoringData.getItemsOnStock());
    }

    @Test
    void testDoubleQuantity() {
        YTreeMapNode testNode = new YTreeMapNodeBuilder()
                .addString(DATE, "2022-11-11")
                .addDouble(ITEMS_ON_STOCK_NUMERIC, 15.4)
                .build();
        ScoringData scoringData = mapYTreeMapNode(testNode);
        Assertions.assertEquals(15, scoringData.getItemsOnStock());
    }
}
