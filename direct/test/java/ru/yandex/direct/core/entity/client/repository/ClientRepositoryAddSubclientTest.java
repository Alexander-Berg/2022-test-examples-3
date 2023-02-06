package ru.yandex.direct.core.entity.client.repository;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientWithOptions;
import ru.yandex.direct.core.entity.user.model.ApiEnabled;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_TO_FETCH_NDS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientRepositoryAddSubclientTest {
    private static final int SHARD = 1;

    @Autowired
    private Steps steps;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientId subclientClientId;

    @Before
    public void setUp() {
        subclientClientId = steps.clientSteps().generateNewClientId();
    }

    @Test
    public void testAddClient() {
        ClientWithOptions client = new ClientWithOptions()
                .withRole(RbacRole.CLIENT)
                .withClientId(subclientClientId)
                .withCurrency(CurrencyCode.RUB)
                .withHideMarketRating(true)
                .withNoTextAutocorrection(true)
                .withCountryRegionId(1L)
                .withName("Fio");

        clientRepository.addClient(SHARD, client, false);

        Collection<Client> result = clientRepository.get(SHARD, singleton(client.getClientId()));
        ApiEnabled enabledStatus = clientRepository.getClientApiOptionsEnabledStatus(
                SHARD, client.getClientId());

        assertThat(
                result,
                Matchers.contains(
                        allOf(
                                hasProperty("id", equalTo(client.getClientId().asLong())),
                                hasProperty("workCurrency", equalTo(client.getCurrency())),
                                hasProperty("hideMarketRating", equalTo(client.isHideMarketRating())),
                                hasProperty("noTextAutocorrection", equalTo(client.isNoTextAutocorrection())),
                                hasProperty("allowCreateScampBySubclient", equalTo(false)),
                                hasProperty("countryRegionId", equalTo(1L)),
                                hasProperty("name", equalTo("Fio"))
                        )
                )
        );
        assertThat(enabledStatus, equalTo(ApiEnabled.DEFAULT));
        assertNdsIsNotFetched(SHARD, client.getClientId());
    }

    @Test
    public void testAddSubclient() {
        ClientWithOptions client = new ClientWithOptions()
                .withRole(RbacRole.CLIENT)
                .withClientId(subclientClientId)
                .withCurrency(CurrencyCode.RUB)
                .withHideMarketRating(true)
                .withNoTextAutocorrection(true);

        clientRepository.addClient(SHARD, client, false);

        Collection<Client> result = clientRepository.get(SHARD, singleton(client.getClientId()));
        ApiEnabled enabledStatus = clientRepository.getClientApiOptionsEnabledStatus(
                SHARD, client.getClientId());

        assertThat(
                result,
                Matchers.contains(
                        allOf(
                                hasProperty("id", equalTo(client.getClientId().asLong())),
                                hasProperty("workCurrency", equalTo(client.getCurrency())),
                                hasProperty("hideMarketRating", equalTo(client.isHideMarketRating())),
                                hasProperty("noTextAutocorrection", equalTo(client.isNoTextAutocorrection())),
                                hasProperty("allowCreateScampBySubclient", equalTo(false)),
                                hasProperty("countryRegionId", equalTo(0L))
                        )
                )
        );
        assertThat(enabledStatus, equalTo(ApiEnabled.DEFAULT));
        assertNdsIsNotFetched(SHARD, client.getClientId());
    }

    private void assertNdsIsNotFetched(int shard, ClientId clientId) {
        Assert.assertThat(
                dslContextProvider.ppc(shard).select(CLIENTS_TO_FETCH_NDS.TRIES)
                        .from(CLIENTS_TO_FETCH_NDS)
                        .where(CLIENTS_TO_FETCH_NDS.CLIENT_ID.eq(clientId.asLong()))
                        .fetchOne(CLIENTS_TO_FETCH_NDS.TRIES),
                is((Long) null));
    }
}
