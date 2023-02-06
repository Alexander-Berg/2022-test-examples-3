package ru.yandex.direct.ytcomponents.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.config.DirectConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectYtDynamicConfigTest {

    private DirectYtDynamicConfig directYtDynamicConfig;

    @Before
    public void setUp() {
        Config config = ConfigFactory.parseResources("ru/yandex/direct/ytcomponents/config/dynamic-yt.conf").resolve();
        DirectConfig directConfig = new DirectConfig(config.getConfig("dynamic-yt"));
        directYtDynamicConfig = new DirectYtDynamicConfig(directConfig);
    }

    @Test
    public void success_tablesDirectBanners() {
        assertThat(directYtDynamicConfig.tables().direct().bannersTablePath())
                .isEqualTo("//home/direct/mysql-sync/combined/banners");
    }

    @Test
    public void success_tablesDirectBids() {
        assertThat(directYtDynamicConfig.tables().direct().bidsTablePath())
                .isEqualTo("//home/direct/mysql-sync/combined/bids");
    }

    @Test
    public void success_tablesDirectCampaigns() {
        assertThat(directYtDynamicConfig.tables().direct().campaignsTablePath())
                .isEqualTo("//home/direct/mysql-sync/combined/campaigns");
    }

    @Test
    public void success_tablesDirectPhrases() {
        assertThat(directYtDynamicConfig.tables().direct().phrasesTablePath())
                .isEqualTo("//home/direct/mysql-sync/combined/phrases");
    }

    @Test
    public void success_tablesDirectSyncStates() {
        assertThat(directYtDynamicConfig.tables().direct().syncStatesTablePath())
                .isEqualTo("//home/direct/mysql-sync/mysql-sync-states");
    }

    @Test
    public void success_tablesYabsStatPhrases() {
        assertThat(directYtDynamicConfig.tables().yabsStat().phrasesTablePath())
                .isEqualTo("//home/yabs/stat/DirectPhraseStat");
    }

    @Test
    public void success_tablesYabsStatOrders() {
        assertThat(directYtDynamicConfig.tables().yabsStat().ordersTablePath())
                .isEqualTo("//home/yabs/stat/DirectGridStat");
    }

    @Test
    public void success_tablesYabsStatPhraseGoals() {
        assertThat(directYtDynamicConfig.tables().yabsStat().phraseGoalsTablePath())
                .isEqualTo("//home/yabs/stat/DirectPhraseGoalsStat");
    }

    @Test
    public void success_tablesYabsStatOrderGoals() {
        assertThat(directYtDynamicConfig.tables().yabsStat().orderGoalsTablePath())
                .isEqualTo("//home/yabs/stat/DirectGridGoalsStat");
    }

}
