package ru.yandex.direct.core.entity.user.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.request.FindClientRequest;
import ru.yandex.direct.balance.client.model.request.ListPaymentMethodsSimpleRequest;
import ru.yandex.direct.balance.client.model.response.ListPaymentMethodsSimpleResponseItem;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParams;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParamsItem;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsResult;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.user.model.BlackboxUser;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewTextBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TextBannerSteps;
import ru.yandex.direct.core.testing.stub.PassportClientStub;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddUserServiceTest {

    @Autowired
    private PassportClientStub passportClientStub;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private ShardSupport shardSupport;
    @Autowired
    private BalanceClient balanceClient;
    @Autowired
    private BlackboxUserService blackboxUserService;
    @Autowired
    private AddUserService addUserService;
    @Autowired
    private DbQueueRepository dbQueueRepository;
    @Autowired
    private TextBannerSteps textBannerSteps;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private Integer shard;
    private String blackboxLogin;
    private Long blackboxUid;
    private Long chiefUid;
    private BlackboxUser blackboxUser;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        clientId = clientInfo.getClientId();
        chiefUid = clientInfo.getClient().getChiefUid();
        shard = clientInfo.getShard();
        blackboxUser = steps.userSteps().createBlackboxUserInBlackboxStub();
        blackboxUid = blackboxUser.getUid();
        blackboxLogin = passportClientStub.getLoginByUid(blackboxUid);

        // Мокаем хождение во внешние сервисы внутри AddUserService и AddUserValidationService
        when(blackboxUserService.getUsersInfo(anyList()))
                .thenAnswer(args -> singletonMap(blackboxUid, blackboxUser));
        when(balanceClient.listPaymentMethodsSimple(any(ListPaymentMethodsSimpleRequest.class)))
                .thenReturn(
                        new ListPaymentMethodsSimpleResponseItem()
                                .withPaymentMethods(emptyMap()));
        when(balanceClient.findClient(any(FindClientRequest.class)))
                .thenReturn(emptyList());
    }

    @After
    public void tearDown() {
        // Отвязываем наше моканье хождения во внешние сервисы, чтобы не мешать другим тестам.
        Mockito.reset(blackboxUserService);
        Mockito.reset(balanceClient);
    }

    @Test
    public void addUserFromBlackbox_checkDataBase_success() {
        //Вызываем тестируемый метод сервиса
        Result<User> userResult = addUserService.addUserFromBlackbox(blackboxUid, clientId);
        checkState(userResult.isSuccessful(), "Result is not successful");

        //Проверяем получившееся состояние системы
        User resultUser = userResult.getResult();
        User actualUser = userRepository.fetchByUids(shard, singleton(blackboxUid)).get(0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(resultUser.getUid()).as("result user uid").isEqualTo(blackboxUser.getUid());
            soft.assertThat(resultUser.getLogin()).as("result user login").isEqualTo(blackboxUser.getLogin());
            soft.assertThat(resultUser.getClientId()).as("result user client id").isEqualTo(clientId);

            soft.assertThat(actualUser.getUid()).as("actualUser user uid").isEqualTo(blackboxUser.getUid());
            soft.assertThat(actualUser.getLogin()).as("actualUser user login").isEqualTo(blackboxUser.getLogin());
            soft.assertThat(actualUser.getClientId()).as("actualUser user client id").isEqualTo(clientId);
        });
    }

    /**
     * Тест упадёт, если при создании нового пользователя почему-то перестали ассоциировать пользователя с клиентом
     * в Балансе.
     */
    @Test
    public void addUserFromBlackbox_checkBalanceAssociated_success() {
        ArrayList<Object> balanceAssociationArgs = new ArrayList<>();

        Answer<Void> voidAnswer = invocation -> {
            balanceAssociationArgs.addAll(Arrays.asList(invocation.getArguments()));
            return null;
        };
        doAnswer(voidAnswer).when(balanceClient).createUserClientAssociation(anyLong(), anyLong(), anyLong());
        addUserService.addUserFromBlackbox(blackboxUid, clientId);

        assertThat(balanceAssociationArgs).as("operatorUid, clientId, representativeUid")
                .containsExactly(chiefUid, clientId.asLong(), blackboxUid);
    }

    @Test
    public void addUserFromBlackbox_checkCounterGrantsJob_success() {
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
                        .withUid(chiefUid)
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
        addUserService.addUserFromBlackbox(blackboxUid, clientId);

        //Проверяем появившуюся в очереди джобу
        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> actualJob =
                dbQueueRepository.grabSingleJob(shard, UPDATE_COUNTER_GRANTS_JOB);
        List<Long> actualJobUserIds = actualJob.getArgs().getItems().get(0).getUserIds();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualJob).as("update counter grants job")
                    .is(matchedBy(beanDiffer(expectedJob).useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualJobUserIds).as("uids in job")
                    .containsExactlyInAnyOrder(chiefUid, blackboxUid);
        });
    }

    @Test
    public void addUserFromBlackbox_checkNullUser_success() {
        when(blackboxUserService.getUsersInfo(anyList()))
                .thenAnswer(args -> singletonMap(blackboxUid, null));

        assertThatCode(() -> addUserService.addUserFromBlackbox(blackboxUid, clientId))
                .doesNotThrowAnyException();
    }
}
