package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.business.process.Bp;
import ru.yandex.market.b2bcrm.module.business.process.BpState;
import ru.yandex.market.b2bcrm.module.business.process.BpStatus;
import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointOwner;
import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointPotentialTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.PreLegalPartnerBpStatusMapping;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.PupEventProcessor;
import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.utils.Maps;

import static ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl.AbstractPupEventProcessorTest.getStringResource;

@B2bPickupPointTests
public class PreLegalPartnerPupEventProcessorTest extends AbstractPupEventTicketProcessorTest<PickupPointPotentialTicket> {

    private static final String EXPECTED_COMMENT = getStringResource("preLegalPartnerComment.html");

    private static final String EXPECTED_COMMENT_FROM_FILE = "Оферта лида ПВЗ";

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private PreLegalPartnerPupEventProcessor preLegalPartnerPupEventProcessor;

    private Service service;

    private Team team;

    private Bp bp;

    private BpStatus bpStatus;

    public PreLegalPartnerPupEventProcessorTest() {
        super("preLegalPartnerPupEvent.json");
    }

    @BeforeEach
    public void setUp() {
        super.setUp();
        service = dbService.getByNaturalId(Service.FQN, Service.CODE, "pickupPointPotential");
        team = dbService.getByNaturalId(Team.FQN, Service.CODE, "firstLinePickupPoint");
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
        configurationService.setValue("pickupPointPotentialTicketBp", bp);
        bcpService.create(PreLegalPartnerBpStatusMapping.FQN, Maps.of(
                PreLegalPartnerBpStatusMapping.CODE, "CHECKING",
                PreLegalPartnerBpStatusMapping.TITLE, "someTitle",
                PreLegalPartnerBpStatusMapping.BP_STATUS, bpStatus
        ));
    }

    @Test
    public void shouldCreateNewTicketWithComment() {
        Service diffService = dbService.getByNaturalId(Service.FQN, Service.CODE, "b2bDefaultService");
        Team diffTeam = dbService.getByNaturalId(Team.FQN, Service.CODE, "b2bFirstLineMail");

        createTicket(team, diffService, "Другая очередь", 1);
        createTicket(diffTeam, service, "Другая линия", 1);
        createTicket(team, service, "Другой ID ПО ПВЗ", 2);

        PickupPointOwner account = createAccount(40L);

        processor.process(event);
        PickupPointPotentialTicket ticket = getLastTicket();
        assertTicketAttributes(ticket);
        checkCommentsAdded(ticket, EXPECTED_COMMENT, EXPECTED_COMMENT_FROM_FILE);
        checkAttachmentAdded(ticket);
        EntityCollectionAssert.assertThat(dbService.list(Query.of(PickupPointPotentialTicket.FQN)))
                .hasSize(4)
                .anyHasAttributes(
                        PickupPointPotentialTicket.GID, ticket.getGid(),
                        PickupPointPotentialTicket.PARTNER, account.getGid()
                )

                .withFailMessage("Тикеты в других очередях должны остаться без изменений")
                .anyHasAttributes(
                        PickupPointPotentialTicket.SERVICE, diffService.getGid(),
                        PickupPointPotentialTicket.TITLE, "Другая очередь"
                )

                .withFailMessage("Тикеты на других линиях должны остаться без изменений")
                .anyHasAttributes(
                        PickupPointPotentialTicket.RESPONSIBLE_TEAM, diffTeam.getGid(),
                        PickupPointPotentialTicket.TITLE, "Другая линия"
                )

                .withFailMessage("Тикеты с другим ID ПО ПВЗ должны остаться без изменений")
                .anyHasAttributes(
                        PickupPointPotentialTicket.PUP_ID, "2",
                        PickupPointPotentialTicket.TITLE, "Другой ID ПО ПВЗ"
                );
    }

    @Test
    public void shouldNotAddCommentOnUpdate() {
        PickupPointPotentialTicket ticket = createTicket("Будет обновлен событием");
        processor.process(event);
        assertTicketAttributes(ticket);
        // один комментарий от аттачмента, который добавляется всегда
        EntityCollectionAssert.assertThat(commentTestUtils.getComments(ticket)).hasSize(1);
    }

    protected void checkCommentsAdded(Entity entity, String... bodies) {
        EntityCollectionAssert<Comment> entityCollectionAssert =
                EntityCollectionAssert.assertThat(commentTestUtils.getCommentsOfType(entity, InternalComment.class))
                        .hasSize(2);
        for (var body : bodies) {
            entityCollectionAssert = entityCollectionAssert.anyHasAttributes(
                    Comment.BODY, htmls.safeHtml(body),
                    Comment.ENTITY, entity.getGid()
            );
        }
    }

    private void checkAttachmentAdded(PickupPointPotentialTicket ticket) {
        Collection<Attachment> attachments = commentTestUtils.getCommentsOfType(ticket, InternalComment.class).stream()
                .flatMap(c -> attachmentsService.getAttachments(c).stream())
                .collect(Collectors.toList());
        EntityCollectionAssert.assertThat(attachments)
                .hasSize(1)
                .allHasAttributes(Attachment.NAME, "Оферта Вуколова.jpg",
                        Attachment.CONTENT_TYPE, "image/jpeg",
                        Attachment.SIZE, 562682L,
                        Attachment.URL, "https://pvz-int.vs.market.yandex.net/v1/pi/partners/22000449/offer");
    }

    @Override
    protected void assertTicketAttributes(PickupPointPotentialTicket ticket) {
        EntityAssert.assertThat(ticket)
                .hasAttributes(
                        PickupPointPotentialTicket.TITLE, "ООО \"Логистический Ургант\"",
                        PickupPointPotentialTicket.DESCRIPTION, EXPECTED_COMMENT,
                        PickupPointPotentialTicket.CHANNEL, "mail",
                        PickupPointPotentialTicket.RECEIPT_CHANNEL, "lb_pvz_crm",
                        PickupPointPotentialTicket.SERVICE, "pickupPointPotential",
                        PickupPointPotentialTicket.RESPONSIBLE_TEAM, "firstLinePickupPoint",
                        PickupPointPotentialTicket.CLIENT_NAME, "Ургант Иван Андреевич",
                        PickupPointPotentialTicket.CLIENT_PHONE, Phone.fromRaw("+79094516356"),
                        PickupPointPotentialTicket.CLIENT_EMAIL, "urgantia@yandex.ru",
                        PickupPointPotentialTicket.PUP_COUNT, 1L,
                        PickupPointPotentialTicket.INN, "344309228393",
                        PickupPointPotentialTicket.REGION, "Московская область",
                        PickupPointPotentialTicket.CITY, "Москва",
                        PickupPointPotentialTicket.PUP_ADDRESS, "Рижский проезд, д.4, офис 5",
                        PickupPointPotentialTicket.PUP_LATITUDE, "55.77914",
                        PickupPointPotentialTicket.PUP_LONGITUDE, "37.577921",
                        PickupPointPotentialTicket.PUP_SQUARE, BigDecimal.valueOf(75.2),
                        PickupPointPotentialTicket.PUP_CEILING_HEIGHT, BigDecimal.valueOf(3.2),
                        PickupPointPotentialTicket.PUP_PHOTO_URL, new Hyperlink("https://yadi.sk/d/58jtug_cboznta"),
                        PickupPointPotentialTicket.PUP_COMMENT, "комментарий",
                        PickupPointPotentialTicket.COOPERATION_OPTION, "Хочу подключить существующий ПВЗ",
                        PickupPointPotentialTicket.BRANDED_PUP, false,
                        PickupPointPotentialTicket.BP, bp.getGid(),
                        PickupPointPotentialTicket.CURRENT_STATUS, bpStatus.getGid(),
                        PickupPointPotentialTicket.CAMPAIGN_ID, 21960143L,
                        PickupPointPotentialTicket.OGRN, "1190280074841",
                        PickupPointPotentialTicket.PUP_ID, "1"
                );
    }

    @Override
    protected PickupPointPotentialTicket createTicket(String title) {
        return createTicket(team, service, title, 1);
    }

    @Override
    protected PupEventProcessor getProcessor() {
        return preLegalPartnerPupEventProcessor;
    }

    private PickupPointPotentialTicket createTicket(Team team, Service service, String title, long pupId) {
        return ticketTestUtils.createTicket(
                PickupPointPotentialTicket.FQN,
                team,
                service,
                Maps.of(
                        PickupPointPotentialTicket.TITLE, title,
                        PickupPointPotentialTicket.PUP_ID, pupId
                )
        );
    }
}
