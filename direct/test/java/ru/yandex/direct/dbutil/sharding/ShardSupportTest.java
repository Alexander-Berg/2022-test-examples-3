package ru.yandex.direct.dbutil.sharding;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.jooq.exception.DataAccessException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.dbutil.testing.DbUtilTest;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.SimpleDb;
import ru.yandex.direct.liveresource.LiveResourceFactory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

@DbUtilTest
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@SuppressWarnings("unchecked")
public class ShardSupportTest {
    private static final int NUM_OF_PPC_SHARDS = 1;

    @Autowired
    DatabaseWrapperProvider databaseWrapperProvider;

    private ShardSupport shardSupport;

    @Before
    public void setup() {
        String sqlInsert = LiveResourceFactory.get("classpath://" + "/sharding-mysql/insertData.sql").getContent();
        databaseWrapperProvider.get(SimpleDb.PPCDICT).query(jdbc -> jdbc.update(sqlInsert));

        ShardedValuesGenerator valuesGenerator = new ShardedValuesGenerator(databaseWrapperProvider);
        shardSupport = new ShardSupport(databaseWrapperProvider, valuesGenerator, NUM_OF_PPC_SHARDS);
    }

    @After
    public void cleanup() {
        String sqlDelete = LiveResourceFactory.get("classpath://" + "/sharding-mysql/deleteData.sql").getContent();
        databaseWrapperProvider.get(SimpleDb.PPCDICT).query(jdbc -> jdbc.update(sqlDelete));
    }

    @Test
    public void clientIdShard() {
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 123);
        assertThat(shard, is(4));
    }

    @Test
    public void clientIdNotFound() {
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 121);
        assertThat(shard, is(ShardSupport.NO_SHARD));
    }

    @Test
    public void clientIdShards() {
        List<Integer> shards = shardSupport.getShards(ShardKey.CLIENT_ID, Lists.newArrayList(123, 121, 124));
        assertThat(shards, contains(4, null, 5));
    }

    @Test
    public void uidShard() {
        int shard = shardSupport.getShard(ShardKey.UID, 17179869184L);
        assertThat(shard, is(4));
    }

    @Test
    public void uidShardNotFound() {
        int shard = shardSupport.getShard(ShardKey.UID, 17179869183L);
        assertThat(shard, is(ShardSupport.NO_SHARD));
    }

    @Test
    public void uidShards() {
        List<Integer> shards = shardSupport
                .getShards(ShardKey.UID, Lists.newArrayList(17179869184L, 17179869183L, 17179869186L, 17179869185L));
        assertThat(shards, contains(4, null, 5, 4));
    }

    @Test
    public void loginShard() {
        int shard = shardSupport.getShard(ShardKey.LOGIN, "foo");
        assertThat(shard, is(4));
    }

    @Test
    public void loginShardCaseInsensitive() {
        int shard = shardSupport.getShard(ShardKey.LOGIN, "FooBar");
        assertThat(shard, is(5));
    }


    @Test
    public void loginShardDatabaseDifferentCase() {
        int shard = shardSupport.getShard(ShardKey.LOGIN, "nonLowercase");
        assertThat(shard, is(2));
    }

    @Test
    public void loginShardNotFound() {
        int shard = shardSupport.getShard(ShardKey.LOGIN, "nosuchuser");
        assertThat(shard, is(ShardSupport.NO_SHARD));
    }

    @Test
    public void loginShards() {
        List<Integer> shards =
                shardSupport.getShards(ShardKey.LOGIN, Lists.newArrayList("foo", "nosuchuser", "foobar", "bar"));
        assertThat(shards, contains(4, null, 5, 4));
    }

    @Test
    public void loginShardsCaseInsensitive() {
        List<Integer> shards =
                shardSupport.getShards(ShardKey.LOGIN, Lists.newArrayList("Foo", "NoSuchUser", "FOOBAR", "bAR"));
        assertThat(shards, contains(4, null, 5, 4));
    }

    @Test
    public void loginShardsCached() {
        shardSupport.getShard(ShardKey.LOGIN, "foo");
        shardSupport.getShard(ShardKey.LOGIN, "FOOBAR");
        List<Integer> shards = shardSupport.getShards(ShardKey.LOGIN, Lists.newArrayList("Foo", "FooBar"));
        assertThat(shards, contains(4, 5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rootKeyShard() {
        shardSupport.getShard(ShardKey.SHARD, 1);
    }

    @Test
    public void loginToUid() {
        Long uid = shardSupport.getValue(ShardKey.LOGIN, "foo", ShardKey.UID, Long.class);
        assertThat(uid, is(17179869184L));
    }

    @Test
    public void loginsToUids() {
        List<Long> uids = shardSupport
                .getValues(ShardKey.LOGIN, Lists.newArrayList("foo", "nosuchuser", "foobar", "bar"), ShardKey.UID,
                        Long.class);
        assertThat(uids, contains(17179869184L, null, 17179869186L, 17179869185L));
    }

    @Test
    public void loginToClientId() {
        Integer clientId = shardSupport.getValue(ShardKey.LOGIN, "foo", ShardKey.CLIENT_ID, Integer.class);
        assertThat(clientId, is(123));
    }

    @Test
    public void loginsToClientIds() {
        List<Integer> clientIds = shardSupport
                .getValues(ShardKey.LOGIN, Lists.newArrayList("foo", "nosuchuser", "foobar", "bar"), ShardKey.CLIENT_ID,
                        Integer.class);
        assertThat(clientIds, contains(123, null, 124, 123));
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientIdToLoginUnreachable() {
        shardSupport.getValue(ShardKey.CLIENT_ID, 123, ShardKey.LOGIN, String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clientIdToLoginMissingUnreachable() {
        shardSupport.getValue(ShardKey.CLIENT_ID, 121, ShardKey.LOGIN, String.class);
    }

    @Test
    public void saveValuesSavesFirstElement() {
        shardSupport.deleteValues(ShardKey.CLIENT_ID, asList(1024, 1025));
        shardSupport.saveValues(ShardKey.CLIENT_ID, asList(1024, 1025), ShardKey.SHARD, 8);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 1024);
        assertThat(shard, is(8));
    }

    @Test
    public void saveValuesSavesSecondElement() {
        shardSupport.deleteValues(ShardKey.CLIENT_ID, asList(1024, 1025));
        shardSupport.saveValues(ShardKey.CLIENT_ID, asList(1024, 1025), ShardKey.SHARD, 8);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 1025);
        assertThat(shard, is(8));
    }

    @Test
    public void saveValuesMapSavesFirstKey() {
        shardSupport.deleteValues(ShardKey.CLIENT_ID, asList(1024, 1025));
        Map<Integer, Integer> saveMap = new HashMap<>();
        saveMap.put(1024, 8);
        saveMap.put(1025, 8);
        shardSupport.saveValues(ShardKey.CLIENT_ID, ShardKey.SHARD, saveMap);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 1024);
        assertThat(shard, is(8));
    }

    @Test
    public void saveValuesMapSavesSecondKey() {
        shardSupport.deleteValues(ShardKey.CLIENT_ID, asList(1024, 1025));
        Map<Integer, Integer> saveMap = new HashMap<>();
        saveMap.put(1024, 8);
        saveMap.put(1025, 8);
        shardSupport.saveValues(ShardKey.CLIENT_ID, ShardKey.SHARD, saveMap);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 1025);
        assertThat(shard, is(8));
    }

    @Test
    public void saveValuesMapSavesSecondElement() {
        shardSupport.deleteValues(ShardKey.CLIENT_ID, asList(1024, 1025, 1026));
        Map<Integer, Integer> saveMap = new HashMap<>();
        saveMap.put(1024, 8);
        saveMap.put(1025, 8);
        saveMap.put(1026, 9);
        shardSupport.saveValues(ShardKey.CLIENT_ID, ShardKey.SHARD, saveMap);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 1026);
        assertThat(shard, is(9));
    }

    @Test
    public void saveValue() {
        shardSupport.deleteValues(ShardKey.CLIENT_ID, Collections.singletonList(125));
        shardSupport.saveValue(ShardKey.CLIENT_ID, 125, ShardKey.SHARD, 9);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 125);
        assertThat(shard, is(9));
    }

    @Test(expected = DataAccessException.class)
    public void saveValueOverwrite() {
        shardSupport.saveValue(ShardKey.CLIENT_ID, 125, ShardKey.SHARD, 9);
        shardSupport.saveValue(ShardKey.CLIENT_ID, 125, ShardKey.SHARD, 42);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 125);
        assertThat(shard, is(9));
    }

    @Test(expected = IllegalStateException.class)
    public void saveValueNotFound() {
        shardSupport.saveValue(ShardKey.CLIENT_ID, 125, ShardKey.CLIENT_ID, 42);
    }

    @Test
    public void deleteValue() {
        shardSupport.saveValue(ShardKey.CLIENT_ID, 127, ShardKey.SHARD, 9);
        int shard = shardSupport.getShard(ShardKey.CLIENT_ID, 127);
        checkState(shard == 9, "должны получить добавленный шард");
        shardSupport.deleteValues(ShardKey.CLIENT_ID, Collections.singleton(127));

        int deletedValue = shardSupport.getShard(ShardKey.CLIENT_ID, 127);
        assertThat(deletedValue, equalTo(ShardSupport.NO_SHARD));
    }

    @Test
    public void deleteValueMultiple() {
        shardSupport.saveValue(ShardKey.CLIENT_ID, 128, ShardKey.SHARD, 9);
        shardSupport.saveValue(ShardKey.CLIENT_ID, 129, ShardKey.SHARD, 42);
        List<Integer> shards = shardSupport.getShards(ShardKey.CLIENT_ID, asList(128L, 129L));
        checkState(shards.size() == 2, "должно вернуться два шарда");

        shardSupport.deleteValues(ShardKey.CLIENT_ID, asList(128, 129));
        List<Integer> deletedValues = shardSupport.getShards(ShardKey.CLIENT_ID, asList(128L, 129L));
        assertThat(deletedValues, contains(nullValue(), nullValue()));
    }

    @Test
    public void generateValueSharded() {
        int value = shardSupport.generateValue(ShardKey.CID, ShardKey.CLIENT_ID, 123).intValue();
        assertThat(value, greaterThan(0));
    }

    @Test
    public void generateValueChain() {
        int value = shardSupport.generateValue(ShardKey.CID, ShardKey.LOGIN, "foobar").intValue();
        assertThat(value, greaterThan(0));
    }

    @Test(expected = IllegalStateException.class)
    public void generateValueChainNotFound() {
        shardSupport.generateValue(ShardKey.CID, ShardKey.LOGIN, "nosuchlogin");
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateValueShardedThrowsForRoot() {
        shardSupport.generateValue(ShardKey.SHARD, ShardKey.SHARD, 123);
    }

    @Test
    public void lookupLoginsByShard() {
        List<String> keys = shardSupport.lookupKeysWithValue(ShardKey.LOGIN, ShardKey.SHARD, 4, String.class);
        assertThat(keys, containsInAnyOrder("foo", "bar", "special1", "special2"));
    }

    @Test
    public void lookupLoginsByShardAutoconvert() {
        List<String> keys = shardSupport.lookupKeysWithValue(ShardKey.LOGIN, ShardKey.SHARD, "5", String.class);
        assertThat(keys, containsInAnyOrder("foobar"));
    }

    @Test
    public void lookupLoginsByClientId() {
        List<String> keys = shardSupport.lookupKeysWithValue(ShardKey.LOGIN, ShardKey.CLIENT_ID, 444, String.class);
        assertThat(keys, containsInAnyOrder("special1", "special2"));
    }

    @Test
    public void lookupLoginsByClientIdNotFound() {
        List<String> keys = shardSupport.lookupKeysWithValue(ShardKey.LOGIN, ShardKey.CLIENT_ID, 555, String.class);
        assertThat(keys, empty());
    }

    @Test
    public void lookupLoginsByUid() {
        List<String> logins = shardSupport.lookupKeysWithValue(ShardKey.LOGIN, ShardKey.UID, 123456789, String.class);
        assertThat(logins, containsInAnyOrder("special1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void lookupLoginsByCid() {
        shardSupport.lookupKeysWithValue(ShardKey.LOGIN, ShardKey.CID, 123, String.class);
    }

    @Test
    public void lookupLoginsByShards() {
        List<List<String>> logins = shardSupport
                .lookupKeysWithValues(ShardKey.LOGIN, ShardKey.SHARD, Lists.newArrayList(3, 4, 5), String.class);
        assertThat(logins, contains(emptyIterable(), containsInAnyOrder("foo", "bar", "special1", "special2"),
                containsInAnyOrder("foobar")));
    }

    @Test
    public void lookupLoginsByUids() {
        List<List<String>> logins = shardSupport.lookupKeysWithValues(ShardKey.LOGIN, ShardKey.UID,
                Lists.newArrayList(17179869184L, 123456789, 17179869186L, 12345678), String.class);
        assertThat(logins,
                contains(containsInAnyOrder("foo"), containsInAnyOrder("special1"), containsInAnyOrder("foobar"),
                        emptyIterable()));
    }
}
