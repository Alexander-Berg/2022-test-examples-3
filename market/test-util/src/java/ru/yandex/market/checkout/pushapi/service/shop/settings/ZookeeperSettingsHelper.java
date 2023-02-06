package ru.yandex.market.checkout.pushapi.service.shop.settings;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.DataType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.util.DateUtilBean;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;
import ru.yandex.market.common.zk.ZooClient;

import java.util.Collection;
import java.util.Date;

/**
 * @author msavelyev
 */
public class ZookeeperSettingsHelper {

    public static final String ZK_SETTINGS_TIMESTAMP_PATH = "/checkout/pushapi/settings/timestamp";
    public static final String ZK_SHOP_PREFIX = "/checkout/pushapi/settings/shops";

    private ZooClient zooClient;
    private DateUtilBean dateUtilBean = new DateUtilBean();

    private static final Logger log = Logger.getLogger(ZookeeperSettingsHelper.class);

    public void setZooClient(ZooClient zooClient) {
        this.zooClient = zooClient;
    }

    public void setDateUtilBean(DateUtilBean dateUtilBean) {
        this.dateUtilBean = dateUtilBean;
    }

    public AllSettings loadSettings() {
        try {
            final AllSettings allSettings = new AllSettings();
            final Collection<String> children = zooClient.children(ZK_SHOP_PREFIX);

            for(String child : children) {
                log.info("loading settings from zk-path " + child);
                allSettings.put(
                    parseShopIdFromPath(child),
                    parseSettings(zooClient.getStringData(createFullShopPath(child)))
                );
            }

            return allSettings;
        } catch(KeeperException e) {
            throw new RuntimeException("can't load settings from zookeeper", e);
        }
    }

    public long loadTimestamp() {
        try {
            final String strTimestamp = zooClient.getStringData(ZK_SETTINGS_TIMESTAMP_PATH);
            return Long.valueOf(strTimestamp);
        } catch(KeeperException e) {
            throw new RuntimeException("can't load timestamp from zookeeper", e);
        }
    }

    private long parseShopIdFromPath(String path) {
        final String[] tokens = path.split("-");
        return Long.parseLong(tokens[tokens.length-1]);
    }

    private Settings parseSettings(String strSettings) {
        final String[] tokens = strSettings.split("\t");

        final byte[] fingerprint;
        // "tokens.length > 4" — for migration, "null" — for new version with partnerInterface
        if(tokens.length > 4) {
            if(tokens[4].equalsIgnoreCase("null")) {
                fingerprint = null;
            }
            else {
                try {
                    fingerprint = Hex.decodeHex(tokens[4].toCharArray());
                } catch (DecoderException e) {
                    throw new RuntimeException("can't parse fingerprint");
                }
            }
        } else {
            fingerprint = null;
        }

        final boolean isPartnerInterface;
        if(tokens.length <= 5) {
            isPartnerInterface = false;
        } else {
            isPartnerInterface = Boolean.parseBoolean(tokens[5]);
        }

        return new Settings(
            tokens[0].equalsIgnoreCase("null") ? null : tokens[0],
            tokens[1].equalsIgnoreCase("null") ? null : tokens[1],
            tokens[2].equalsIgnoreCase("null") ? null :  DataType.valueOf(tokens[2]),
            tokens[3].equalsIgnoreCase("null") ? null : AuthType.valueOf(tokens[3]),
            fingerprint,
            isPartnerInterface
        );
    }

    public long storeSettings(long shopId, Settings settings) {
        try {
            try {
                zooClient.setData(createShopPath(shopId), settingsToStringData(settings));
            } catch(KeeperException e1) {
                zooClient.newNode(createShopPath(shopId)).data(settingsToStringData(settings)).create();
            }

            final Date now = dateUtilBean.now();
            final long timestamp = now.getTime();
            zooClient.setData(
                ZK_SETTINGS_TIMESTAMP_PATH,
                String.valueOf(timestamp)
            );

            return timestamp;
        } catch(KeeperException e) {
            throw new RuntimeException("can't store data to zookeeper", e);
        }
    }

    private String createFullShopPath(String child) {
        return ZK_SHOP_PREFIX + "/" + child;
    }

    private String createShopPath(long shopId) {
        return createFullShopPath("shop-" + shopId);
    }

    private String settingsToStringData(Settings settings) {
        return settings.getUrlPrefix()
            + "\t" + settings.getAuthToken()
            + "\t" + settings.getDataType()
            + "\t" + settings.getAuthType()
            + "\t" + settings.fingerprintAsString()
            + "\t" + settings.isPartnerInterface();
    }

}
