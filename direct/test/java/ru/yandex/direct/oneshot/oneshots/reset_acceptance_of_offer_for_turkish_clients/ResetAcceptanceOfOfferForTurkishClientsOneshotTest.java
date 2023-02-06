package ru.yandex.direct.oneshot.oneshots.reset_acceptance_of_offer_for_turkish_clients;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.feature.FeatureName.MODERATION_OFFER_ENABLED_FOR_DNA;

@OneshotTest
@RunWith(JUnitParamsRunner.class)
public class ResetAcceptanceOfOfferForTurkishClientsOneshotTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResetAcceptanceOfOfferForTurkishClientsOneshot oneshot;
    @Autowired
    private Steps steps;

    public static List<Object[]> params_offerAccepted() {
        return List.of(new Object[][]{
                {"Клиент из Турции с TRY валютой и с принятой офертой -> приянтие сбрасывается",
                        Region.TURKEY_REGION_ID, CurrencyCode.TRY, true, false},
                {"Клиент из Турции с TRY валютой и с не принятой офертой -> не изменяется",
                        Region.TURKEY_REGION_ID, CurrencyCode.TRY, false, false},
                {"Клиент из России с TRY валютой и с принятой офертой -> приянтие сбрасывается",
                        Region.RUSSIA_REGION_ID, CurrencyCode.TRY, true, false},
                {"Клиент из России с TRY валютой и с не принятой офертой -> не изменяется",
                        Region.RUSSIA_REGION_ID, CurrencyCode.TRY, false, false},
                {"Клиент из Турции с RUB валютой и с принятой офертой -> приянтие сбрасывается",
                        Region.TURKEY_REGION_ID, CurrencyCode.RUB, true, false},
                {"Клиент из Турции с RUB валютой и с не принятой офертой -> не изменяется",
                        Region.TURKEY_REGION_ID, CurrencyCode.RUB, false, false},
                {"Клиент из России с RUB валютой и с принятой офертой -> не изменяется",
                        Region.RUSSIA_REGION_ID, CurrencyCode.RUB, true, true},
                {"Клиент из России с RUB валютой и с не принятой офертой -> не изменяется",
                        Region.RUSSIA_REGION_ID, CurrencyCode.RUB, false, false},
        });
    }

    @Test
    @Parameters(method = "params_offerAccepted")
    @TestCaseName("{0}")
    public void test(String description,
                     Long clientRegionId,
                     CurrencyCode clientCurrency,
                     Boolean offerAccepted,
                     Boolean expectedOfferAccepted) {
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient()
                .withCountryRegionId(clientRegionId)
                .withWorkCurrency(clientCurrency));

        steps.userSteps().setUserProperty(clientInfo.getChiefUserInfo(), User.IS_OFFER_ACCEPTED, offerAccepted);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, false);

        oneshot.execute(null, null, clientInfo.getShard());

        List<User> actualUsers = userRepository.fetchByUids(clientInfo.getShard(), singletonList(clientInfo.getUid()));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actualUsers)
                    .as("список пользователей")
                    .hasSize(1);
            softly.assertThat(actualUsers.get(0).getIsOfferAccepted())
                    .as("флаг принятия офферты")
                    .isEqualTo(expectedOfferAccepted);
        });
    }

    @Test
    public void testResetOfferAcceptanceForTwoUsers() {
        List<Long> userIds = IntStream.range(0, 2)
                .mapToObj(i -> {
                    ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient()
                            .withCountryRegionId(Region.TURKEY_REGION_ID)
                            .withWorkCurrency(CurrencyCode.TRY));

                    steps.userSteps().setUserProperty(clientInfo.getChiefUserInfo(), User.IS_OFFER_ACCEPTED, true);
                    steps.featureSteps()
                            .addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, false);

                    return clientInfo.getUid();
                })
                .collect(Collectors.toList());

        oneshot.execute(null, null, ClientSteps.DEFAULT_SHARD);

        List<User> actualUsers = userRepository.fetchByUids(ClientSteps.DEFAULT_SHARD, userIds);
        Map<Long, Boolean> userIdToOfferAccepted = StreamEx.of(actualUsers)
                .mapToEntry(User::getIsOfferAccepted)
                .mapKeys(User::getUid)
                .toMap();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actualUsers)
                    .as("список пользователей")
                    .hasSize(2);
            softly.assertThat(userIdToOfferAccepted)
                    .as("флаг принятия офферты")
                    .hasEntrySatisfying(userIds.get(0), offerAccepted -> assertThat(offerAccepted).isFalse())
                    .hasEntrySatisfying(userIds.get(1), offerAccepted -> assertThat(offerAccepted).isFalse());
        });
    }
}
