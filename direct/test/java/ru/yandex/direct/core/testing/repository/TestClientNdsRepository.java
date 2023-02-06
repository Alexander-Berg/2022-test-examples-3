package ru.yandex.direct.core.testing.repository;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.client.model.ClientNds;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.common.util.RepositoryUtils.percentToBigInteger;
import static ru.yandex.direct.core.entity.client.repository.ClientNdsRepository.CLIENT_NDS_MAPPER;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_NDS;

@Repository
@ParametersAreNonnullByDefault
public class TestClientNdsRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public void addClientNds(int shard, ClientNds clientNds) {
        new InsertHelper<>(dslContextProvider.ppc(shard),
                CLIENT_NDS).add(CLIENT_NDS_MAPPER, clientNds).onDuplicateKeyIgnore().execute();
    }

    public void updateClientNds(int shard, long clientId, Percent nds) {
        dslContextProvider.ppc(shard)
                .update(CLIENT_NDS)
                .set(CLIENT_NDS.NDS, percentToBigInteger(nds))
                .where(CLIENT_NDS.CLIENT_ID.eq(clientId))
                .execute();
    }

    public void deleteClientNds(int shard, long clientId) {
        dslContextProvider.ppc(shard)
                .delete(CLIENT_NDS)
                .where(CLIENT_NDS.CLIENT_ID.eq(clientId))
                .execute();
    }
}
