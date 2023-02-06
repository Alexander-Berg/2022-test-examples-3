package ru.yandex.market.logistics.mqm.service.processor.qualityrule;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.BaseFlow;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO;
import ru.yandex.startrek.client.AuthenticatingStartrekClient;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Transition;
import ru.yandex.startrek.client.model.Update;
import ru.yandex.startrek.client.utils.StartrekClientModule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.logistics.mqm.utils.CacheUtilsKt.clearCache;

@DisplayName("Тесты обработчика ДШ-СЦ для создания и обновления тикетов просрочки приемки")
class DropshipScIntakeQualityRuleProcessorTest extends StartrekProcessorTest {

    private static final Long TEST_SHOP_ID = 12345678L;
    private static final Set<String> TEST_EMAILS = Set.of("test1@mail.com", "test2@mail.com");
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;
    @Autowired
    @Qualifier("mbiApiClientLogged")
    private MbiApiClient mbiApiClient;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    public void setUp() {
        clearCache(caffeineCacheManager);
        // Нужно для создания Attachment
        mapper.registerModule(new StartrekClientModule(new EmptyMap<>()));
        mapper.setInjectableValues(new AuthenticatingStartrekClient.ClientInjectableValues(startrekSession));

        clock.setFixed(Instant.parse("2020-11-07T07:00:10.00Z"), MOSCOW_ZONE);
        when(startrekSession.issues()).thenReturn(issues);
        when(startrekSession.transitions()).thenReturn(transitions);
        when(startrekSession.comments()).thenReturn(comments);
        when(startrekSession.attachments()).thenReturn(attachments);
    }

    @DisplayName("Создание тикета в Startrek для одного просроченного план-факта приемки ДШ-СД")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/intake/dropship_sc/create_ticket_with_one_planfact.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_sc/create_ticket_with_one_planfact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSingleIssueTest() {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        when(issues.create(any()))
            .thenReturn(new Issue(null, null, "MONITORINGSNDBX-1", null, 1, new EmptyMap<>(), null));
        mockLogisticsPointsResponse(10003345341L);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary")).isEqualTo(
            "[MQM] 07.11.2020: СЦ-партнеры вовремя не приняли заказы, отгруженные ДШ Тестовый dropship (id: 12345678)."
        );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(
                String.join(
                    "\n",
                    "https://abo.market.yandex-team.ru/order/777",
                    "https://ow.market.yandex-team.ru/order/777",
                    "https://lms-admin.market.yandex-team.ru/lom/orders/100111\n",
                    "Дата создания заказа: 01-11-2020",
                    "Дедлайн приемки СЦ Тестовый сортировочный центр: 07-11-2020 10:00:00\n",
                    "Трек ДШ: 101",
                    "Трек СЦ Тестовый сортировочный центр: 102",
                    "Email главного представителя: test1@mail.com,test2@mail.com",
                    "Расписание ДШ: пн 10:00-19:00;вт 10:00-18:00"
                )
            );
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(1);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777");
        softly.assertThat((String[]) values.getOrThrow("tags"))
            .containsExactly(
                "Тестовый dropship:ДШ",
                "Тестовый сортировочный центр:СЦ",
                "Лог. точка ДШ:10003345341"
            );
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_CUSTOMER_EMAIL))
            .isEqualTo("test1@mail.com");
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_SHOP_ID))
            .isEqualTo(TEST_SHOP_ID);

        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID);
    }

    @DisplayName("Создание тикета в Startrek для просроченных план-фактов приемки ДШ-СЦ.")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/intake/dropship_sc/create_ticket_with_some_planfacts.xml"
    )
    @ExpectedDatabase(
        value =
            "/service/processor/qualityrule/after/intake/dropship_sc/create_ticket_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAggregatedIssueTest() throws IOException {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        Attachment attachment = mapper.readValue("{}", Attachment.class);

        when(issues.create(any()))
            .thenReturn(new Issue(null, null, "MONITORINGSNDBX-1", null, 1, new EmptyMap<>(), null));

        when(attachments.upload(anyString(), any())).thenReturn(attachment);

        mockLogisticsPointsResponse(10003345341L);
        mockLogisticsPointsResponse(10003345345L);

        handleGroups();
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(issues).create(captor.capture());

        IssueCreate issueCreate = captor.getValue();
        MapF<String, Object> values = issueCreate.getValues();

        softly.assertThat(values.getOrThrow("summary")).isEqualTo(
            "[MQM] 07.11.2020: СЦ-партнеры вовремя не приняли заказы, отгруженные ДШ Тестовый dropship (id: 12345678)."
        );
        softly.assertThat(values.getOrThrow("description"))
            .isEqualTo(String.join(
                "\n",
                "Список заказов в приложении (2 шт.)",
                "Расписание ДШ: пн 10:00-19:00;вт 10:00-18:00"
            ));
        softly.assertThat(values.getOrThrow("queue"))
            .isEqualTo("MONITORINGSNDBX");
        softly.assertThat(values.getOrThrow("defectOrders"))
            .isEqualTo(2);
        softly.assertThat(values.getOrThrow("customerOrderNumber"))
            .isEqualTo("777, 778");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) values.getOrThrow("tags");
        softly.assertThat(tags)
            .containsOnly(
                "Тестовый сортировочный центр:СЦ",
                "Тестовый dropship:ДШ",
                "Лог. точка ДШ:10003345345",
                "Лог. точка ДШ:10003345341"
            );
        verify(attachments).upload(anyString(), any());
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_CUSTOMER_EMAIL))
            .isEqualTo("test1@mail.com");
        softly.assertThat(values.getOrThrow(BaseFlow.FIELD_SHOP_ID))
            .isEqualTo(TEST_SHOP_ID);

        verify(mbiApiClient).getPartnerSuperAdmin(TEST_SHOP_ID);
    }

    @DisplayName("Добавление комментариев в тикете Startrek для просроченных план-фактов приемки ДШ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropship_sc/comment_with_some_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_sc/comment_with_some_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void commentAggregatedIssueTest() throws IOException {
        when(mbiApiClient.getPartnerSuperAdmin(TEST_SHOP_ID))
            .thenReturn(new BusinessOwnerDTO(1, 2, "test", TEST_EMAILS));
        Attachment attachment = mapper.readValue("{}", Attachment.class);
        when(attachments.upload(anyString(), any())).thenReturn(attachment);

        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(issues).update(eq("MONITORINGSNDBX-1"), captor.capture());

        IssueUpdate issueUpdate = captor.getValue();
        String commentString = issueUpdate.getComment().get().getComment().get();
        MapF<String, Update<?>> values = issueUpdate.getValues();

        softly.assertThat(commentString).isEqualTo(String.join(
            "\n",
            "Информация в тикете была автоматически изменена.",
            "",
            "Удалены неактуальные заказы (1 шт.): 778.",
            "Добавлены новые заказы (1 шт.): 779.",
            "Список заказов в приложении (2 шт.)."
        ));

        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("customerOrderNumber")).getSet().get())
            .isEqualTo("777, 778, 779");
        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("defectOrders")).getSet().get())
            .isEqualTo(3);

        verify(attachments).upload(anyString(), any());
    }

    @DisplayName("Закрытие тикета Startrek для просроченных план-фактов приемки ДШ-СЦ")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropship_sc/close_all_planfacts.xml")
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_sc/close_all_planfacts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void closeAllAggregatedIssueTest() {
        handleGroups();
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);
        verify(transitions).execute(eq("MONITORINGSNDBX-1"), any(String.class), captor.capture());

        IssueUpdate issueUpdate = captor.getValue();
        String commentString = issueUpdate.getComment().get().getComment().get();
        MapF<String, Update<?>> values = issueUpdate.getValues();

        softly.assertThat(commentString).isEqualTo("Тикет автоматически закрыт.");

        softly.assertThat(((ScalarUpdate<?>) values.getOrThrow("resolution")).getSet().get())
            .isEqualTo("can'tReproduce");
    }

    @DisplayName("Переоткрытие тикета, если проблемы по нему актуальны")
    @Test
    @DatabaseSetup("/service/processor/qualityrule/before/intake/dropship_sc/reopen_ticket.xml")
    void reopenClosedIssueFlowTest() {
        when(issueStatusRef.getKey()).thenReturn("closed");

        Transition transition = Mockito.mock(Transition.class);
        when(transitions.get("MONITORINGSNDBX-1", "reopen")).thenReturn(transition);

        handleGroups();
        verify(transitions, times(1)).execute(eq("MONITORINGSNDBX-1"), eq(transition));
    }

    @DisplayName("Игнорирование группировки до начала часов создания тикетов.")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/intake/dropship_sc/create_ticket_with_one_planfact.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_sc/create_ticket_with_one_planfact_ignored.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void ignoreAggregationBeforeTicketCreationTime() {
        clock.setFixed(Instant.parse("2020-11-07T06:59:10.00Z"), MOSCOW_ZONE);

        handleGroups();
        verify(issues, never()).create(any());
    }

    @DisplayName("Игнорирование группировки после часов создания тикетов.")
    @Test
    @DatabaseSetup(
        "/service/processor/qualityrule/before/intake/dropship_sc/create_ticket_with_one_planfact.xml"
    )
    @ExpectedDatabase(
        value = "/service/processor/qualityrule/after/intake/dropship_sc/create_ticket_with_one_planfact_ignored.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void ignoreAggregationAfterTicketCreationTime() {
        clock.setFixed(Instant.parse("2020-11-06T16:01:10.00Z"), MOSCOW_ZONE);

        handleGroups();
        verify(issues, never()).create(any());
    }

    private static Set<ScheduleDayResponse> createScheduleDayResponse() {
        return Set.of(
            new ScheduleDayResponse(1L, 1, LocalTime.of(10, 0), LocalTime.of(19, 0)),
            new ScheduleDayResponse(2L, 2, LocalTime.of(10, 0), LocalTime.of(18, 0))
        );
    }

    private void mockLogisticsPointsResponse(Long logisticsPointId) {
        when(lmsClient.getLogisticsPoint(logisticsPointId))
            .thenReturn(
                Optional.of(LogisticsPointResponse.newBuilder()
                    .schedule(createScheduleDayResponse())
                    .build()
                ));
    }
}
