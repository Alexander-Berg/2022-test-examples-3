package ru.yandex.direct.core.testing.repository;


import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.client.model.AgencyNds;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.common.util.RepositoryUtils.percentToBigInteger;
import static ru.yandex.direct.core.entity.client.repository.AgencyNdsRepository.AGENCY_NDS_MAPPER;
import static ru.yandex.direct.dbschema.ppc.Tables.AGENCY_NDS;

@Repository
@ParametersAreNonnullByDefault
public class TestAgencyNdsRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    public void addAgencyNds(int shard, AgencyNds agencyNds) {
        new InsertHelper<>(dslContextProvider.ppc(shard),
                AGENCY_NDS).add(AGENCY_NDS_MAPPER, agencyNds).onDuplicateKeyIgnore().execute();
    }

    public void updateAgencyNds(int shard, long clientId, Percent nds) {
        dslContextProvider.ppc(shard)
                .update(AGENCY_NDS)
                .set(AGENCY_NDS.NDS, percentToBigInteger(nds))
                .where(AGENCY_NDS.CLIENT_ID.eq(clientId))
                .execute();
    }
}
