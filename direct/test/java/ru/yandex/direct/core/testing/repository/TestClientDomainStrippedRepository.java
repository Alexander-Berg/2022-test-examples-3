package ru.yandex.direct.core.testing.repository;

import java.util.Collection;

import ru.yandex.direct.core.entity.client.model.ClientDomainStripped;
import ru.yandex.direct.core.entity.client.repository.ClientDomainsStrippedRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.tables.ClientDomainsStripped.CLIENT_DOMAINS_STRIPPED;

public class TestClientDomainStrippedRepository {

    private final ClientDomainsStrippedRepository clientDomainsStrippedRepository;

    private final DslContextProvider dslContextProvider;


    public TestClientDomainStrippedRepository(ClientDomainsStrippedRepository clientDomainsStrippedRepository,
                                              DslContextProvider dslContextProvider) {
        this.clientDomainsStrippedRepository = clientDomainsStrippedRepository;
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Добавление записей таблицы
     */
    public void add(int shard, Collection<ClientDomainStripped> domains) {

        new InsertHelper<>(dslContextProvider.ppc(shard), CLIENT_DOMAINS_STRIPPED)
                .addAll(clientDomainsStrippedRepository.mapper, domains)
                .onDuplicateKeyIgnore()
                .execute();
    }
}
