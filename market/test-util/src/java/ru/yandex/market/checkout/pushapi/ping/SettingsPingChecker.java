package ru.yandex.market.checkout.pushapi.ping;

import ru.yandex.market.checkout.pushapi.service.shop.settings.ZooKeeperSettingsService;
import ru.yandex.market.common.ping.CachingPingChecker;
import ru.yandex.market.common.ping.CheckResult;

/**
 * @author msavelyev
 */
public class SettingsPingChecker extends CachingPingChecker {

    private ZooKeeperSettingsService zooKeeperSettingsService;

    public void setZooKeeperSettingsService(ZooKeeperSettingsService zooKeeperSettingsService) {
        this.zooKeeperSettingsService = zooKeeperSettingsService;
    }

    @Override
    protected CheckResult makeChecks() {
        if (!zooKeeperSettingsService.settingsAreLoaded()) {
            return newResult("Settings have not been loaded yet");
        }
        return CheckResult.OK;
    }
}
