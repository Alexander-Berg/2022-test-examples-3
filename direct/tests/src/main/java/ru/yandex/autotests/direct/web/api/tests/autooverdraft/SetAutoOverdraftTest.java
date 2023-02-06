package ru.yandex.autotests.direct.web.api.tests.autooverdraft;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.apollographql.apollo.api.Response;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ClientsOptionsRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.queries.SetAutoOverdraftLimitMutation;
import ru.yandex.autotests.direct.web.api.models.queries.SwitchOffLimitPaymentDataQuery;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Установка порога автоовердрафта")
@Stories(TestFeatures.AutoOverdraft.SET_AUTO_OVERDRAFT)
@Features(TestFeatures.AUTO_OVERDRAFT)
@Tag(TrunkTag.YES)
@Tag(Tags.AUTO_OVERDRAFT)
public class SetAutoOverdraftTest {

    private static final String LOGIN = "auto-overdraft-client3";
    private static final int DEFAULT_OVERDRAFT_LIMIT = 10_000;
    private static final BigDecimal AUTO_OVERDRAFT_LIMIT = BigDecimal.valueOf(100);

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(LOGIN);

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    @Rule
    public DirectRule directRule = DirectRule.defaultRule().as(LOGIN);

    @Rule
    public Trashman trasher = new Trashman(api);

    @Test
    public void setAutoOverdraftSuccess() {

        DirectJooqDbSteps dbSteps = directRule.dbSteps().useShardForLogin(LOGIN);
        Long clientId = dbSteps.usersSteps().getUser(LOGIN).getClientid();
        ClientsOptionsRecord clientsOptionsRecord = dbSteps.clientsOptionsSteps().getClientOptions(clientId)
                .setOverdraftLim(BigDecimal.valueOf(DEFAULT_OVERDRAFT_LIMIT)).setAutoOverdraftLim(BigDecimal.ZERO);
        dbSteps.clientsOptionsSteps().updateClientOptions(clientsOptionsRecord);
        directRule.dbSteps().featuresSteps().ensureClientHasFeature(clientId, "autooverdraft_enabled");
        double vatRate = MoneyCurrency.get(Currency.RUB).getVatRate();
        api.userSteps.clientFakeSteps().setVATRate(LOGIN, vatRate);

        api.userSteps.balanceSteps().operator(User.get(LOGIN)).setClientOverdraftByLogin(LOGIN, DEFAULT_OVERDRAFT_LIMIT, Currency.RUB);
        List<SwitchOffLimitPaymentDataQuery.AutoOverdraftPaymentOption> paymentOptions =
                directRule.webApiSteps().autoOverdraftSteps().getPaymentOptions(LOGIN);
        assumeThat("Биллинг отдал возможные варианты оплаты", paymentOptions, is(not(nullValue())));
        assumeThat("Биллинг отдал непустой список плательщиков", paymentOptions, is(not(empty())));

        SwitchOffLimitPaymentDataQuery.AutoOverdraftPaymentOption paymentOption = paymentOptions.get(0);
        assumeThat("Биллинг отдал непустой список вариантов оплаты", paymentOption.paymentMethods(), is(not(empty())));

        Response<Optional<SetAutoOverdraftLimitMutation.Data>> response =
                directRule.webApiSteps().autoOverdraftSteps().setAutoOverdraftLimit(LOGIN,
                        AUTO_OVERDRAFT_LIMIT,
                        Long.class.cast(paymentOption.personInfo().id()), paymentOption.paymentMethods().get(0).code());
        assumeThat("Порог овердрафта успешно установлен",
                !response.data()
                        .map(SetAutoOverdraftLimitMutation.Data::setAutoOverdraftLimit)
                        .map(SetAutoOverdraftLimitMutation.SetAutoOverdraftLimit::validationResult)
                        .map(Optional::isPresent)
                        .orElse(false), is(true));

        double actualAutoOverdraftLimit = dbSteps.clientsOptionsSteps().getClientOptions(clientId).getAutoOverdraftLim().doubleValue();
        double expectedAutoOverdraftLimit = AUTO_OVERDRAFT_LIMIT.doubleValue();
        assertThat("Порог овердрафта соответствует ожидаемому",
                Math.abs(expectedAutoOverdraftLimit - actualAutoOverdraftLimit),
                lessThan(0.01));
    }

}
