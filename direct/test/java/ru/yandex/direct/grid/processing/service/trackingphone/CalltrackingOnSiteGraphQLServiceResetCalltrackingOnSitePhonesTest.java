package ru.yandex.direct.grid.processing.service.trackingphone;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.calltracking.model.CalltrackingSettings;
import ru.yandex.direct.core.entity.calltrackingsettings.repository.CalltrackingSettingsRepository;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.trackingphone.GdResetCalltrackingOnSitePhones;
import ru.yandex.direct.grid.processing.model.trackingphone.GdResetCalltrackingOnSitePhonesItem;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdResetCalltrackingOnSitePhone;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdResetCalltrackingOnSitePhonesPayload;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdResetCalltrackingOnSitePhonesPayloadItem;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static ru.yandex.direct.grid.processing.service.trackingphone.CalltrackingOnSiteGraphQLService.RESET_CALLTRACKING_ON_SITE_PHONES;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalltrackingOnSiteGraphQLServiceResetCalltrackingOnSitePhonesTest {

    private static final long COUNTER_ID_1 = RandomUtils.nextLong();
    private static final long COUNTER_ID_2 = RandomUtils.nextLong();
    private static final LocalDateTime NOW = LocalDateTime.now().withNano(0);
    private static final String PHONE_1 = ClientPhoneTestUtils.getUniqPhone();
    private static final String PHONE_2 = ClientPhoneTestUtils.getUniqPhone();
    private static final String PHONE_3 = ClientPhoneTestUtils.getUniqPhone();
    private static final Map<String, LocalDateTime> CREATE_TIME_BY_PHONE =
            Map.of(PHONE_1, NOW.minusHours(1), PHONE_2, NOW.minusHours(1));

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    items{\n"
            + "      calltrackingSettingsId\n"
            + "      success\n"
            + "    }\n"
            + "    validationResult{\n"
            + "      errors{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "      warnings{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "    }"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.
            TemplateMutation<GdResetCalltrackingOnSitePhones, GdResetCalltrackingOnSitePhonesPayload>
            RESET_CALLTRACKING_ON_SITE_PHONES_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(
            RESET_CALLTRACKING_ON_SITE_PHONES, MUTATION_TEMPLATE,
            GdResetCalltrackingOnSitePhones.class, GdResetCalltrackingOnSitePhonesPayload.class);

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private CalltrackingSettingsRepository calltrackingSettingsRepository;
    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    private GridGraphQLContext context;
    private ClientInfo clientInfo;
    private ClientId clientId;
    private int shard;
    private User user;
    private DomainInfo domain1;
    private DomainInfo domain2;
    private long calltrackingSettingId;
    private long wrongCalltrackingSettingId;
    private ClientPhone clientPhone1;
    private ClientPhone clientPhone2;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        user = userService.getUser(clientInfo.getUid());
        domain1 = steps.domainSteps().createDomain(shard);
        domain2 = steps.domainSteps().createDomain(shard);
        TestAuthHelper.setDirectAuthentication(user);
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
        MockitoAnnotations.initMocks(this);
        calltrackingSettingId = steps.calltrackingSettingsSteps()
                .add(clientId, domain1.getDomainId(), COUNTER_ID_1, CREATE_TIME_BY_PHONE, false);
        wrongCalltrackingSettingId = steps.calltrackingSettingsSteps()
                .add(clientId, domain2.getDomainId(), COUNTER_ID_2, CREATE_TIME_BY_PHONE, false);
        clientPhone1 = steps
                .clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_1, COUNTER_ID_1, CREATE_TIME_BY_PHONE.get(PHONE_1));
        clientPhone2 = steps
                .clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_2, COUNTER_ID_1, CREATE_TIME_BY_PHONE.get(PHONE_2));
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        steps.campCalltrackingPhonesSteps().add(shard, clientPhone1.getId(), campaignInfo.getCampaignId());
        steps.campCalltrackingPhonesSteps().add(shard, clientPhone2.getId(), campaignInfo.getCampaignId());
        steps.campCalltrackingSettingsSteps().link(shard, campaignInfo.getCampaignId(), calltrackingSettingId);
    }

    @After
    public void tearDown() {
        steps.clientPhoneSteps().delete(shard);
        steps.calltrackingSettingsSteps().deleteAll(shard);
        steps.campCalltrackingPhonesSteps().deleteAll(shard);
        steps.campCalltrackingPhonesSteps().deleteAll(shard);
        steps.domainSteps().delete(shard, List.of(domain1.getDomain().getDomain(), domain2.getDomain().getDomain()));
    }

    @Test
    public void resetCalltrackingOnSitePhones_success() {
        var resetItems = List.of(
                new GdResetCalltrackingOnSitePhonesItem()
                        .withCalltrackingSettingsId(calltrackingSettingId)
                        .withCalltrackingPhones(
                                List.of(new GdResetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1))),
                new GdResetCalltrackingOnSitePhonesItem()
                        .withCalltrackingSettingsId(wrongCalltrackingSettingId)
                        .withCalltrackingPhones(
                                List.of(new GdResetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1)))
        );

        var request = new GdResetCalltrackingOnSitePhones().withResetItems(resetItems);

        GdResetCalltrackingOnSitePhonesPayload result =
                processor.doMutationAndGetPayload(RESET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var successBySettingsId = listToMap(
                result.getItems(),
                GdResetCalltrackingOnSitePhonesPayloadItem::getCalltrackingSettingsId
        );
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(successBySettingsId.get(calltrackingSettingId).getSuccess()).isTrue();
            sa.assertThat(successBySettingsId.get(wrongCalltrackingSettingId).getSuccess()).isFalse();

            List<CalltrackingSettings> actualSettings = calltrackingSettingsRepository.getByIds(
                    clientId,
                    List.of(calltrackingSettingId)
            );
            for (var settings : actualSettings) {
                Map<String, LocalDateTime> actualCreateTimeByPhone = new HashMap<>();
                for (var phoneToTrack : settings.getPhonesToTrack()) {
                    actualCreateTimeByPhone.put(phoneToTrack.getPhone(), phoneToTrack.getCreateTime());
                }
                sa.assertThat(actualCreateTimeByPhone.get(PHONE_1)).isAfter(CREATE_TIME_BY_PHONE.get(PHONE_1));
                sa.assertThat(actualCreateTimeByPhone.get(PHONE_2)).isEqualTo(CREATE_TIME_BY_PHONE.get(PHONE_2));
            }
            List<ClientPhone> actualClientPhones = clientPhoneRepository.getByIds(
                    clientId,
                    List.of(clientPhone1.getId(), clientPhone2.getId())
            );
            Map<String, LocalDateTime> actualLastShowTimeByPhone = new HashMap<>();
            for (var clientPhone : actualClientPhones) {
                actualLastShowTimeByPhone.put(clientPhone.getPhoneNumber().getPhone(), clientPhone.getLastShowTime());
            }
            sa.assertThat(actualLastShowTimeByPhone.get(PHONE_1)).isAfter(CREATE_TIME_BY_PHONE.get(PHONE_1));
            sa.assertThat(actualLastShowTimeByPhone.get(PHONE_2)).isEqualTo(CREATE_TIME_BY_PHONE.get(PHONE_2));
        });
    }

    @Test
    public void resetCalltrackingOnSitePhones_success_settingsWithTwoCampaign() {
        ClientPhone clientPhone3 = steps
                .clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_1, COUNTER_ID_1, CREATE_TIME_BY_PHONE.get(PHONE_1));
        ClientPhone clientPhone4 = steps
                .clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_2, COUNTER_ID_1, CREATE_TIME_BY_PHONE.get(PHONE_2));
        CampaignInfo campaignInfo2 = steps.campaignSteps().createActiveCampaign(clientInfo);
        steps.campCalltrackingPhonesSteps().add(shard, clientPhone3.getId(), campaignInfo2.getCampaignId());
        steps.campCalltrackingPhonesSteps().add(shard, clientPhone4.getId(), campaignInfo2.getCampaignId());
        steps.campCalltrackingSettingsSteps().link(shard, campaignInfo2.getCampaignId(), calltrackingSettingId);

        var resetItems = List.of(
                new GdResetCalltrackingOnSitePhonesItem()
                        .withCalltrackingSettingsId(calltrackingSettingId)
                        .withCalltrackingPhones(
                                List.of(new GdResetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1))),
                new GdResetCalltrackingOnSitePhonesItem()
                        .withCalltrackingSettingsId(wrongCalltrackingSettingId)
                        .withCalltrackingPhones(
                                List.of(new GdResetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1)))
        );

        var request = new GdResetCalltrackingOnSitePhones().withResetItems(resetItems);

        GdResetCalltrackingOnSitePhonesPayload result =
                processor.doMutationAndGetPayload(RESET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);
        var successBySettingsId = listToMap(
                result.getItems(),
                GdResetCalltrackingOnSitePhonesPayloadItem::getCalltrackingSettingsId
        );
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(successBySettingsId.get(calltrackingSettingId).getSuccess()).isTrue();
            sa.assertThat(successBySettingsId.get(wrongCalltrackingSettingId).getSuccess()).isFalse();

            List<CalltrackingSettings> actualSettings = calltrackingSettingsRepository.getByIds(
                    clientId,
                    List.of(calltrackingSettingId)
            );
            for (var settings : actualSettings) {
                Map<String, LocalDateTime> actualCreateTimeByPhone = new HashMap<>();
                for (var phoneToTrack : settings.getPhonesToTrack()) {
                    actualCreateTimeByPhone.put(phoneToTrack.getPhone(), phoneToTrack.getCreateTime());
                }
                sa.assertThat(actualCreateTimeByPhone.get(PHONE_1)).isAfter(CREATE_TIME_BY_PHONE.get(PHONE_1));
                sa.assertThat(actualCreateTimeByPhone.get(PHONE_2)).isEqualTo(CREATE_TIME_BY_PHONE.get(PHONE_2));
            }
            List<ClientPhone> actualClientPhones = clientPhoneRepository.getByIds(
                    clientId,
                    List.of(clientPhone1.getId(), clientPhone2.getId(), clientPhone3.getId(), clientPhone4.getId())
            );
            Map<String, List<LocalDateTime>> actualLastShowTimeByPhone = new HashMap<>();
            for (var clientPhone : actualClientPhones) {
                actualLastShowTimeByPhone
                        .computeIfAbsent(clientPhone.getPhoneNumber().getPhone(), id -> new ArrayList<>())
                        .add(clientPhone.getLastShowTime());
            }
            actualLastShowTimeByPhone.get(PHONE_1)
                    .forEach(actualLastShowTime -> actualLastShowTime.isAfter(CREATE_TIME_BY_PHONE.get(PHONE_1)));
            actualLastShowTimeByPhone.get(PHONE_2)
                    .forEach(actualLastShowTime -> actualLastShowTime.isAfter(CREATE_TIME_BY_PHONE.get(PHONE_2)));
        });
    }

    @Test
    public void resetCalltrackingOnSitePhones_wrongInputSettingsId() {
        var resetItems = List.of(new GdResetCalltrackingOnSitePhonesItem()
                .withCalltrackingSettingsId(wrongCalltrackingSettingId)
                .withCalltrackingPhones(List.of(new GdResetCalltrackingOnSitePhone().withRedirectPhone(PHONE_1))));

        var request = new GdResetCalltrackingOnSitePhones().withResetItems(resetItems);

        GdResetCalltrackingOnSitePhonesPayload result =
                processor.doMutationAndGetPayload(RESET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);

        SoftAssertions.assertSoftly(sa -> result.getItems()
                .forEach(item -> sa.assertThat(item.getSuccess()).isFalse()));
    }

    @Test
    public void resetCalltrackingOnSitePhones_wrongInputPhoneList() {
        ArrayList<GdResetCalltrackingOnSitePhonesItem> resetItems = new ArrayList<>();
        resetItems.add(new GdResetCalltrackingOnSitePhonesItem()
                .withCalltrackingSettingsId(calltrackingSettingId)
                .withCalltrackingPhones(List.of(
                        new GdResetCalltrackingOnSitePhone().withRedirectPhone(PHONE_2),
                        new GdResetCalltrackingOnSitePhone().withRedirectPhone(PHONE_3)
                )));
        var request = new GdResetCalltrackingOnSitePhones().withResetItems(resetItems);

        GdResetCalltrackingOnSitePhonesPayload result =
                processor.doMutationAndGetPayload(RESET_CALLTRACKING_ON_SITE_PHONES_MUTATION, request, user);

        SoftAssertions.assertSoftly(sa -> result.getItems()
                .forEach(item -> sa.assertThat(item.getSuccess()).isFalse()));
    }

}
