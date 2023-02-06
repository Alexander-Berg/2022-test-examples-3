package ru.yandex.direct.core.entity.client.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.response.ClientPassportInfo;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParams;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParamsItem;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsResult;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewTextBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbschema.ppc.enums.UsersOptionsSendclientletters;
import ru.yandex.direct.dbschema.ppc.enums.UsersOptionsSendclientsms;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.result.Result;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB;
import static ru.yandex.direct.dbschema.ppc.Tables.USERS_OPTIONS;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientChiefServiceTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BalanceClient balanceClient;
    @Autowired
    private ClientChiefService clientChiefService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private DbQueueRepository dbQueueRepository;
    @Autowired
    private TextBannerSteps textBannerSteps;

    private ClientId clientId;
    private Long newChiefUid;
    private Integer shard;
    private Long oldChiefUid;
    private ClientInfo clientInfo;
    private UserInfo newChiefUserInfo;

    /**
     * Возвращает значение совйства IsMain переданное в клиент Баласа (метод BalanceClient#editPassport)
     * для соответствующего uid'а.
     */
    private static Integer getSentIsMainProperty(List<Object[]> balanceSentArgs, Long uid) {
        return StreamEx.of(balanceSentArgs)
                .filter(requestArgs -> Objects.equals(requestArgs[1], uid))
                .map(requestArgs -> (ClientPassportInfo) requestArgs[2])
                .map(ClientPassportInfo::getIsMain)
                .findFirst()
                .orElse(null);
    }

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        newChiefUserInfo = steps.userSteps().createRepresentative(clientInfo, RbacRepType.CHIEF);

        clientId = clientInfo.getClientId();
        newChiefUid = newChiefUserInfo.getUid();
        shard = newChiefUserInfo.getShard();
        oldChiefUid = clientInfo.getClient().getChiefUid();

        Long clientIdAsLong = clientId.asLong();
        ClientPassportInfo newChiefBalanceInfo = new ClientPassportInfo()
                .withIsMain(0)
                .withClientId(clientIdAsLong)
                .withUid(newChiefUid);
        ClientPassportInfo oldChiefBalanceInfo = new ClientPassportInfo()
                .withIsMain(1)
                .withClientId(clientIdAsLong)
                .withUid(oldChiefUid);
        when(balanceClient.getPassportByUid(anyLong(), ArgumentMatchers.eq(newChiefUid)))
                .thenReturn(newChiefBalanceInfo);
        when(balanceClient.getClientRepresentativePassports(anyLong(), ArgumentMatchers.eq(clientIdAsLong)))
                .thenReturn(List.of(oldChiefBalanceInfo, newChiefBalanceInfo));
    }

    @After
    public void tearDown() {
        // Отвязываем наше моканье хождения во внешние сервисы, чтобы не мешать другим тестам.
        Mockito.reset(balanceClient);
    }

    /**
     * Тест основных полей
     */
    @Test
    public void changeChief_success() {
        //Вызываем сервис
        Result result = clientChiefService.changeChief(newChiefUid, clientId);

        //Проверяем результат
        Client actualClient = clientRepository.get(shard, singleton(clientId)).get(0);
        User actualNewChief = userRepository.fetchByUids(shard, singleton(newChiefUid)).get(0);
        User actualOldChief = userRepository.fetchByUids(shard, singleton(oldChiefUid)).get(0);
        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(result.isSuccessful()).as("successful result").isTrue();
            soft.assertThat(actualClient.getChiefUid()).as("client chief Uid").isEqualTo(newChiefUid);
            soft.assertThat(actualNewChief.getRepType()).as("new chief RepType")
                    .isEqualTo(RbacRepType.CHIEF);
            soft.assertThat(actualOldChief.getRepType()).as("old chief RepType")
                    .isEqualTo(RbacRepType.MAIN);
        });
    }

    /**
     * Тест параметров передаваемых в Баланс
     */
    @Test
    public void changeChief_checkBalanceRequests_success() {
        //Значения свойства IsMain в балансе для главного и не главного представителей, соотвественно
        int chiefIsMain = 1;
        int notChiefIsMain = 0;

        //Организуем сохранение аргументов вызова клиента Баланса для последующей проверки, что же там передавалось
        List<Object[]> balanceSentArgs = new ArrayList<>();
        Answer<Void> voidAnswer = invocation -> {
            balanceSentArgs.add(invocation.getArguments());
            return null;
        };
        doAnswer(voidAnswer).when(balanceClient).editPassport(anyLong(), anyLong(), any(ClientPassportInfo.class));

        //Вызываем сервис
        clientChiefService.changeChief(newChiefUid, clientId);

        //Проверяем результат
        Integer requestedIsMainForNew = getSentIsMainProperty(balanceSentArgs, newChiefUid);
        Integer requestedIsMainForOld = getSentIsMainProperty(balanceSentArgs, oldChiefUid);
        SoftAssertions.assertSoftly(soft ->
        {
            //Изменение значения должно было вызваться как минимум дважды: для сарого и нового представителей.
            soft.assertThat(balanceSentArgs.size()).as("count of calls method editPassport")
                    .isGreaterThanOrEqualTo(2);
            soft.assertThat(requestedIsMainForNew).as("IsMain property for new chief")
                    .isEqualTo(chiefIsMain);
            soft.assertThat(requestedIsMainForOld).as("IsMain property for old chief")
                    .isEqualTo(notChiefIsMain);
        });
    }

    /**
     * Тест НЕ основных полей, которые, в соответствии с бизнес-логикой, тоже должны были поменяться при смене
     * главного представителя
     */
    @Test
    public void changeChief_secondaryFields_success() {
        //Начальное состояние системы
        long oldChiefRegion = SAINT_PETERSBURG_REGION_ID;
        updateGeoId(shard, oldChiefUid, oldChiefRegion);
        steps.userSteps().setUserProperty(newChiefUserInfo, User.SEND_CLIENT_LETTERS, false);
        steps.userSteps().setUserProperty(newChiefUserInfo, User.SEND_CLIENT_SMS, false);
        User startNewChief = userRepository.fetchByUids(shard, singleton(newChiefUid)).get(0);
        checkState(notEqual(startNewChief.getGeoId(), oldChiefRegion));

        //Вызываем сервис
        clientChiefService.changeChief(newChiefUid, clientId);

        //Проверяем результат
        User actualNewChief = userRepository.fetchByUids(shard, singleton(newChiefUid)).get(0);
        UsersOptionsSendclientletters actualLettersStatus = getSendClientLettersStatus(shard, newChiefUid);
        UsersOptionsSendclientsms actualSmsStatus = getSendClientSmsStatus(shard, newChiefUid);
        SoftAssertions.assertSoftly(soft ->
        {
            soft.assertThat(actualNewChief.getGeoId()).as("new chief region")
                    .isEqualTo(oldChiefRegion);
            soft.assertThat(actualLettersStatus).as("new chief send letters status")
                    .isEqualTo(UsersOptionsSendclientletters.Yes);
            soft.assertThat(actualSmsStatus).as("new chief send sms status")
                    .isEqualTo(UsersOptionsSendclientsms.Yes);
        });
    }

    private void updateGeoId(Integer shard, Long uid, long regionId) {
        User user = userRepository.fetchByUids(shard, singleton(uid)).get(0);
        AppliedChanges<User> appliedChanges = new ModelChanges<>(uid, User.class)
                .process(regionId, User.GEO_ID)
                .applyTo(user);
        userRepository.update(shard, singleton(appliedChanges));
    }

    private UsersOptionsSendclientletters getSendClientLettersStatus(int shard, Long uid) {
        return dslContextProvider.ppc(shard)
                .select(USERS_OPTIONS.SEND_CLIENT_LETTERS)
                .from(USERS_OPTIONS)
                .where(USERS_OPTIONS.UID.eq(uid))
                .fetchOne(USERS_OPTIONS.SEND_CLIENT_LETTERS);
    }

    private UsersOptionsSendclientsms getSendClientSmsStatus(int shard, Long uid) {
        return dslContextProvider.ppc(shard)
                .select(USERS_OPTIONS.SEND_CLIENT_SMS)
                .from(USERS_OPTIONS)
                .where(USERS_OPTIONS.UID.eq(uid))
                .fetchOne(USERS_OPTIONS.SEND_CLIENT_SMS);
    }

    public void turnOffNotifications(int shard, Long uid) {
        dslContextProvider.ppc(shard)
                .update(USERS_OPTIONS)
                .set(USERS_OPTIONS.SEND_CLIENT_LETTERS, UsersOptionsSendclientletters.No)
                .set(USERS_OPTIONS.SEND_CLIENT_SMS, UsersOptionsSendclientsms.No)
                .where(USERS_OPTIONS.UID.eq(uid))
                .execute();
    }

    @Test
    public void changeChief_checkCounterGrantsJob_success() {
        //Подготавливаем счётчик
        Long turbolandingCounterId = 999L;
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long campaignId = adGroupInfo.getCampaignId();
        TurboLanding turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());
        TextBanner textBanner = TestNewTextBanners.fullTextBanner(campaignId, adGroupInfo.getAdGroupId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES)
                .withTurboLandingId(turboLanding.getId());
        textBannerSteps.createBanner(new NewTextBannerInfo().withBanner(textBanner).withAdGroupInfo(adGroupInfo));
        steps.turboLandingSteps().addBannerToBannerTurbolandingsTableOrUpdateNew(campaignId, singletonList(textBanner));
        steps.bannerSteps().addTurbolandingMetricaCounters(shard, textBanner, singletonList(turbolandingCounterId));

        //Ожидаемый результат
        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> expectedJob =
                new DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult>()
                        .withUid(newChiefUid)
                        .withClientId(clientId)
                        .withArgs(
                                new UpdateCounterGrantsParams()
                                        .withItems(singletonList(
                                                new UpdateCounterGrantsParamsItem()
                                                        .withCounterId(turbolandingCounterId)
                                        )));

        //Выполняем тестируемый метод
        steps.dbQueueSteps().registerJobType(UPDATE_COUNTER_GRANTS_JOB);
        steps.dbQueueSteps().clearQueue(UPDATE_COUNTER_GRANTS_JOB);
        clientChiefService.changeChief(newChiefUid, clientId);

        //Проверяем появившуюся в очереди джобу
        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> actualJob =
                dbQueueRepository.grabSingleJob(shard, UPDATE_COUNTER_GRANTS_JOB);
        List<Long> actualJobUserIds = actualJob.getArgs().getItems().get(0).getUserIds();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualJob).as("update counter grants job")
                    .is(matchedBy(beanDiffer(expectedJob).useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualJobUserIds).as("uids in job")
                    .containsExactlyInAnyOrder(newChiefUid, oldChiefUid);
        });
    }

}
