package ru.yandex.direct.core.entity.client.service;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.validation.UpdateClientValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateClientValidationServiceTest {

    private static final int SHARD = 1;
    private static final String NEW_NAME = randomAlphabetic(5);
    private long operatorUid;
    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;

    @Autowired
    private Steps steps;

    @Autowired
    private UpdateClientValidationService validationService;
    private RbacService rbacService;

    @Before
    public void before() {
        rbacService = mock(RbacService.class);
        validationService = new UpdateClientValidationService(rbacService);
        clientInfo1 = steps.clientSteps().createClient(new ClientInfo().withShard(SHARD));
        clientInfo2 = steps.clientSteps().createClient(new ClientInfo().withShard(SHARD));
        operatorUid = clientInfo1.getUid();

        when(rbacService.getChiefsByClientIds(anyCollection()))
                .thenReturn(ImmutableMap.of(
                        clientInfo1.getClientId(), clientInfo1.getUid(),
                        clientInfo2.getClientId(), clientInfo2.getUid()
                ));
        when(rbacService.getAccessibleAgencySubclients(anyLong(), anyCollection()))
                .thenAnswer(a -> new ArrayList<>(a.getArgument(1)));
    }

    @Test
    public void preValidate_NameIsValid_ResultIsSuccessful() {
        ModelChanges<Client> modelChanges =
                ModelChanges.build(clientInfo1.getClientId().asLong(), Client.class, Client.NAME, NEW_NAME);

        ValidationResult<List<ModelChanges<Client>>, Defect> vr =
                validationService.preValidate(singletonList(modelChanges), operatorUid);

        assertThat("результат валидации не должен содержать ошибок", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_HideMarketRatingAreValid_ResultIsSuccessful() {
        ModelChanges<Client> modelChanges = ModelChanges.build(clientInfo1.getClientId().asLong(), Client.class,
                Client.HIDE_MARKET_RATING, true);
        ValidationResult<List<ModelChanges<Client>>, Defect> vr =
                validationService.preValidate(singletonList(modelChanges), operatorUid);

        assertThat("результат валидации не должен содержать ошибок", vr, hasNoDefectsDefinitions());
    }


    @Test
    public void preValidate_IdBelongsToOtherClient_ResultHasElementError() {
        when(rbacService.getAccessibleAgencySubclients(anyLong(), anyCollection()))
                .thenReturn(emptyList());
        ModelChanges<Client> modelChanges =
                ModelChanges.build(clientInfo1.getClientId().asLong(), Client.class, Client.NAME, NEW_NAME);
        ValidationResult<List<ModelChanges<Client>>, Defect> validationResult =
                validationService.preValidate(singletonList(modelChanges), operatorUid);

        checkElementError(validationResult, path(index(0), field("id")), objectNotFound());
    }

    @Test
    public void preValidate_OperatorHasNoRights_ResultHasElementError() {
        when(rbacService.getAccessibleAgencySubclients(anyLong(), anyCollection()))
                .thenReturn(emptyList());
        ModelChanges<Client> modelChanges =
                ModelChanges.build(clientInfo1.getClientId().asLong(), Client.class, Client.NAME, NEW_NAME);
        ValidationResult<List<ModelChanges<Client>>, Defect> validationResult =
                validationService.preValidate(singletonList(modelChanges), clientInfo2.getUid());

        checkElementError(validationResult, path(index(0), field("id")), objectNotFound());
    }

    @Test
    public void preValidate_TwoValidItems_ResultHasElementError() {
        ModelChanges<Client> modelChanges1 = ModelChanges
                .build(clientInfo1.getClientId().asLong(), Client.class, Client.HIDE_MARKET_RATING, false);
        ModelChanges<Client> modelChanges2 = ModelChanges
                .build(clientInfo2.getClientId().asLong(), Client.class, Client.HIDE_MARKET_RATING, true);
        ValidationResult<List<ModelChanges<Client>>, Defect> vr =
                validationService.preValidate(asList(modelChanges1, modelChanges2), operatorUid);
        assertThat("результат валидации не должен содержать ошибок", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_TwoDuplicateValidItems_ResultHasElementError() {
        ModelChanges<Client> modelChanges = ModelChanges
                .build(clientInfo1.getClientId().asLong(), Client.class, Client.HIDE_MARKET_RATING, false);
        ValidationResult<List<ModelChanges<Client>>, Defect> vr =
                validationService.preValidate(asList(modelChanges, modelChanges), operatorUid);
        checkElementError(vr, path(index(0)), duplicatedElement());
        checkElementError(vr, path(index(1)), duplicatedElement());
    }

    @Test
    public void preValidate_TwoDuplicateInvalidItems_ResultHasElementError() {
        ModelChanges<Client> modelChanges = ModelChanges.build(clientInfo1.getClientId().asLong(), Client.class,
                Client.NO_TEXT_AUTOCORRECTION, null);
        ValidationResult<List<ModelChanges<Client>>, Defect> vr =
                validationService.preValidate(asList(modelChanges, modelChanges), operatorUid);

        checkElementError(vr, path(index(0)), duplicatedElement());
        checkElementError(vr, path(index(1)), duplicatedElement());
    }

    private <T> void checkElementError(ValidationResult<List<T>, Defect> validationResult,
                                       Path path, Defect defect) {
        assertThat("результат валидации не должен содержать ошибок уровня операции",
                validationResult.hasErrors(), is(false));
        assertThat("результат валидации должен содержать ошибку уровня элемента",
                validationResult, hasDefectDefinitionWith(validationError(path, defect)));
    }
}

