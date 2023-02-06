package ru.yandex.market.supportwizard.service.alert;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.impl.DefaultMapF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.supplier.SupplierType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.StartrekSessionBuilder;
import ru.yandex.market.supportwizard.service.alert.ticket.SupplierOnboardingTicketAlertService;
import ru.yandex.market.supportwizard.storage.SupplierOnboardingAlertTicketEntity;
import ru.yandex.market.supportwizard.storage.SupplierOnboardingAlertTicketRepository;
import ru.yandex.market.supportwizard.storage.jsonb.SupplierOnboardingStepData;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.Update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.supportwizard.base.supplier.SupplierOnboardingStepType.REGISTRATION;

public class SupplierOnboardingTicketAlertServiceTest extends BaseFunctionalTest {

    private static final String TICKET_KEY = "ONBERU-1";

    private Session session = Mockito.mock(Session.class);
    private Issues issues = Mockito.mock(Issues.class);
    private Issue issue = spy(new Issue("", URI.create(""), TICKET_KEY, "", 0,
            DefaultMapF.wrap(Collections.emptyMap()), session));

    @Autowired
    private SupplierOnboardingTicketAlertService tested;
    @Autowired
    private StartrekSessionBuilder startrekSessionBuilder;
    @Autowired
    private SupplierOnboardingAlertTicketRepository ticketRepository;

    @BeforeEach
    public void init() {
        Mockito.when(startrekSessionBuilder.buildSession()).thenReturn(session);
        Mockito.when(session.issues()).thenReturn(issues);

        Mockito.when(issues.get(TICKET_KEY)).thenReturn(issue);
        doReturn("ONBERU-1").when(issue).getId();

        tested.afterPropertiesSet();
    }

    /**
     * Тестирование создание тикета.
     */
    @DbUnitDataSet
    @ParameterizedTest(name = "{0}")
    @MethodSource("argumentsForTicketCreation")
    void newTicketCreation(
            @SuppressWarnings("unused") String caseName,
            Set<SupplierOnboardingStepData> stuckSuppliers,
            Set<SupplierOnboardingStepData> suppliersFinishedStep,
            String pathToExpectedDescription) throws IOException {
        /*
        Given
         */
        doReturn(issue).when(issues).create(any());
        /*
        When
         */
        tested.alert(Instant.EPOCH, REGISTRATION, stuckSuppliers, suppliersFinishedStep);
        /*
        Then
         */
        ArgumentCaptor<IssueCreate> captor = ArgumentCaptor.forClass(IssueCreate.class);

        verify(issues).create(captor.capture());
        // Assert startrek ticket
        assertNotNull(captor.getValue());

        assertStartrekTicket(pathToExpectedDescription, captor.getValue().getValues());
        // Assert database ticket
        assertDatabaseTicket(stuckSuppliers, suppliersFinishedStep);
    }

    /**
     * Тестирование обновления тикета.
     */
    @DbUnitDataSet(before = "registrationTicket.before.csv")
    @ParameterizedTest(name = "{0}")
    @MethodSource("argumentsForTicketUpdate")
    void ticketUpdate(
            @SuppressWarnings("unused") String caseName,
            Set<SupplierOnboardingStepData> stuckSuppliers,
            Set<SupplierOnboardingStepData> suppliersFinishedStep,
            String pathToExpectedDescription,
            String pathToExpectedComment) throws IOException {
        /*
        Given
         */
        doReturn(issue).when(issue).update(any());
        /*
        When
         */
        tested.alert(Instant.EPOCH, REGISTRATION, stuckSuppliers, suppliersFinishedStep);
        /*
        Then
         */
        ArgumentCaptor<IssueUpdate> captor = ArgumentCaptor.forClass(IssueUpdate.class);

        verify(issue).update(captor.capture());

        //Assert startrek ticket
        assertNotNull(captor.getValue());
        MapF<String, Update<?>> ticketProperties = captor.getValue().getValues();

        assertTrue(((ScalarUpdate<?>) ticketProperties.getOrThrow("description")).getSet().isPresent());
        assertEquals(getFileContent(pathToExpectedDescription), ((ScalarUpdate<?>) ticketProperties.getOrThrow("description")).getSet().get());
        assertTrue(captor.getValue().getComment().isPresent());
        assertTrue(captor.getValue().getComment().get().getComment().isPresent());
        assertEquals(getFileContent(pathToExpectedComment), captor.getValue().getComment().get().getComment().get());

        //Assert database ticket
        assertDatabaseTicket(stuckSuppliers, suppliersFinishedStep);
    }

    /**
     * Тестирование обновления тикета.
     * Однако списки индетичные тем, что есть уже в базе.
     */
    @Test
    @DbUnitDataSet(before = "registrationTicket.before.csv")
    void ticketUpdate_noUpdateRequired() {
        /*
        Given
         */
        doReturn(issue).when(issue).update(any());
        /*
        When
         */
        tested.alert(Instant.EPOCH, REGISTRATION, Set.of(
                SupplierOnboardingStepData.builder()
                        .withSupplierId(1L)
                        .withSupplierName("Sup1")
                        .withPartnerType(SupplierType.DROPSHIP)
                        .withIsClickAndCollect(false)
                        .withIsPartnerApi(true)
                        .withStartTime(Instant.ofEpochMilli(12))
                        .build()), new HashSet<>());
        /*
        Then
         */
        verify(issue, never()).update(any());
    }

    private void assertDatabaseTicket(Set<SupplierOnboardingStepData> stuckSuppliers, Set<SupplierOnboardingStepData> suppliersFinishedStep) {
        Optional<SupplierOnboardingAlertTicketEntity> ticket = ticketRepository
                .findByDateAndStep(LocalDate.of(1970, 1,1), REGISTRATION);
        assertTrue(ticket.isPresent());
        assertNotNull(ticket.get().getTicketData());
        assertEquals(stuckSuppliers, ticket.get().getTicketData().getStuckOnStep());
        assertEquals(suppliersFinishedStep, ticket.get().getTicketData().getFinishedStep());
    }

    private void assertStartrekTicket(String pathToExpectedDescription,
                                      MapF<String, Object> ticketProperties) throws IOException {
        assertEquals("1970-01-01: Регистрация кабинета - застрявшие в онбординге", ticketProperties.getOrThrow("summary"));
        assertEquals("ONBERU", ticketProperties.getOrThrow("queue"));
        String[] tags = (String[]) ticketProperties.getOrThrow("tags");
        assertTrue(tags.length > 0);
        assertEquals(REGISTRATION.name(), tags[0]);
        assertEquals(getFileContent(pathToExpectedDescription), ticketProperties.getOrThrow("description"));
    }

    static Stream<Arguments> argumentsForTicketCreation() {
        return Stream.of(
                Arguments.of(
                        "Создается тикет со списком застрявших и окончивших шаг",
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(1L)
                                        .withSupplierName("Sup1")
                                        .withPartnerType(SupplierType.DROPSHIP)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .build(),
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(2L)
                                        .withSupplierName("Sup2")
                                        .withPartnerType(SupplierType.FULFILLMENT)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(false)
                                        .withStartTime(Instant.ofEpochMilli(13))
                                        .build()),
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(3L)
                                        .withSupplierName("Sup3")
                                        .withPartnerType(SupplierType.CROSSDOCK)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .withEndTime(Instant.ofEpochMilli(13))
                                        .build()),
                        "ticket/description_bothLists.txt"
                ),
                Arguments.of(
                        "Создается тикет только со списком окончивших шаг",
                        new HashSet<SupplierOnboardingStepData>(),
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(3L)
                                        .withSupplierName("Sup3")
                                        .withPartnerType(SupplierType.CROSSDOCK)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .withEndTime(Instant.ofEpochMilli(13))
                                        .build()),
                        "ticket/dscription_onlyFinished_new.txt"
                ),
                Arguments.of(
                        "Создается тикет только со списком застрявших на шаге",
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(1L)
                                        .withSupplierName("Sup1")
                                        .withPartnerType(SupplierType.DROPSHIP)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .build(),
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(2L)
                                        .withSupplierName("Sup2")
                                        .withPartnerType(SupplierType.FULFILLMENT)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(false)
                                        .withStartTime(Instant.ofEpochMilli(13))
                                        .build()),
                        new HashSet<>(),
                        "ticket/description_onlyStuck_new.txt"
                ));
    }

    static Stream<Arguments> argumentsForTicketUpdate() {
        return Stream.of(
                Arguments.of(
                        "Обновляется и список застрявших на шаге и список окончивших шаг",
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(1L)
                                        .withSupplierName("Sup1")
                                        .withPartnerType(SupplierType.DROPSHIP)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .build(),
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(2L)
                                        .withSupplierName("Sup2")
                                        .withPartnerType(SupplierType.FULFILLMENT)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(false)
                                        .withStartTime(Instant.ofEpochMilli(13))
                                        .build()),
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(3L)
                                        .withSupplierName("Sup3")
                                        .withPartnerType(SupplierType.CROSSDOCK)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .withEndTime(Instant.ofEpochMilli(13))
                                        .build()),
                        "ticket/description_bothLists.txt",
                        "ticket/comment_bothLists.txt"),
                Arguments.of(
                        "Обновляется только список застрявших на шаге",
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(1L)
                                        .withSupplierName("Sup1")
                                        .withPartnerType(SupplierType.DROPSHIP)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .build(),
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(2L)
                                        .withSupplierName("Sup2")
                                        .withPartnerType(SupplierType.FULFILLMENT)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(false)
                                        .withStartTime(Instant.ofEpochMilli(13))
                                        .build()),
                        new HashSet<>(),
                        "ticket/description_onlyStuck_upd.txt",
                        "ticket/comment_onlyStuck_upd.txt"),
                Arguments.of(
                        "Обновляется только список закончивших шаг",
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(2L)
                                        .withSupplierName("Sup2")
                                        .withPartnerType(SupplierType.FULFILLMENT)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(false)
                                        .withStartTime(Instant.ofEpochMilli(13))
                                        .build()),
                        Set.of(
                                SupplierOnboardingStepData.builder()
                                        .withSupplierId(3L)
                                        .withSupplierName("Sup3")
                                        .withPartnerType(SupplierType.CROSSDOCK)
                                        .withIsClickAndCollect(false)
                                        .withIsPartnerApi(true)
                                        .withStartTime(Instant.ofEpochMilli(12))
                                        .withEndTime(Instant.ofEpochMilli(13))
                                        .build()),
                        "ticket/description_onlyFinished_upd.txt",
                        "ticket/comment_onlyFinished_upd.txt")
        );
    }

    private String getFileContent(String pathToExpectedDescription) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(pathToExpectedDescription));
    }
}
