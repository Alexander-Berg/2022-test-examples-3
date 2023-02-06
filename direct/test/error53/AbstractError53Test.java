package ru.yandex.autotests.directapi.test.error53;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.directapi.apiclient.errors.Api5Error;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directapi.test.error53.impl.AuthenticationInfo;
import ru.yandex.autotests.directapi.test.error53.impl.Error53Lock;
import ru.yandex.autotests.directapi.test.error53.impl.FixtureAction;
import ru.yandex.autotests.directapi.test.error53.impl.TestAction;
import ru.yandex.autotests.directapi.test.error53.impl.cases.ApiAccessCases;
import ru.yandex.autotests.directapi.test.error53.impl.cases.ClientLoginCases;
import ru.yandex.autotests.directapi.test.error53.impl.cases.InvalidTokenCases;
import ru.yandex.autotests.directapi.test.error53.impl.cases.SuccessCase;

/**
 * Базовый класс для теста на ошибку 53
 * <p>
 * Для того, чтобы создать тест для сервиса необходимо отнаследоваться от
 * заданного класса и передать в конструктор конфиг для теста
 */
@RunWith(Parameterized.class)
public abstract class AbstractError53Test {
    @ClassRule
    public static final ApiSteps api = new ApiSteps();

    public static final Error53Lock lock = Error53Lock.INSTANCE;

    @Rule
    public final Trashman trashman = new Trashman(api);

    private final Error53TestConfig config;

    @Parameterized.Parameter()
    public String description;
    @Parameterized.Parameter(value = 1)
    public String operator;
    @Parameterized.Parameter(value = 2)
    public String clientLogin;
    @Parameterized.Parameter(value = 3)
    public String apiToken;
    @Parameterized.Parameter(value = 4)
    public FixtureAction setUpAction;
    @Parameterized.Parameter(value = 5)
    public TestAction testAction;
    @Parameterized.Parameter(value = 6)
    public FixtureAction tearDownAction;
    @Parameterized.Parameter(value = 7)
    public Api5Error expectedError;
    @Parameterized.Parameter(value = 8)
    public boolean lockRequired;

    protected AbstractError53Test(Error53TestConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Parameterized.Parameters(name = "{0} (op: {1}, CL: {2})")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                SuccessCase.success(),

                InvalidTokenCases.expiredToken(),
                InvalidTokenCases.absentToken(),
                InvalidTokenCases.invalidTokenFormat(),

                ClientLoginCases.agencySuccess(),
                ClientLoginCases.agencyAbsentClientLogin(),
                ClientLoginCases.agencyUnknownLogin(),
                ClientLoginCases.agencyDirectClient(),
                ClientLoginCases.agencyAnotherAgencyClient(),

                ClientLoginCases.simpleClientReplSuccess(),
                ClientLoginCases.simpleClientMainReplSuccess(),
                ClientLoginCases.simpleClientUnknownLogin(),
                ClientLoginCases.simpleClientAnotherClient(),

                ClientLoginCases.expectedSimpleClient(),

                ApiAccessCases.clientWithStatusBlocked(),
                ApiAccessCases.clientWithBlockedApiAccess(),
                ApiAccessCases.clientWithNotAllowedIP(),
                ApiAccessCases.clientWithConvertingCurrency(),
                ApiAccessCases.yaAgencyClient(),
                ApiAccessCases.agencyClientWithStatusBlockedGetMethod(),
                ApiAccessCases.agencyClientWithStatusBlockedNotGetMethod());
    }

    @Test
    public void test() {
        if (lockRequired) {
            lock.acquire(config.getLockTimeout());
        }

        try {
            AuthenticationInfo authenticationInfo = new AuthenticationInfo()
                    .withOperator(operator)
                    .withClientLogin(clientLogin)
                    .withApiToken(apiToken);

            try {
                if (setUpAction != null) {
                    setUpAction.invoke(api, authenticationInfo);
                }

                testAction.invokeAndAssert(
                        api,
                        config,
                        authenticationInfo,
                        expectedError);
            } finally {
                if (tearDownAction != null) {
                    tearDownAction.invoke(api, authenticationInfo);
                }
            }
        } finally {
            if (lockRequired) {
                lock.release();
            }
        }
    }
}
