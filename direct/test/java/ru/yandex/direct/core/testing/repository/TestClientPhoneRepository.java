package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbschema.ppc.enums.ClientPhonesPhoneType;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_PHONES;

@Repository
@QueryWithoutIndex("тестовый репозиторий")
public class TestClientPhoneRepository {

    private final ShardHelper shardHelper;
    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestClientPhoneRepository(ShardHelper shardHelper, DslContextProvider dslContextProvider) {
        this.shardHelper = shardHelper;
        this.dslContextProvider = dslContextProvider;
    }

    public void delete(int shard, ClientId clientId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CLIENT_PHONES)
                .where(CLIENT_PHONES.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    public void delete(int shard) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CLIENT_PHONES)
                .execute();
    }

    public Long addManual(int shard, ClientId clientId, String phoneNumber) {
        return addManual(shard, clientId, phoneNumber, "");
    }

    public Long addManual(int shard, ClientId clientId, String phoneNumber, String comment) {
        Long id = shardHelper.generateClientPhoneIds(1).get(0);
        var insertHelper = new InsertHelper<>(dslContextProvider.ppc(shard), CLIENT_PHONES);
        insertHelper.set(CLIENT_PHONES.CLIENT_PHONE_ID, id)
                .set(CLIENT_PHONES.CLIENT_ID, clientId.asLong())
                .set(CLIENT_PHONES.PHONE_TYPE, ClientPhonesPhoneType.manual)
                .set(CLIENT_PHONES.PHONE, phoneNumber)
                .set(CLIENT_PHONES.COMMENT, comment)
                .set(CLIENT_PHONES.TELEPHONY_SERVICE_ID, "")
                .newRecord();
        insertHelper.execute();
        return id;
    }

}
