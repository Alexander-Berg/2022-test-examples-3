package ru.yandex.direct.core.entity.currency.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.currency.model.CurrencyConversionState;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CurrencyConvertQueueConvertType;
import ru.yandex.direct.dbschema.ppc.enums.CurrencyConvertQueueNewCurrency;
import ru.yandex.direct.dbschema.ppc.enums.CurrencyConvertQueueState;
import ru.yandex.direct.dbutil.SqlUtils;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CURRENCY_CONVERT_QUEUE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CurrencyConvertQueueRepositoryFetchConvertingClientsTest {

    private CurrencyConvertQueueRepository currencyConvertQueueRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo firstClientInfo;
    private UserInfo firstUserInfo;
    private ClientId firstClientId;
    private ClientInfo secondClientInfo;
    private UserInfo secondUserInfo;
    private ClientId secondClientId;
    private List<ClientId> bothClientIds;
    private int shard;
    DSLContext dslContext;
    private Duration stopOperationMinutesBeforeConvert = Duration.ofMinutes(15);

    @Before
    public void setUp() {
        currencyConvertQueueRepository = new CurrencyConvertQueueRepository(dslContextProvider);

        firstClientInfo = steps.clientSteps().createDefaultClient();
        firstUserInfo = steps.userSteps().createUser(new User().withClientId(firstClientInfo.getClientId()));
        firstClientId = firstClientInfo.getClientId();

        secondClientInfo = steps.clientSteps().createDefaultClient();
        secondUserInfo = steps.userSteps().createUser(new User().withClientId(secondClientInfo.getClientId()));
        secondClientId = secondClientInfo.getClientId();

        bothClientIds = Arrays.asList(firstClientId, secondClientId);

        checkState(firstClientInfo.getShard() == secondClientInfo.getShard(),
                "клиенты должны быть на одном шарде для удобства тестирования");

        shard = firstClientInfo.getShard();
        dslContext = dslContextProvider.ppc(shard);

        // первый клиент по умолчанию скоро конвертируется
        dslContext.insertInto(CURRENCY_CONVERT_QUEUE)
                .set(CURRENCY_CONVERT_QUEUE.CLIENT_ID, firstClientInfo.getClientId().asLong())
                .set(CURRENCY_CONVERT_QUEUE.UID, firstClientInfo.getUid())
                .set(CURRENCY_CONVERT_QUEUE.CONVERT_TYPE, CurrencyConvertQueueConvertType.COPY)
                .set(CURRENCY_CONVERT_QUEUE.NEW_CURRENCY, CurrencyConvertQueueNewCurrency.RUB)
                .set(CURRENCY_CONVERT_QUEUE.COUNTRY_REGION_ID, 0L)
                .set(CURRENCY_CONVERT_QUEUE.EMAIL, "direct-dev-letters@yandex-team.ru")
                .set(CURRENCY_CONVERT_QUEUE.START_CONVERT_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(-5))
                )
                .set(CURRENCY_CONVERT_QUEUE.CONVERT_STARTED_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(-5))
                )
                .execute();

        // второй клиент по умолчанию уже сконвертировался
        dslContext.insertInto(CURRENCY_CONVERT_QUEUE)
                .set(CURRENCY_CONVERT_QUEUE.CLIENT_ID, secondClientInfo.getClientId().asLong())
                .set(CURRENCY_CONVERT_QUEUE.UID, secondClientInfo.getUid())
                .set(CURRENCY_CONVERT_QUEUE.CONVERT_TYPE, CurrencyConvertQueueConvertType.COPY)
                .set(CURRENCY_CONVERT_QUEUE.STATE, CurrencyConvertQueueState.DONE)
                .set(CURRENCY_CONVERT_QUEUE.NEW_CURRENCY, CurrencyConvertQueueNewCurrency.RUB)
                .set(CURRENCY_CONVERT_QUEUE.COUNTRY_REGION_ID, 0L)
                .set(CURRENCY_CONVERT_QUEUE.EMAIL, "direct-dev-letters@yandex-team.ru")
                .set(CURRENCY_CONVERT_QUEUE.START_CONVERT_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(-5))
                )
                .set(CURRENCY_CONVERT_QUEUE.CONVERT_STARTED_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(-5))
                )
                .set(CURRENCY_CONVERT_QUEUE.CONVERT_FINISHED_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(-5))
                )
                .execute();
    }

    @Test
    public void testWithTwoClients() {
        assertThat(
                "первый клиент скоро конвертируется, второй нет",
                currencyConvertQueueRepository.fetchConvertingClients(
                        shard,
                        bothClientIds,
                        stopOperationMinutesBeforeConvert,
                        Collections.emptySet()),
                Matchers.containsInAnyOrder(firstClientId));
    }

    @Test
    public void testWithExcludeState() {
        assertThat(
                "excludeState = NEW исключает клиента, у которого state = NEW",
                currencyConvertQueueRepository.fetchConvertingClients(
                        shard,
                        Collections.singletonList(firstClientId),
                        stopOperationMinutesBeforeConvert,
                        Collections.singleton(CurrencyConversionState.NEW)),
                Matchers.empty());
    }

    @Test
    public void testWithConversionStartingSoon() {
        final int minutes = 5;

        dslContext.update(CURRENCY_CONVERT_QUEUE)
                .set(CURRENCY_CONVERT_QUEUE.START_CONVERT_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(minutes)))
                .where(CURRENCY_CONVERT_QUEUE.CLIENT_ID.eq(firstClientId.asLong()))
                .execute();

        assertThat(
                "startConvertAt = " + minutes + " минут в будущем не исключает клиента",
                currencyConvertQueueRepository.fetchConvertingClients(
                        shard,
                        Collections.singletonList(firstClientId),
                        stopOperationMinutesBeforeConvert,
                        Collections.emptySet()),
                Matchers.containsInAnyOrder(firstClientId));
    }

    @Test
    public void testWithConversionNotStartingSoon() {
        final int minutes = 20;

        dslContext.update(CURRENCY_CONVERT_QUEUE)
                .set(CURRENCY_CONVERT_QUEUE.START_CONVERT_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(minutes)))
                .where(CURRENCY_CONVERT_QUEUE.CLIENT_ID.eq(firstClientId.asLong()))
                .execute();

        assertThat(
                "startConvertAt = " + minutes + " минут в будущем исключает клиента",
                currencyConvertQueueRepository.fetchConvertingClients(
                        shard,
                        Collections.singletonList(firstClientId),
                        stopOperationMinutesBeforeConvert,
                        Collections.emptySet()),
                Matchers.empty());
    }

}
