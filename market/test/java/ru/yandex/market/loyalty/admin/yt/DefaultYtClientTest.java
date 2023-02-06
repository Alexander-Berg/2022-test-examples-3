package ru.yandex.market.loyalty.admin.yt;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.config.YtHahn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.yandex.market.loyalty.admin.yt.DefaultYtClient.LINK_OBJECT_SYMBOL;

public class DefaultYtClientTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    @YtHahn
    private Yt yt;
    @Autowired
    @YtHahn
    private YtClient ytClient;

    @Test
    public void testCreateFolder() {
        when(yt.cypress().exists(any(Optional.class), anyBoolean(), any(YPath.class))).thenReturn(false);
        ytClient.createFolder(YPath.simple("//test"));
        verify(yt.cypress()).create(any(Optional.class), anyBoolean(), any(), any(), anyBoolean(), anyBoolean(),
                anyMap());
    }

    @Test
    public void testExists() {
        ytClient.exists(YPath.simple("//test"));
        verify(yt.cypress()).exists(any(Optional.class), anyBoolean(), any(YPath.class));
    }

    @Test
    public void testRead() throws Exception {
        when(yt.tables().read(any(), anyBoolean(), any(), any())).thenReturn(CloseableIterator.wrap(Collections.emptyList()));
        ytClient.read(YPath.simple("//test"), String.class);
        verify(yt.tables()).read(any(), anyBoolean(), any(), any());
    }

    @Test
    public void testAppendEmptyList() {
        ytClient.append(YPath.simple("//test"), Collections.emptyList());
        verify(yt.tables(), never()).write(any(Optional.class), anyBoolean(), eq(YPath.simple("//test")),
                any(YTableEntryType.class), any(Iterator.class));
    }

    @Test
    public void testAppendFullList() {
        ytClient.append(YPath.simple("//test"), Collections.singletonList(""));
        verify(yt.tables()).write(any(Optional.class), anyBoolean(), eq(YPath.simple("//test").append(true)),
                any(YTableEntryType.class), any(Iterator.class));
    }


    @Test
    public void testCreateNewTable() {
        when(yt.cypress().exists(any(Optional.class), anyBoolean(), any(YPath.class))).thenReturn(false);
        ytClient.createTable(YPath.simple("//test"));
        verify(yt.cypress()).create(any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean(), anyMap());
    }

    @Test
    public void testOverwriteTable() {
        when(yt.cypress().exists(any(Optional.class), anyBoolean(), any(YPath.class))).thenReturn(true);
        ytClient.createTable(YPath.simple("//test"));
        verify(yt.cypress(), times(2)).exists(any(Optional.class), anyBoolean(), any(YPath.class));
        verify(yt.cypress()).remove(any(Optional.class), anyBoolean(), any(YPath.class));
        verify(yt.cypress()).create(any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean(), anyMap());
    }

    @Test
    public void testRemove() {
        when(yt.cypress().exists(any(Optional.class), anyBoolean(), any(YPath.class))).thenReturn(true);
        ytClient.remove(YPath.simple("//test"));
        verify(yt.cypress()).exists(any(Optional.class), anyBoolean(), any(YPath.class));
        verify(yt.cypress()).remove(any(Optional.class), anyBoolean(), any(YPath.class));
    }

    @Test
    public void testDereferenceList() {
        ytClient.dereferenceLink(YPath.simple("//test"), GUID.valueOfO("1-15c37686-c9c2c55a-5ce7847a"));
        verify(yt.cypress()).get(any(Optional.class), anyBoolean(),
                eq(YPath.simple("//test" + LINK_OBJECT_SYMBOL).attribute("target_path")));
    }


    @Test
    public void testStartTransaction() {
        ytClient.startTransaction(0L);
        verify(yt.transactions()).start(any(Optional.class), anyBoolean(), any(), any(Map.class));
    }

    @Test
    public void testAbortTransaction() {
        ytClient.abortTransaction(GUID.valueOf("1-15c37686-c9c2c55a-5ce7847a"));
        verify(yt.transactions()).abort(any(GUID.class), anyBoolean());
    }

    @Test
    public void testCommitTransaction() {
        ytClient.commitTransaction(GUID.valueOf("1-15c37686-c9c2c55a-5ce7847a"));
        verify(yt.transactions()).commit(any(GUID.class), anyBoolean());
    }

    @Test
    public void testDoInTransaction() {
        when(yt.transactions().start(any(Optional.class), anyBoolean(), any(Duration.class), any())).thenReturn(GUID.valueOf("1-15c37686-c9c2c55a-5ce7847a"));
        ytClient.doInTransaction(a -> {
        });
        verify(yt.transactions()).start(any(Optional.class), anyBoolean(), any(Duration.class), any());
        verify(yt.transactions()).commit(any(GUID.class), anyBoolean());
    }

    @Test
    public void testCreateLink() {
        ytClient.createLink(YPath.simple("//test"), YPath.simple("//test_link"), Optional.of(GUID.valueOf("1-15c37686" +
                "-c9c2c55a-5ce7847a")));
        verify(yt.cypress()).link(any(), anyBoolean(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    public void testRemoveLink() {
        when(yt.cypress().exists(any(Optional.class), anyBoolean(), any(YPath.class))).thenReturn(true);
        ytClient.removeLink(YPath.simple("//test_link"), Optional.of(GUID.valueOf("1-15c37686-c9c2c55a-5ce7847a")));
        verify(yt.cypress()).exists(any(Optional.class), anyBoolean(), any(YPath.class));
        verify(yt.cypress()).remove(any(Optional.class), anyBoolean(), any(YPath.class));
    }

    @Test
    public void testList() {
        ytClient.list(YPath.simple("//test"));
        verify(yt.cypress()).list(any(Optional.class), anyBoolean(), any());
    }

    @Test
    public void testYql() {
        ytClient.selectRows("som qweri", String.class);
        verify(yt.tables()).selectRows(eq("som qweri"), any(YTableEntryType.class), any(Consumer.class));
    }
}
