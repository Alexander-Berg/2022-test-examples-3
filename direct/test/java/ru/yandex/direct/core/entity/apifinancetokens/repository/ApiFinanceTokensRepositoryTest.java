package ru.yandex.direct.core.entity.apifinancetokens.repository;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ApiFinanceTokensRepositoryTest {

    @Autowired
    private ApiFinanceTokensRepository repository;
    @Autowired
    private Steps steps;
    @Autowired
    private ApiUserService apiUserService;

    private long uid;

    @Before
    public void before() {
        uid = steps.userSteps().createDefaultUser().getUid();
        apiUserService.allowFinancialOperationsAcceptApiOffer(uid);
    }

    @Test
    public void getMasterToken_userHasNoToken_returnEmpty() {
        assertThat(repository.getMasterToken(uid), is(Optional.empty()));
    }

    @Test
    public void createAndGetMasterToken() {
        assertThat(repository.createAndGetMasterToken(uid), notNullValue());
    }

    @Test
    public void createAndThenGetMasterToken_canGetTokenAfterGeneration() {
        String token = repository.createAndGetMasterToken(uid);
        assertThat(repository.getMasterToken(uid), is(Optional.of(token)));
    }

    @Test
    public void dropMasterToken_tokenRemoved() {
        String token = repository.createAndGetMasterToken(uid);
        assertThat(repository.getMasterToken(uid), is(Optional.of(token)));
        repository.dropMasterToken(uid);
        assertThat(repository.getMasterToken(uid), is(Optional.empty()));
    }

    @Test
    public void dropMasterToken_userHasNoToken_noErrors() {
        repository.dropMasterToken(uid);
        assertThat(repository.getMasterToken(uid), is(Optional.empty()));
    }
}
