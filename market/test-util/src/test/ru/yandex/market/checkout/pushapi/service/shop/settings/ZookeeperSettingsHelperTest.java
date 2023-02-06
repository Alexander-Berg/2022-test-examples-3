package ru.yandex.market.checkout.pushapi.service.shop.settings;

import org.apache.commons.codec.binary.Hex;
import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.DataType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder;
import ru.yandex.market.checkout.pushapi.client.util.DateUtilBean;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettings;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettingsBuilder;
import ru.yandex.market.common.zk.ZNodeBuilder;
import ru.yandex.market.common.zk.ZooClient;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.createDate;
import static ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder.sameSettings;
import static ru.yandex.market.checkout.pushapi.shop.entity.AllSettingsBuilder.sameAllSettings;

/**
 * @author msavelyev
 */
public class ZookeeperSettingsHelperTest {

    private final long SHOP_ID = 1234;

    private ZooClient zooClient = mock(ZooClient.class);
    private DateUtilBean dateUtilBean = mock(DateUtilBean.class);
    private ZookeeperSettingsHelper zookeeperSettingsHelper = new ZookeeperSettingsHelper();

    @Before
    public void setUp() throws Exception {
        zookeeperSettingsHelper.setZooClient(zooClient);
        zookeeperSettingsHelper.setDateUtilBean(dateUtilBean);

        when(dateUtilBean.now()).thenReturn(createDate("2013-05-05"));
    }

    @Test
    public void loadsTimestamp() throws Exception {
        final long zookeeperTimestamp = 1234567l;
        when(zooClient.getStringData(ZookeeperSettingsHelper.ZK_SETTINGS_TIMESTAMP_PATH))
            .thenReturn(String.valueOf(zookeeperTimestamp));

        final long timestamp = zookeeperSettingsHelper.loadTimestamp();

        assertThat(timestamp, is(equalTo(zookeeperTimestamp)));
    }

    @Test
    public void loadsSettingsFromTree() throws Exception {
        final long shop1234id = SHOP_ID;
        final long shop2345id = 2345;
        final String shop1234path = createFullShopPath(shop1234id);
        final String shop2345path = createFullShopPath(shop2345id);
        final SettingsBuilder shop1234settings = new SettingsBuilder()
            .withAuthToken("token")
            .withUrlPrefix("prefix")
            .withDataType(DataType.XML)
            .withAuthType(AuthType.URL)
            .withFingerprint(new byte[] { 0x01, 0x02 });
        final SettingsBuilder shop2345settings = shop1234settings
            .withAuthToken("token2")
            .withUrlPrefix("prefix2")
            .withDataType(DataType.JSON)
            .withAuthType(AuthType.HEADER)
            .withFingerprint(new byte[] { 0x03, 0x04 });

        when(zooClient.children(ZookeeperSettingsHelper.ZK_SHOP_PREFIX))
            .thenReturn(asList(
                createShortShopPath(shop1234id),
                createShortShopPath(shop2345id)
            ));
        when(zooClient.getStringData(shop1234path))
            .thenReturn("prefix\ttoken\tXML\tURL\t0102");
        when(zooClient.getStringData(shop2345path))
            .thenReturn("prefix2\ttoken2\tJSON\tHEADER\t0304");

        final AllSettings allSettings = zookeeperSettingsHelper.loadSettings();

        assertThat(
            allSettings,
            is(sameAllSettings(new AllSettingsBuilder()
                .add(shop1234id, shop1234settings)
                .add(shop2345id, shop2345settings)))
        );
    }

    @Test
    public void updatesSettings() throws Exception {
        final SettingsBuilder settings = new SettingsBuilder();
        zookeeperSettingsHelper.storeSettings(SHOP_ID, settings.build());

        verify(zooClient).setData(createFullShopPath(SHOP_ID), settingsToStringData(settings.build()));
    }

    @Test
    public void createsSettingsNodeWithDataIfItDoesNotExist() throws Exception {
        final Settings settings = new SettingsBuilder().build();

        final ZNodeBuilder nodeBuilder = createNodeBuilder();
        when(zooClient.newNode(createFullShopPath(SHOP_ID))).thenReturn(nodeBuilder);
        when(zooClient.setData(eq(createFullShopPath(SHOP_ID)), anyString()))
            .thenThrow(new KeeperException.NoNodeException());

        zookeeperSettingsHelper.storeSettings(SHOP_ID, settings);

        verify(nodeBuilder).data(settingsToStringData(settings));
        verify(nodeBuilder).create();
    }

    private ZNodeBuilder createNodeBuilder() {
        final ZNodeBuilder nodeBuilder = mock(ZNodeBuilder.class);
        when(nodeBuilder.data(anyString())).thenReturn(nodeBuilder);
        return nodeBuilder;
    }

    @Test
    public void storesNullFingerprintAsEmptyString() throws Exception {
        final SettingsBuilder settingsBuilder = new SettingsBuilder()
            .withFingerprint(null);

        zookeeperSettingsHelper.storeSettings(SHOP_ID, settingsBuilder.build());

        verify(zooClient)
            .setData(
                eq(createFullShopPath(SHOP_ID)),
                endsWith("\t")
            );
    }

    @Test
    public void loadsNullFingerprintFromNode() throws Exception {
        final SettingsBuilder settingsBuilder = new SettingsBuilder()
            .withUrlPrefix("prefix")
            .withAuthToken("token")
            .withDataType(DataType.XML)
            .withAuthType(AuthType.URL)
            .withFingerprint(null);

        when(zooClient.children(ZookeeperSettingsHelper.ZK_SHOP_PREFIX))
            .thenReturn(asList(createShortShopPath(SHOP_ID)));
        when(zooClient.getStringData(createFullShopPath(SHOP_ID)))
            .thenReturn("prefix\ttoken\tXML\tURL\t");

        final AllSettings allSettings = zookeeperSettingsHelper.loadSettings();
        final Settings settings = allSettings.get(SHOP_ID);

        assertThat(settings, sameSettings(settingsBuilder));
    }

    @Test
    public void updatesTimestampEvenIfSettingsNodeWasNotExist() throws Exception {
        final SettingsBuilder settings = new SettingsBuilder();

        when(zooClient.newNode(anyString())).thenReturn(mock(ZNodeBuilder.class, RETURNS_DEEP_STUBS));
        when(zooClient.setData(eq(createFullShopPath(SHOP_ID)), anyString()))
            .thenThrow(new KeeperException.NoNodeException());

        zookeeperSettingsHelper.storeSettings(SHOP_ID, settings.build());

        verify(zooClient).setData(eq(createFullShopPath(SHOP_ID)), anyString());
        verify(zooClient).setData(
            eq(ZookeeperSettingsHelper.ZK_SETTINGS_TIMESTAMP_PATH),
            anyString()
        );
    }

    @Test
    public void updatesTimestampInZookeeperOnSettingsUpdate() throws Exception {
        final SettingsBuilder settings = new SettingsBuilder();
        final Date now = createDate("2013-06-20 14:00:00");

        when(dateUtilBean.now()).thenReturn(now);

        zookeeperSettingsHelper.storeSettings(SHOP_ID, settings.build());

        verify(zooClient).setData(
            ZookeeperSettingsHelper.ZK_SETTINGS_TIMESTAMP_PATH,
            String.valueOf(now.getTime())
        );
    }

    @Test
    public void updatesTimestampAfterSettingsUpdate() throws Exception {

        zookeeperSettingsHelper.storeSettings(SHOP_ID, new SettingsBuilder().build());

        final InOrder inOrder = inOrder(zooClient);

        inOrder.verify(zooClient).setData(eq(createFullShopPath(SHOP_ID)), anyString());
        inOrder.verify(zooClient).setData(eq(ZookeeperSettingsHelper.ZK_SETTINGS_TIMESTAMP_PATH), anyString());
    }

    private String settingsToStringData(Settings settings) {
        return settings.getUrlPrefix()
            + "\t" + settings.getAuthToken()
            + "\t" + settings.getDataType()
            + "\t" + settings.getAuthType()
            + "\t" + Hex.encodeHexString(settings.getFingerprint());
    }

    private String createShortShopPath(long shopId) {
        return "shop-" + shopId;
    }

    private String createFullShopPath(long shopId) {
        return ZookeeperSettingsHelper.ZK_SHOP_PREFIX + "/shop-" + shopId;
    }
}
