package ru.yandex.direct.core.entity.client.repository;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientRepositoryUpdateTest {
    private static final CompareStrategy EXCLUDE_CLIENTS_OPTIONS_STRATEGY =
            DefaultCompareStrategies
                    .allFieldsExcept(newPath("overdraftLimit"), newPath("autoOverdraftLimit"), newPath("debt"),
                            newPath("usesQuasiCurrency"));

    public static final boolean OLD_HIDE_MARKET_RATING = false;
    public static final boolean OLD_NO_TEXT_AUTOCORRECTION = true;
    public static final boolean OLD_NO_DISPLAY_HREF = false;

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private ClientInfo secondClientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(createTestClient());
        secondClientInfo = steps.clientSteps().createClient(createTestClient());
    }

    private Client createTestClient() {
        return TestClients.defaultClient()
                .withHideMarketRating(OLD_HIDE_MARKET_RATING)
                .withNoTextAutocorrection(OLD_NO_TEXT_AUTOCORRECTION)
                .withNoDisplayHref(OLD_NO_DISPLAY_HREF);
    }

    @Test
    public void updateShouldNotModifyClientWhenArgumentIsEmpty() {
        Client clientBeforeUpdate = getActualClient(clientInfo);

        clientRepository.update(clientInfo.getShard(), emptyList());

        Client clientAfterUpdate = getActualClient(clientInfo);

        assertThat(clientAfterUpdate, beanDiffer(clientBeforeUpdate));
    }

    @Test
    public void updateShouldNotModifyClientWhenModelChangesIsEmpty() {
        Client clientBeforeUpdate = getActualClient(clientInfo);

        ModelChanges<Client> modelChanges = new ModelChanges<>(clientInfo.getClient().getId(), Client.class);
        AppliedChanges<Client> appliedChanges = modelChanges.applyTo(clientInfo.getClient());

        clientRepository.update(clientInfo.getShard(), singleton(appliedChanges));

        Client clientAfterUpdate = getActualClient(clientInfo);

        assertThat(clientAfterUpdate,
                beanDiffer(clientBeforeUpdate).useCompareStrategy(EXCLUDE_CLIENTS_OPTIONS_STRATEGY));
    }

    @Test
    public void updateOne() {
        ModelChanges<Client> modelChanges = new ModelChanges<>(clientInfo.getClient().getId(), Client.class);
        modelChanges.process(!OLD_HIDE_MARKET_RATING, Client.HIDE_MARKET_RATING);
        modelChanges.process(!OLD_NO_TEXT_AUTOCORRECTION, Client.NO_TEXT_AUTOCORRECTION);
        modelChanges.process(!OLD_NO_DISPLAY_HREF, Client.NO_DISPLAY_HREF);

        AppliedChanges<Client> appliedChanges = modelChanges.applyTo(clientInfo.getClient());
        clientRepository.update(clientInfo.getShard(), singleton(appliedChanges));

        Client clientAfterUpdate = getActualClient(clientInfo);

        assertThat(clientAfterUpdate, allOf(
                hasProperty(Client.HIDE_MARKET_RATING.name(), is(!OLD_HIDE_MARKET_RATING)),
                hasProperty(Client.NO_TEXT_AUTOCORRECTION.name(), is(!OLD_NO_TEXT_AUTOCORRECTION)),
                hasProperty(Client.NO_DISPLAY_HREF.name(), is(!OLD_NO_DISPLAY_HREF))
        ));
    }

    @Test
    public void updateOneShouldNotAffectAnotherClient() {
        ModelChanges<Client> modelChanges = new ModelChanges<>(clientInfo.getClient().getId(), Client.class);
        modelChanges.process(!OLD_HIDE_MARKET_RATING, Client.HIDE_MARKET_RATING);
        AppliedChanges<Client> appliedChanges = modelChanges.applyTo(clientInfo.getClient());
        clientRepository.update(clientInfo.getShard(), singleton(appliedChanges));

        Client secondClientAfterUpdate = getActualClient(secondClientInfo);
        assertThat(secondClientAfterUpdate.getHideMarketRating(), is(OLD_HIDE_MARKET_RATING));
    }

    @Nonnull
    private Client getActualClient(ClientInfo clientInfo) {
        Collection<Client> clients = clientRepository.get(clientInfo.getShard(), singleton(clientInfo.getClientId()));
        Client client = Iterables.getFirst(clients, null);
        assumeThat(client, notNullValue());
        return client;
    }

    @Test
    public void updatePrimaryManager_success() {
        boolean isIdmManager = true;
        UserInfo internalUserInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        Client startClient = getActualClient(clientInfo);

        ModelChanges<Client> modelChanges = new ModelChanges<>(startClient.getClientId(), Client.class);
        modelChanges.process(internalUserInfo.getUid(), Client.PRIMARY_MANAGER_UID);
        modelChanges.process(isIdmManager, Client.IS_IDM_PRIMARY_MANAGER);
        Client currentClient = getActualClient(clientInfo);
        AppliedChanges<Client> appliedChanges = modelChanges.applyTo(currentClient);
        clientRepository.update(clientInfo.getShard(), singleton(appliedChanges));

        Client actualClient = getActualClient(clientInfo);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualClient.getPrimaryManagerUid()).isEqualTo(internalUserInfo.getUid());
            soft.assertThat(actualClient.getIsIdmPrimaryManager()).isEqualTo(isIdmManager);
        });
    }

}
