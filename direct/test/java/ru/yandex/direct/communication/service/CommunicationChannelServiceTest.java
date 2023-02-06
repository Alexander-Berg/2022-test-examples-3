package ru.yandex.direct.communication.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.ads.bsyeti.libs.communications.EChannel;
import ru.yandex.ads.bsyeti.libs.communications.ESourceType;
import ru.yandex.ads.bsyeti.libs.communications.TEventSource;
import ru.yandex.ads.bsyeti.libs.communications.proto.TMessageData;
import ru.yandex.direct.autobudget.restart.service.AutobudgetRestartService;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.communication.CommunicationChannelRepository;
import ru.yandex.direct.communication.CommunicationClient;
import ru.yandex.direct.communication.container.CommunicationMessageData;
import ru.yandex.direct.communication.facade.CommunicationEventVersionProcessingFacade;
import ru.yandex.direct.communication.facade.CommunicationEventVersionProcessingFacadeBuilder;
import ru.yandex.direct.communication.facade.impl.actual.DefaultActualChecker;
import ru.yandex.direct.communication.facade.impl.actual.DummyActualChecker;
import ru.yandex.direct.communication.facade.impl.formatter.DefaultFormatter;
import ru.yandex.direct.communication.facade.impl.status.DefaultStatusChecker;
import ru.yandex.direct.communication.inventory.CommunicationInventoryClient;
import ru.yandex.direct.communication.model.inventory.ObjectEventData;
import ru.yandex.direct.communication.model.inventory.Request;
import ru.yandex.direct.communication.model.inventory.Response;
import ru.yandex.direct.communication.model.inventory.SlotResponse;
import ru.yandex.direct.communication.repository.CommunicationSlotRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientWithUsers;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.communication.model.ButtonConfig;
import ru.yandex.direct.core.entity.communication.model.CommunicationEventVersion;
import ru.yandex.direct.core.entity.communication.model.CommunicationEventVersionStatus;
import ru.yandex.direct.core.entity.communication.model.Condition;
import ru.yandex.direct.core.entity.communication.repository.CommunicationEventVersionsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.i18n.Translatable;
import ru.yandex.direct.rbac.RbacService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class CommunicationChannelServiceTest {

    private static final long EVENT_ID = 15;
    private static final long EVENT_VERSION_ID = 9;

    private CommunicationChannelService service;

    @Mock
    private CommunicationInventoryClient inventoryClient;

    @Mock
    private CommunicationClient communicationClient;

    @Mock
    private CommunicationChannelRepository channelRepository;

    @Mock
    private CommunicationEventVersionsRepository eventVersionsRepository;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    PpcPropertiesSupport propertiesSupport;

    @Mock
    private FeatureService featureService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientNdsService clientNdsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RbacService rbacService;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AutobudgetRestartService autobudgetRestartService;

    @Mock
    private TranslationService translationService;

    @Autowired
    private CommunicationEventVersionProcessingFacade processingFacade;

    private CommunicationSlotRepository slotRepository;

    @Before
    public void setUp() throws ExecutionException, TimeoutException {
        openMocks(this);
        slotRepository = new CommunicationSlotRepository();
        when(campaignRepository.getCampaigns(anyInt(), anyList())).thenReturn(Collections.emptyList());
        when(communicationClient.send(anyList())).thenReturn(1);
        when(translationService.translate(any(Translatable.class), any(Locale.class))).thenReturn("translation");
        processingFacade = new CommunicationEventVersionProcessingFacadeBuilder()
                .withActualCheckers(List.of(
                        new DefaultActualChecker(),
                        new DummyActualChecker()
                ))
                .withStatusCheckers(List.of(new DefaultStatusChecker(communicationClient)))
                .withFormatters(List.of(new DefaultFormatter(translationService)))
                .withCampaignRepository(campaignRepository)
                .build();

        var eventVersions = List.of(new CommunicationEventVersion()
                .withEventId(EVENT_ID)
                .withIter(EVENT_VERSION_ID)
                .withStatus(CommunicationEventVersionStatus.ACTIVE)
                .withSlots(List.of(3L))
                .withCheckActual("DUMMY")
                .withFormatName("default")
                .withIsForAll(true));
        when(inventoryClient.getRecommendations(any(Request.class)))
                .thenReturn(Response.newBuilder()
                        .addSlotResponses(SlotResponse.newBuilder()
                                .setSlotId(3L)
                                .addObjectEventData(ObjectEventData.newBuilder()
                                        .setEventId(EVENT_ID)
                                        .setEventVersionId(EVENT_VERSION_ID)
                                        .setObjectId(1L)
                                        .build())
                                .build())
                        .build());
        when(eventVersionsRepository.getVersionsByStatuses(anyList()))
                .thenReturn(eventVersions);
        when(eventVersionsRepository.getVersions(
                argThat(map -> map.getOrDefault(EVENT_ID, List.of()).contains(EVENT_VERSION_ID))
        )).thenReturn(eventVersions);
        when(featureService.getEnabledForClientId(any(ClientId.class)))
                .thenReturn(Set.of());
        when(autobudgetRestartService.getActualRestartTimes(anyInt(), anyMap()))
                .thenReturn(Collections.emptyList());
        when(clientRepository.getClientData(anyInt(), anyList()))
                .thenReturn(List.of(new ClientWithUsers(new Client()
                        .withWorkCurrency(CurrencyCode.RUB),
                        List.of())));
        when(rbacService.canWrite(anyLong(), anyLong()))
                .thenReturn(true);
        PpcProperty<Boolean> ppcProp = Mockito.mock(PpcProperty.class);
        when(ppcProp.getOrDefault(anyBoolean())).thenReturn(false);
        when(propertiesSupport.get(any(PpcPropertyName.class), any(Duration.class)))
                .thenReturn(ppcProp);
        service = new CommunicationChannelService(shardHelper, propertiesSupport, featureService, clientRepository,
                clientNdsService, userRepository, rbacService, channelRepository, eventVersionsRepository, slotRepository, processingFacade,
                inventoryClient, autobudgetRestartService, null, null, null);
    }

    @Test
    public void getCommunicationMessage() {
        var result = service.getCommunicationMessage(ClientId.fromLong(1), 1L, Set.of("wallet_balance"), Collections.emptyList(), "ru");
        assertThat(result).isNotEmpty();
    }

    @Test
    public void getCommunicationMessage_eventVersionNotFound() {
        eventVersionsRepository = mock(CommunicationEventVersionsRepository.class);
        when(eventVersionsRepository.getVersions(
                argThat(map -> map.getOrDefault(EVENT_ID, List.of()).contains(EVENT_VERSION_ID))
        )).thenReturn(List.of());
        service = new CommunicationChannelService(shardHelper, propertiesSupport, featureService, clientRepository,
                clientNdsService, userRepository, rbacService, channelRepository, eventVersionsRepository, slotRepository, processingFacade,
                inventoryClient, autobudgetRestartService, null, null, null);
        var result = service.getCommunicationMessage(ClientId.fromLong(1), 1L, Set.of("wallet_balance"), Collections.emptyList(), "ru");
        assertThat(result).isEmpty();
    }

    @Test
    public void getCommunicationMessage_abortVersion() {
        eventVersionsRepository.getVersions(Map.of(EVENT_ID, List.of(EVENT_VERSION_ID))).get(0)
                .withStatus(CommunicationEventVersionStatus.NEW_);
        var result = service.getCommunicationMessage(ClientId.fromLong(1), 1L, Set.of("wallet_balance"), Collections.emptyList(), "ru");
        assertThat(result).isEmpty();
    }

    @Test
    public void getCommunicationMessage_not_actual() {
        eventVersionsRepository.getVersions(Map.of(EVENT_ID, List.of(EVENT_VERSION_ID))).get(0)
                .withCheckActual("default");
        var result = service.getCommunicationMessage(ClientId.fromLong(1), 1L, Set.of("wallet_balance"), Collections.emptyList(), "ru");
        assertThat(result).isEmpty();
    }

    @Test
    public void getCommunicationMessage_actual() {
        var result = service.getCommunicationMessage(ClientId.fromLong(1), 1L, Set.of("wallet_balance"), Collections.emptyList(), "ru");
        assertThat(result).isNotEmpty();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getCommunicationMessage_formatter_not_found() {
        eventVersionsRepository.getVersions(Map.of(EVENT_ID, List.of(EVENT_VERSION_ID))).get(0).withFormatName("invalid");
        var result = service.getCommunicationMessage(ClientId.fromLong(1), 1L, Set.of("wallet_balance"), Collections.emptyList(), "ru");
        assertThat(result).isEmpty();
    }

    @Test
    public void getCommunicationMessage_disabled_button() {
        var trueCond = new Condition().withLeftValue("qwe").withRightValue("qwe");
        var falseCond = new Condition().withLeftValue("qwe").withRightValue("ewq");
        var enableConditions = new ArrayList<Condition>();
        var expectedDisabledResults = new ArrayList<Boolean>();
        Map.of(
                trueCond, true,
                falseCond, false,
                new Condition().withNotSubCondition(trueCond), false,
                new Condition().withNotSubCondition(falseCond), true,
                new Condition().withAndSubConditions(List.of(trueCond, trueCond, trueCond)), true,
                new Condition().withAndSubConditions(List.of(trueCond, falseCond, trueCond)), false,
                new Condition().withOrSubConditions(List.of(falseCond, falseCond, falseCond)), false,
                new Condition().withOrSubConditions(List.of(falseCond, trueCond, falseCond)), true
        ).forEach((condition, result) -> {
            enableConditions.add(condition);
            expectedDisabledResults.add(!result);
        });
        enableConditions.add(null);
        expectedDisabledResults.add(false);
        eventVersionsRepository.getVersions(Map.of(EVENT_ID, List.of(EVENT_VERSION_ID))).get(0)
                .withButtonConfigs(mapList(enableConditions, c -> new ButtonConfig()
                        .withAction("BUTTON_ACTION_TYPE_ACTION")
                        .withStyle("BUTTON_STYLE_DEFAULT")
                        .withEnableCondition(c)));
        var result = service.getCommunicationMessage(ClientId.fromLong(1), 1L, Set.of("wallet_balance"), Collections.emptyList(), "ru");
        assertThat(result).isNotEmpty();
        var buttons = result.get(0).getContent().getButtons();
        assertThat(buttons).isNotEmpty();
        for (int i = 0; i < expectedDisabledResults.size(); i++) {
            var expectedDisabled = expectedDisabledResults.get(i);
            var actualDisabled = buttons.get(i).isDisabled();
            var condition = enableConditions.get(i);
            assertThat(actualDisabled).as("Проверяем вычисление условия " + condition).isEqualTo(expectedDisabled);
        }
    }

    private CommunicationMessageData createCommunicationMessageData(
            long messageId,
            long userId,
            long targetEntityId,
            long eventId,
            long versionId
    ) {
        CommunicationMessageData data = mock(CommunicationMessageData.class);
        when(data.getChannel()).thenReturn(EChannel.DIRECT_WEB_UI);
        when(data.getMessageId()).thenReturn(messageId);
        when(data.getUserId()).thenReturn(userId);
        when(data.getTargetEntityId()).thenReturn(targetEntityId);
        when(data.getEventId()).thenReturn(eventId);
        when(data.getSource()).thenReturn(TEventSource.newBuilder()
                .setType(ESourceType.DIRECT_OFFLINE_REGULAR)
                .setId(versionId)
                .build());
        when(data.getMessageData()).thenReturn(TMessageData.getDefaultInstance());
        return data;
    }
}
