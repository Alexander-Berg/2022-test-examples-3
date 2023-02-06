package ru.yandex.market.yt.util.table.shardstrategy;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.yt.util.table.model.YtShardStrategy;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.misc.lang.number.UnsignedLong;

import static org.assertj.core.api.Assertions.assertThat;


public class OnePerTabletShardStrategyTest {

    @Test
    public void shouldGenerateKeyForOnePerTable() {
        // given
        YtTableModel tableModel = new YtTableModel();
        tableModel.setTabletCount(5);
        YtShardStrategy<UnsignedLong> shardStrategy = new OnePerTabletShardStrategy();

        // when
        List<UnsignedLong> keys = shardStrategy.generateKeys(tableModel);

        // then
        assertThat(keys).map(UnsignedLong::intValue).containsExactly(1, 2, 3, 4);
    }

}
