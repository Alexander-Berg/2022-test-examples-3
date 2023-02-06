package ru.yandex.market.checkout.pushapi.service.shop.settings;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.market.checkout.pushapi.client.entity.Condition;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.DataType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder;
import ru.yandex.market.checkout.pushapi.shop.entity.AllSettingsBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder.sameSettings;

/**
 * @author msavelyev
 */
public class ZooKeeperSettingsServiceTest {

    private final long SHOP_ID = 155l;

    private SettingsBuilder DEFAULT_SHOP_SETTINGS= new SettingsBuilder()
        .withAuthToken("token1")
        .withAuthType(AuthType.HEADER)
        .withDataType(DataType.XML)
        .withUrlPrefix("prefix1")
        .withFingerprint(new byte[] { 0x03, 0x04, 0x05 });
    private AllSettingsBuilder ALL_SETTINGS = new AllSettingsBuilder()
        .add(SHOP_ID, DEFAULT_SHOP_SETTINGS);

    private final long ZOOKEEPER_TIMESTAMP = 12345l;
    private final long RELOAD_DELAY_MINUTES = 5;

    private ZookeeperSettingsHelper zookeeperSettingsHelper = mock(ZookeeperSettingsHelper.class);

    private volatile boolean finishedTask = false;
    private Condition FINISHED_TASK_CONDITION = new Condition() {
        @Override
        public boolean satisfies() {
            return finishedTask;
        }
    };
    /*private ScheduledExecutorService executor = spy(new ScheduledThreadPoolExecutor(1) {
        @Override
        public void execute(final Runnable command) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    command.run();
                    finishedTask = true;
                }
            }).run();
        }
    });*/
    private ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    private ZooKeeperSettingsService zooKeeperSettingsService = new ZooKeeperSettingsService();

    @Before
    public void setUp() throws Exception {
        zooKeeperSettingsService.setReloadDelayInMinutes(RELOAD_DELAY_MINUTES);
        zooKeeperSettingsService.setExecutor(executor);
        zooKeeperSettingsService.setZookeeperSettingsHelper(zookeeperSettingsHelper);
        
        when(zookeeperSettingsHelper.loadSettings()).thenReturn(ALL_SETTINGS.build());
        when(zookeeperSettingsHelper.loadTimestamp()).thenReturn(ZOOKEEPER_TIMESTAMP);
        zooKeeperSettingsService.init();
    }

    @Test
    public void returnsOldShopSettingsIfTimestampHasNotBeenChanged() throws Exception {
        runRegisteredTask();

        final Settings settings = zooKeeperSettingsService.getSettings(SHOP_ID);

        assertThat(settings, is(sameSettings(DEFAULT_SHOP_SETTINGS.build())));
    }

    @Test
    public void reloadsSettingsIfTimestampDiffersOnShopGetSettings() throws Exception {
        runRegisteredTask();

        when(zookeeperSettingsHelper.loadTimestamp()).thenReturn(ZOOKEEPER_TIMESTAMP + 1);

        zooKeeperSettingsService.getSettings(SHOP_ID);

        verify(executor).execute(any(Runnable.class));
    }

    @Test(timeout = 500l)
    public void returnsNewSettingsWhenFullyReloaded() throws Exception {
        runRegisteredTask();

        when(zookeeperSettingsHelper.loadTimestamp()).thenReturn(ZOOKEEPER_TIMESTAMP + 1);
        final SettingsBuilder newSettingsBuilder = DEFAULT_SHOP_SETTINGS.withAuthToken("token2");
        when(zookeeperSettingsHelper.loadSettings()).thenReturn(
            new AllSettingsBuilder()
                .add(SHOP_ID, newSettingsBuilder)
                .build()
        );
        zooKeeperSettingsService.getSettings(SHOP_ID);
        runLastExecutedTask();

        final Settings settings = zooKeeperSettingsService.getSettings(SHOP_ID);
        assertThat(settings, is(sameSettings(newSettingsBuilder.build())));
    }
    
    @Test(timeout = 500)
    public void storesTimestampOnReloadSoDoesntReloadTwice() throws Exception {
        runRegisteredTask();
        when(zookeeperSettingsHelper.loadTimestamp()).thenReturn(ZOOKEEPER_TIMESTAMP + 1);
        zooKeeperSettingsService.getSettings(SHOP_ID);
        runLastExecutedTask();

        verifyNoMoreInteractions(executor);
        zooKeeperSettingsService.getSettings(SHOP_ID);
    }

    @Ignore
    @Test
    public void doesntEraseFingerprintIfNewSettingsHaveNullFingerprint() throws Exception {
        final SettingsBuilder settingsBuilder = DEFAULT_SHOP_SETTINGS.withFingerprint(null);

        zooKeeperSettingsService.updateSettings(SHOP_ID, settingsBuilder.build());

        verify(zookeeperSettingsHelper).storeSettings(eq(SHOP_ID), argThat(sameSettings(DEFAULT_SHOP_SETTINGS)));
    }

    @Test
    public void callsHelperOnUpdateSettings() throws Exception {
        final Settings newSettings = new SettingsBuilder()
            .withAuthToken("supa-new-token")
            .build();
        zooKeeperSettingsService.updateSettings(SHOP_ID, newSettings);

        verify(zookeeperSettingsHelper).storeSettings(
            eq(SHOP_ID),
            argThat(is(sameSettings(newSettings)))
        );
    }

    @Test
    public void updatesLocalSettingsOnShopSettingsUpdate() throws Exception {
        final Settings newSettings = new SettingsBuilder()
            .withAuthToken("supa-new-token")
            .build();
        zooKeeperSettingsService.updateSettings(SHOP_ID, newSettings);

        final Settings settings = zooKeeperSettingsService.getSettings(SHOP_ID);

        assertThat(settings, is(sameSettings(newSettings)));
    }

    @Test
    public void updatesLocalTimestampToOneReturnedByHelperAfterSettingsUpdate() throws Exception {
        reset(executor);
        final long NEW_TIMESTAMP = ZOOKEEPER_TIMESTAMP + 1;
        final Settings NEW_SETTINGS = new SettingsBuilder()
            .withAuthToken("supa-new-token")
            .build();

        when(zookeeperSettingsHelper.loadTimestamp()).thenReturn(NEW_TIMESTAMP);
        when(zookeeperSettingsHelper.storeSettings(
            eq(SHOP_ID),
            argThat(is(sameSettings(NEW_SETTINGS)))
        )).thenReturn(NEW_TIMESTAMP);

        zooKeeperSettingsService.updateSettings(SHOP_ID, NEW_SETTINGS);
        zooKeeperSettingsService.getSettings(SHOP_ID);

        verifyNoMoreInteractions(executor);
    }

    @Test
    public void registersTaskAtParticularDelay() throws Exception {
        final long initialDelay = 0;

        verify(executor).scheduleAtFixedRate(
            any(Runnable.class), eq(initialDelay), eq(RELOAD_DELAY_MINUTES), eq(TimeUnit.MINUTES)
        );
    }

    @Test
    public void scheduledTaskDoesNothingIfTimestampsAreTheSame() throws Exception {
        runRegisteredTask();
        reset(zookeeperSettingsHelper);

        when(zookeeperSettingsHelper.loadTimestamp()).thenReturn(ZOOKEEPER_TIMESTAMP);
        runRegisteredTask();

        verify(zookeeperSettingsHelper, never()).loadSettings();
    }

    @Test
    public void scheduledTaskReloadsSettingsIfTimestampsAreDifferent() throws Exception {
        reset(zookeeperSettingsHelper);

        final SettingsBuilder newSettingsBuilder = DEFAULT_SHOP_SETTINGS.withAuthToken("token2");

        when(zookeeperSettingsHelper.loadTimestamp()).thenReturn(ZOOKEEPER_TIMESTAMP + 1);
        when(zookeeperSettingsHelper.loadSettings()).thenReturn(
            new AllSettingsBuilder()
                .add(SHOP_ID, newSettingsBuilder)
                .build()
        );

        runRegisteredTask();

        assertThat(zooKeeperSettingsService.getSettings(SHOP_ID), is(sameSettings(newSettingsBuilder)));
    }

    @Test(expected = SettingsServiceException.class)
    public void throwsExceptionIfSettingsNotFound() throws Exception {
        final long UNKNOWN_SHOP_ID = 2345l;

        zooKeeperSettingsService.getSettings(UNKNOWN_SHOP_ID);
    }

    @Test
    public void returnsFalseIfSettingsHasntBeenLoadedYet() throws Exception {
        assertThat(zooKeeperSettingsService.settingsAreLoaded(), is(false));
    }

    @Test
    public void returnsTrueIfSettingsHasBeenLoaded() throws Exception {
        runRegisteredTask();

        assertThat(zooKeeperSettingsService.settingsAreLoaded(), is(true));
    }

    private void runRegisteredTask() {
        final ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleAtFixedRate(captor.capture(), anyLong(), anyLong(), any(TimeUnit.class));

        final Runnable runnable = captor.getValue();
        runnable.run();
    }

    private void runLastExecutedTask() {
        final ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(captor.capture());

        final Runnable runnable = captor.getValue();
        runnable.run();
    }

}
