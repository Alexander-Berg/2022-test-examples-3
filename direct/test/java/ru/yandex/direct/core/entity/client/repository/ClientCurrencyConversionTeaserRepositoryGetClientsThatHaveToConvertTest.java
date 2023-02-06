package ru.yandex.direct.core.entity.client.repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.ClientsWorkCurrency;
import ru.yandex.direct.dbschema.ppc.enums.CurrencyConvertQueueConvertType;
import ru.yandex.direct.dbschema.ppc.enums.CurrencyConvertQueueNewCurrency;
import ru.yandex.direct.dbschema.ppc.enums.UsersHidden;
import ru.yandex.direct.dbutil.SqlUtils;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_TO_FORCE_MULTICURRENCY_TEASER;
import static ru.yandex.direct.dbschema.ppc.Tables.CURRENCY_CONVERT_QUEUE;
import static ru.yandex.direct.dbschema.ppc.Tables.FORCE_CURRENCY_CONVERT;
import static ru.yandex.direct.dbschema.ppc.Tables.USERS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientCurrencyConversionTeaserRepositoryGetClientsThatHaveToConvertTest {
    private ClientCurrencyConversionTeaserRepository clientCurrencyConversionTeaserRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo firstClientInfo;
    private UserInfo firstUserInfo;
    private ClientId firstClientId;
    private ClientInfo secondClientInfo;
    private ClientId secondClientId;
    private List<ClientId> bothClientIds;
    private int shard;
    DSLContext dslContext;

    @Before
    public void setUp() {
        clientCurrencyConversionTeaserRepository = new ClientCurrencyConversionTeaserRepository(dslContextProvider);

        firstClientInfo = steps.clientSteps().createDefaultClient();
        firstUserInfo = firstClientInfo.getChiefUserInfo();
        firstClientId = firstClientInfo.getClientId();

        secondClientInfo = steps.clientSteps().createDefaultClient();
        secondClientId = secondClientInfo.getClientId();

        bothClientIds = Arrays.asList(firstClientId, secondClientId);

        checkState(firstClientInfo.getShard() == secondClientInfo.getShard(),
                "клиенты должны быть на одном шарде для удобства тестирования");

        shard = firstClientInfo.getShard();
        dslContext = dslContextProvider.ppc(shard);

        // делаем так, что первого клиента надо конвертировать
        dslContext.insertInto(CLIENTS_TO_FORCE_MULTICURRENCY_TEASER)
                .set(CLIENTS_TO_FORCE_MULTICURRENCY_TEASER.CLIENT_ID, firstClientId.asLong())
                .execute();

        // запись в force_currency_convert не создаём и так и надо: если запись там есть,
        // клиент принял оферту и тизер "прими оферту" ему не нужен

        dslContext.update(CLIENTS)
                .set(CLIENTS.WORK_CURRENCY, ClientsWorkCurrency.YND_FIXED)
                .where(CLIENTS.CLIENT_ID.eq(firstClientId.asLong()))
                .execute();

        // запись в currency_convert_queue не создаём и так и надо: если запись там есть,
        // клиента сейчас сконвертируют и тизер ему тоже не нужен

        dslContext.update(USERS)
                .set(USERS.HIDDEN, UsersHidden.No)
                .where(USERS.CLIENT_ID.eq(firstClientId.asLong()))
                .execute();

        // а второго клиента не настраиваем, пусть его настроит тест, которому он нужен
        // по умолчанию ему показывать тизер не надо, потому что у него в clients_to_force_multicurrency_teaser
        // ничего нет
    }

    @Test
    public void testWithTwoClients() {
        assertThat(
                "первому клиенту надо показать тизер, второму нет",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, bothClientIds),
                containsInAnyOrder(firstClientId));
    }

    @Test
    public void testWithForceCurrencyConvertEntryPresent() {
        dslContext.insertInto(FORCE_CURRENCY_CONVERT)
                .set(FORCE_CURRENCY_CONVERT.CLIENT_ID, firstClientId.asLong())
                .execute();

        assertThat(
                "если есть запись в force_currency_convert, тизер не показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                empty());
    }

    @Test
    public void testWithWorkCurrencyRub() {
        dslContext.update(CLIENTS)
                .set(CLIENTS.WORK_CURRENCY, ClientsWorkCurrency.RUB)
                .where(CLIENTS.CLIENT_ID.eq(firstClientId.asLong()))
                .execute();

        assertThat(
                "если work_currency = RUB, тизер не показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                empty());
    }

    @Test
    public void testWithClientOptionNotConvertToCurrency() {
        dslContext.update(CLIENTS_OPTIONS)
                .set(CLIENTS_OPTIONS.CLIENT_FLAGS, "not_convert_to_currency")
                .where(CLIENTS_OPTIONS.CLIENT_ID.eq(firstClientId.asLong()))
                .execute();

        assertThat(
                "если в clients_options.client_flags есть not_convert_to_currency, тизер не показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                empty());
    }

    @Test
    public void testWithCurrencyConvertQueueEntryPresent() {
        dslContext.insertInto(CURRENCY_CONVERT_QUEUE)
                .set(CURRENCY_CONVERT_QUEUE.CLIENT_ID, firstClientId.asLong())
                .set(CURRENCY_CONVERT_QUEUE.CONVERT_TYPE, CurrencyConvertQueueConvertType.COPY)
                .set(CURRENCY_CONVERT_QUEUE.NEW_CURRENCY, CurrencyConvertQueueNewCurrency.RUB)
                .set(CURRENCY_CONVERT_QUEUE.COUNTRY_REGION_ID, 0L)
                .set(CURRENCY_CONVERT_QUEUE.EMAIL, "direct-dev-letters@yandex-team.ru")
                .set(CURRENCY_CONVERT_QUEUE.START_CONVERT_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(-5)))
                .set(CURRENCY_CONVERT_QUEUE.CONVERT_STARTED_AT,
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofMinutes(-5)))
                .execute();

        assertThat(
                "если есть запись в currency_convert_queue, тизер не показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                empty());
    }

    @Test
    public void testWithHiddenUser() {
        dslContext.update(USERS)
                .set(USERS.HIDDEN, UsersHidden.Yes)
                .where(USERS.CLIENT_ID.eq(firstClientId.asLong()))
                .execute();

        assertThat(
                "если есть одна запись в users с hidden = Yes, тизер не показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                empty());
    }

    @Test
    public void testWithTwoUsersNeitherHidden() {
        UserInfo additionalUserInfo =
                steps.userSteps().createUser(new User().withClientId(firstClientInfo.getClientId()));
        dslContext.update(USERS)
                .set(USERS.HIDDEN, UsersHidden.No)
                .where(USERS.UID.eq(additionalUserInfo.getUid()))
                .execute();

        assertThat(
                "если есть две записи в users с hidden = No, тизер показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                containsInAnyOrder(firstClientId));
    }

    @Test
    public void testWithTwoUsersOneHidden() {
        UserInfo additionalUserInfo =
                steps.userSteps().createUser(new User().withClientId(firstClientInfo.getClientId()));
        dslContext.update(USERS)
                .set(USERS.HIDDEN, UsersHidden.Yes)
                .where(USERS.UID.eq(additionalUserInfo.getUid()))
                .execute();

        assertThat(
                "если есть две записи в users, из которых у одной hidden = No, тизер не показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                empty());
    }

    @Test
    public void testWithTwoUsersBothHidden() {
        UserInfo additionalUserInfo =
                steps.userSteps().createUser(new User().withClientId(firstClientInfo.getClientId()));
        dslContext.update(USERS)
                .set(USERS.HIDDEN, UsersHidden.Yes)
                .where(USERS.UID.in(firstUserInfo.getUid(), additionalUserInfo.getUid()))
                .execute();

        assertThat(
                "если есть две записи в users, из которых у одной hidden = No, тизер не показывается",
                clientCurrencyConversionTeaserRepository.getClientsThatHaveToConvert(
                        shard, Collections.singletonList(firstClientId)),
                empty());
    }

}
