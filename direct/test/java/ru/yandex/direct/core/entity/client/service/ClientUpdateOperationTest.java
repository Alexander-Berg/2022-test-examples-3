package ru.yandex.direct.core.entity.client.service;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bs.resync.queue.repository.BsResyncQueueRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.validation.UpdateClientValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientUpdateOperationTest {

    private static final int SHARD = 1;
    private static final int SHARD_ELSE = 2;
    private static final String NEW_NAME = randomAlphabetic(5);
    private long operatorUid;
    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;

    @Autowired
    private Steps steps;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private BsResyncQueueRepository bsResyncQueueRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UpdateClientValidationService updateClientValidationService;

    @Autowired
    private ShardHelper shardHelper;

    @Before
    public void before() {
        var agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        clientInfo1 = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(SHARD));
        clientInfo2 = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(SHARD_ELSE));
        operatorUid = agencyClientInfo.getUid();
    }

    // возвращаемый результат при обновлении одного клиента

    @Test
    public void prepareAndApply_OneValidItem_ResultIsExpected() {
        ModelChanges<Client> modelChanges = modelChangesWithValidName(clientInfo1.getClientId());
        updateAndCheckErrors(Applicability.PARTIAL, modelChanges, empty());
    }

    // возвращаемый результат при обновлении двух клиентов

    @Test
    public void prepareAndApply_PartialYes_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidName(clientInfo1.getClientId()),
                modelChangesWithValidName(clientInfo2.getClientId()),
                empty(), empty());
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidName(clientInfo1.getClientId()),
                modelChangesWithInvalidId(),
                empty(), not(empty()));
    }

    @Test
    public void prepareAndApply_PartialYes_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithInvalidId(),
                modelChangesWithInvalidId(),
                not(empty()), not(empty()));
    }

    @Test
    public void prepareAndApply_PartialNo_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidName(clientInfo1.getClientId()),
                modelChangesWithValidName(clientInfo2.getClientId()),
                empty(), empty());
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidName(clientInfo1.getClientId()),
                modelChangesWithInvalidId(),
                empty(), not(empty()));
    }

    @Test
    public void prepareAndApply_PartialNo_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithInvalidId(),
                modelChangesWithInvalidId(),
                not(empty()), not(empty()));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItem_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithInvalidId(),
                modelChangesWithValidName(clientInfo2.getClientId()),
                not(empty()), empty());
    }

    private void checkUpdateResultOfTwoItems(Applicability applicability,
                                             ModelChanges<Client> modelChanges1, ModelChanges<Client> modelChanges2,
                                             Matcher<? super List<?>> modelChanges1ErrorMatcher,
                                             Matcher<? super List<?>> modelChanges2ErrorMatcher) {
        List<ModelChanges<Client>> modelChangesList = asList(modelChanges1, modelChanges2);

        ClientUpdateOperation updateOperation = createUpdateOperation(applicability, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть положительным", result.isSuccessful(), is(true));
        assertThat("успешность поэлементных результатов не соответствует ожидаемой",
                mapList(result.getResult(), Result::getErrors),
                contains(modelChanges1ErrorMatcher, modelChanges2ErrorMatcher));
    }

    private void updateAndCheckErrors(Applicability applicability, ModelChanges<Client> modelChanges,
                                      Matcher<? super List<?>> errorMatcher) {
        ClientUpdateOperation updateOperation = createUpdateOperation(applicability, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть положительный", result.isSuccessful(), is(true));
        assertThat("результат обновления элемента не соответствует ожидаемому",
                result.getResult().get(0).getErrors(), is(errorMatcher));
    }

    private ClientUpdateOperation createUpdateOperation(Applicability applicability,
                                                        List<ModelChanges<Client>> modelChangesList) {
        return new ClientUpdateOperation(applicability, modelChangesList,
                clientRepository, clientService, campaignRepository, bsResyncQueueRepository,
                updateClientValidationService, shardHelper, operatorUid);
    }

    private ModelChanges<Client> modelChangesWithValidName(ClientId id) {
        return ModelChanges.build(id.asLong(), Client.class, Client.NAME, NEW_NAME);
    }

    private ModelChanges<Client> modelChangesWithInvalidId() {
        return new ModelChanges<>(-1L, Client.class);
    }
}
