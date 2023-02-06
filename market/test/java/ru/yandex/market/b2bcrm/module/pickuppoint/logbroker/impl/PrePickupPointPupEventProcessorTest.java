package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.business.process.Bp;
import ru.yandex.market.b2bcrm.module.business.process.BpState;
import ru.yandex.market.b2bcrm.module.business.process.BpStatus;
import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointOwner;
import ru.yandex.market.b2bcrm.module.pickuppoint.PrePickupPointBpStatusMapping;
import ru.yandex.market.b2bcrm.module.pickuppoint.PrePickupPointTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.PupEventProcessor;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl.AbstractPupEventProcessorTest.getStringResource;

@B2bPickupPointTests
public class PrePickupPointPupEventProcessorTest extends AbstractPupEventTicketProcessorTest<PrePickupPointTicket> {

    private static final String EXPECTED_COMMENT = getStringResource("prePickupPointComment.html");

    private static final String EXPECTED_COMMENT_FROM_FILE = """
            Оферта лида ПВЗ
            """;

    @Inject
    private PrePickupPointPupEventProcessor prePickupPointPupEventProcessor;

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private AttachmentsService attachmentsService;

    @Inject
    private ObjectSerializeService serializeService;

    private Service service;

    private Team team;

    private Bp bp;

    private BpStatus bpStatus;

    public PrePickupPointPupEventProcessorTest() {
        super("prePickupPointPupEvent.json");
    }

    @BeforeEach
    public void setUp() {
        super.setUp();
        service = dbService.getByNaturalId(Service.FQN, Service.CODE, "prePickupPoint");
        team = dbService.getByNaturalId(Team.FQN, Service.CODE, "b2bFirstLinePickupPoint");
        initBp();
    }

    private void initBp() {
        bpStatus = bcpService.create(BpStatus.FQN, Maps.of(BpStatus.CODE, "firstCode", BpStatus.TITLE, "first"));
        BpState bpState = bcpService.create(BpState.FQN, Maps.of(
                BpState.TITLE, "bpState",
                BpState.START_STATUS, bpStatus,
                BpState.NEXT_STATUSES, List.of(bcpService.create(BpStatus.FQN, Maps.of(BpStatus.CODE, "secondCode", BpStatus.TITLE, "second")))
        ));
        bp = bcpService.create(Bp.FQN, Maps.of(
                Bp.CODE, "bp",
                Bp.TITLE, "bp",
                Bp.STATES, List.of(bpState)
        ));
        configurationService.setValue("prePickupPointTicketBp", bp);
        bcpService.create(PrePickupPointBpStatusMapping.FQN, Maps.of(
                PrePickupPointBpStatusMapping.CODE, "LEASE_AGREEMENT_REQUIRED",
                PrePickupPointBpStatusMapping.TITLE, "someTitle",
                PrePickupPointBpStatusMapping.BP_STATUS, bpStatus
        ));
    }

    @Test
    public void shouldCreateNewTicketWithComment() {
        Service diffService = dbService.getByNaturalId(Service.FQN, Service.CODE, "b2bDefaultService");
        Team diffTeam = dbService.getByNaturalId(Team.FQN, Service.CODE, "b2bFirstLineMail");

        createTicket(team, diffService, "Другая очередь", 2542);
        createTicket(diffTeam, service, "Другая линия", 2542);
        createTicket(team, service, "Другой ID ПО ПВЗ", 2);

        PickupPointOwner account = createAccount(42L);

        processor.process(event);
        PrePickupPointTicket ticket = getLastTicket();
        assertTicketAttributes(ticket);
        checkCommentAdded(ticket, EXPECTED_COMMENT);
        EntityCollectionAssert.assertThat(dbService.list(Query.of(PrePickupPointTicket.FQN)))
                .hasSize(4)
                .anyHasAttributes(
                        PrePickupPointTicket.GID, ticket.getGid(),
                        PrePickupPointTicket.PARTNER, account.getGid()
                )

                .withFailMessage("Тикеты в других очередях должны остаться без изменений")
                .anyHasAttributes(
                        PrePickupPointTicket.SERVICE, diffService.getGid(),
                        PrePickupPointTicket.TITLE, "Другая очередь"
                )

                .withFailMessage("Тикеты на других линиях должны остаться без изменений")
                .anyHasAttributes(
                        PrePickupPointTicket.RESPONSIBLE_TEAM, diffTeam.getGid(),
                        PrePickupPointTicket.TITLE, "Другая линия"
                )

                .withFailMessage("Тикеты с другим ID ПО ПВЗ должны остаться без изменений")
                .anyHasAttributes(
                        PrePickupPointTicket.PUP_ID, "2",
                        PrePickupPointTicket.TITLE, "Другой ID ПО ПВЗ"
                );
    }

    @Test
    public void shouldNotAddCommentOnUpdate() {
        PrePickupPointTicket ticket = createTicket("Будет обновлен событием");
        processor.process(event);
        assertTicketAttributes(ticket);
        assertThat(commentTestUtils.getComments(ticket)).isEmpty();
    }

    @Override
    protected void assertTicketAttributes(PrePickupPointTicket ticket) {
        EntityAssert.assertThat(ticket)
                .hasAttributes(
                        PrePickupPointTicket.PUP_ID, "2542",
                        PrePickupPointTicket.PUP_LEGAL_PARTNER_ID, "42",
                        PrePickupPointTicket.TITLE, "ПВЗ на Академической",
                        PrePickupPointTicket.DESCRIPTION, EXPECTED_COMMENT,
                        PrePickupPointTicket.PUP_PHONE, Phone.fromRaw("+79991234567"),
                        PrePickupPointTicket.CHANNEL, "mail",
                        PrePickupPointTicket.RECEIPT_CHANNEL, "lb_pvz_crm",
                        PrePickupPointTicket.SERVICE, "prePickupPoint",
                        PrePickupPointTicket.RESPONSIBLE_TEAM, "b2bFirstLinePickupPoint",
                        PrePickupPointTicket.BRANDED_PUP, true,
                        PrePickupPointTicket.PREPAY_ALLOWED, true,
                        PrePickupPointTicket.CASH_ALLOWED, false,
                        PrePickupPointTicket.CARD_ALLOWED, false,
                        PrePickupPointTicket.BP, bp.getGid(),
                        PrePickupPointTicket.CURRENT_STATUS, bpStatus.getGid(),
                        PrePickupPointTicket.CITY, "Сергиев Посад",
                        PrePickupPointTicket.PUP_ADDRESS, "Россия, Московская область, Сергиев Посад, " +
                                "Железнодорожная улица, 37А, строение 1, корпус 3, подъезд 1, этаж 1, офис 316",
                        PrePickupPointTicket.PUP_ADDRESS_METRO, "Пионерская",
                        PrePickupPointTicket.PUP_ADDRESS_ZIP_CODE, "141313",
                        PrePickupPointTicket.PUP_ADDRESS_INTERCOM, "1",
                        PrePickupPointTicket.PUP_LATITUDE, "56.299870",
                        PrePickupPointTicket.PUP_LONGITUDE, "38.154495",
                        PrePickupPointTicket.PUP_SQUARE, BigDecimal.valueOf(75.2),
                        PrePickupPointTicket.PUP_CEILING_HEIGHT, BigDecimal.valueOf(2.5),
                        PrePickupPointTicket.PUP_PHOTO_URL, new Hyperlink("https://disk.yandex.ru/i/6KGvEhmW1u-efw"),
                        PrePickupPointTicket.PUP_COMMENT, "Какой-то комментарий",
                        PrePickupPointTicket.PUP_RENOVATION_START_DATE, LocalDate.parse("2021-04-08"),
                        PrePickupPointTicket.PUP_RENOVATION_FINISH_DATE, LocalDate.parse("2021-05-08"),
                        PrePickupPointTicket.CLIENT_EMAIL, "urgantia@yandex.ru",
                        PrePickupPointTicket.PUP_HAS_WINDOWS, true,
                        PrePickupPointTicket.PUP_HAS_SEPARATE_ENTRANCE, false,
                        PrePickupPointTicket.PUP_HAS_STREET_ENTRANCE, true,
                        PrePickupPointTicket.PUP_FLOOR, "1",
                        PrePickupPointTicket.PUP_POLYGON_ID, "c361be60-91cc",
                        PrePickupPointTicket.WAREHOUSE_AREA, BigDecimal.valueOf(55.1),
                        PrePickupPointTicket.CLIENT_AREA, BigDecimal.valueOf(20.1)
                );
    }

    @Override
    protected PrePickupPointTicket createTicket(String title) {
        return createTicket(team, service, title, 2542);
    }

    @Override
    protected PupEventProcessor getProcessor() {
        return prePickupPointPupEventProcessor;
    }

    private PrePickupPointTicket createTicket(Team team, Service service, String title, long pupId) {
        return ticketTestUtils.createTicket(
                PrePickupPointTicket.FQN,
                team,
                service,
                Maps.of(
                        PrePickupPointTicket.TITLE, title,
                        PrePickupPointTicket.PUP_ID, pupId
                )
        );
    }
}
