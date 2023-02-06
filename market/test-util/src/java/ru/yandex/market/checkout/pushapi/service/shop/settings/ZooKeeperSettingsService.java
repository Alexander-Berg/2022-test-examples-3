package ru.yandex.market.checkout.pushapi.service.shop.settings;

import org.apache.log4j.Logger;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.DataType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;

import javax.annotation.PostConstruct;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author msavelyev
 */
public class ZooKeeperSettingsService implements SettingsService {

    private static final Logger log = Logger.getLogger(ZooKeeperSettingsService.class);

    public static final int INITIAL_TIMESTAMP = -1;
    private long timestamp = INITIAL_TIMESTAMP;
    private volatile AllSettings allSettings = new AllSettings();
    private ScheduledExecutorService executor;
    private ZookeeperSettingsHelper zookeeperSettingsHelper;
    private long reloadDelayInMinutes;

    public void setReloadDelayInMinutes(long reloadDelayInMinutes) {
        this.reloadDelayInMinutes = reloadDelayInMinutes;
    }

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public void setZookeeperSettingsHelper(ZookeeperSettingsHelper zookeeperSettingsHelper) {
        this.zookeeperSettingsHelper = zookeeperSettingsHelper;
    }


    private void checkTimestampAndReload() {
        final long remoteTimestamp = zookeeperSettingsHelper.loadTimestamp();
        if(remoteTimestamp > timestamp) {
            reloadSettingsAndUpdateTimestamp(remoteTimestamp);
        }
    }

    private void reloadSettingsAndUpdateTimestamp(long remoteTimestamp) {
        log.info("reloading settings. remoteTimestamp=" + remoteTimestamp + ", localTimestamp=" + timestamp);
        allSettings = zookeeperSettingsHelper.loadSettings();
        timestamp = remoteTimestamp;
    }

    @PostConstruct
    public void init() {
        executor.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        checkTimestampAndReload();
                    } catch(Exception e) {
                        log.error("can't reload settings by schedule", e);
                    }
                }
            }, 0, reloadDelayInMinutes, TimeUnit.MINUTES
        );
    }

    public boolean settingsAreLoaded() {
        return timestamp != INITIAL_TIMESTAMP;
    }

    @Override
    public AllSettings reloadAllSettings() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Settings reloadSettings(long shopId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AllSettings getAllSettings() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Settings getSettings(long shopId) {

        if(shopId == 1)
            return new Settings("http://aida:40444", "ZZZ", DataType.JSON, AuthType.URL);
        else if(shopId == 2)
            //https://market2you.ru/bitrix/services/ymarket/cart?sandbox=true&auth-token=CB0000012746F0A2
            return new Settings("https://market2you.ru/bitrix/services/ymarket", "CB0000012746F0A2", DataType.JSON, AuthType.URL);
        else if(shopId == 3)
            return new Settings("https://store77.net/bitrix/services/ymarket/", "570000013A43CE4A", DataType.JSON, AuthType.HEADER);



        final long remoteTimestamp = zookeeperSettingsHelper.loadTimestamp();
        if(remoteTimestamp > timestamp) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    reloadSettingsAndUpdateTimestamp(remoteTimestamp);
                }
            });
        }

        if(allSettings.containsKey(shopId)) {
            return allSettings.get(shopId);
        } else {
            throw new SettingsServiceException("settings for shop " + shopId + " not found");
        }
    }

    @Override
    public void updateSettings(long shopId, Settings settings) {
        log.info("updating settings for shop " + shopId + " to " + settings);

        final Settings oldSettings = allSettings.get(shopId);
        final Settings newSettings;
        if(oldSettings != null && settings.getFingerprint() == null) {
            newSettings = new Settings(
                settings.getUrlPrefix(),
                settings.getAuthToken(),
                settings.getDataType(),
                settings.getAuthType(),
                oldSettings.getFingerprint(),
                settings.isPartnerInterface()
            );
        } else {
            newSettings = settings;
        }
        final long newTimestamp = zookeeperSettingsHelper.storeSettings(shopId, newSettings);

        allSettings.put(shopId, settings);
        timestamp = newTimestamp;
    }

}
