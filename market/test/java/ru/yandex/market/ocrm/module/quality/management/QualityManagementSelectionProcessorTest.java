package ru.yandex.market.ocrm.module.quality.management;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.quality.management.domain.QualityManagementSelection;
import ru.yandex.market.ocrm.module.quality.management.domain.QualityManagementTicket;
import ru.yandex.market.ocrm.module.quality.management.impl.QualityManagementSelectionProcessor;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ModuleQualityManagementTestConfiguration.class)
public class QualityManagementSelectionProcessorTest {

    private static final Fqn TEST_FQN = Fqn.of("ticket$testQM");
    private final String surveyText = "{ \"gid\": \"survey@122659184\"," +
            "    \"version\": \"2020-11-14T17:00:23.833+03:00\"," +
            "    \"title\": \"Анкета для тестирования\"," +
            "    \"description\": \"Эта анкета была специально создана, чтобы протестировать работоспособность\"," +
            "    \"maxScore\": 11," +
            "    \"score\": 0," +
            "    \"questionAnswers\": {" +
            "      \"surveyQuestion@123473886\": {" +
            "        \"value\": {" +
            "          \"gid\": \"surveyAnswerOption@123478985\"," +
            "          \"title\": \"Выпадающий список вариант 1\"" +
            "        }," +
            "        \"question\": {" +
            "          \"gid\": \"surveyQuestion@123473886\"," +
            "          \"title\": \"Обязательный вопрос с выдающим списком (!)\"," +
            "          \"description\": \"Описание обязательного вопроса с выдающим списком (!)\"," +
            "          \"required\": true," +
            "          \"type\": \"DROPDOWN\"," +
            "          \"maxScore\": 0," +
            "          \"priority\": 4" +
            "        }" +
            "      }" +
            "    }" +
            "  }";
    @Inject
    ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private DbService dbService;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private TxService txService;
    @Inject
    private QualityManagementSelectionProcessor qualityManagementSelectionProcessor;
    private Channel channel1;
    private Service service1;
    private TicketCategory category1;
    private Ou ou1;
    private Team team1;

    @BeforeAll
    public void before() {
        txService.runInNewTx(() -> {
                    serviceTimeTestUtils.createDefaultServiceTimeWithPeriodByCode("9_21");
                    channel1 = dbService.getByNaturalId(Channel.FQN, Channel.CODE, Channel.MAIL);
                    ou1 = ticketTestUtils.createOu();
                    team1 = ticketTestUtils.createTeam();
                    service1 = ticketTestUtils.createService24x7(team1);
                    category1 = ticketTestUtils.createTicketCategory(service1.getBrand());
                }
        );
    }

    //https://testpalm2.yandex-team.ru/testcase/ocrm-815 Отображение результатов проверок КК в профиле
    @Test
    public void publishQualityManagementSelection() {
        //Создаем выборку и 2 тикета, подходящие под эту выборку
        QualityManagementSelection selection = createQualityManagementSelection();

        //Стартуем процесс создания Тикетов КК
        qualityManagementSelectionProcessor.process();

        Query query = Query.of(QualityManagementTicket.FQN)
                .withFilters(Filters.eq(QualityManagementTicket.ENTITY, selection));
        List<Ticket> tickets = txService.doInNewReadOnlyTx(() -> dbService.list(query));
        //Проверяем, что создалось имеено 2 тикета КК
        Assertions.assertEquals(2, tickets.size());

        String gid1 = tickets.get(0).getGid();
        String gid2 = tickets.get(1).getGid();

        txService.runInNewTx(() -> {
            //Первый тикет скипаем
            bcpService.edit(gid1, Maps.of(Ticket.STATUS, QualityManagementTicket.STATUS_PROCESSING));
            bcpService.edit(gid1, Maps.of(Ticket.STATUS, QualityManagementTicket.STATUS_SKIPPED));
            //Второй тикет оцениваем
            bcpService.edit(gid2, Maps.of(Ticket.STATUS, QualityManagementTicket.STATUS_PROCESSING));
            bcpService.edit(gid2, Maps.of(Ticket.STATUS, Ticket.STATUS_RESOLVED,
                    QualityManagementTicket.SURVEY_RESPONSE_DATA, surveyText));
        });

        //Запускамем процесс поиска оцененых выборок КК
        qualityManagementSelectionProcessor.grade();

        //Проверяем, что статус изменился на Оценен
        txService.runInNewTx(() -> {
            String selectionStatus = ((QualityManagementSelection) dbService.get(selection.getGid())).getStatus();
            Assertions.assertEquals(QualityManagementSelection.STATUS_GRADED, selectionStatus);
        });

        //Инициируем нажатие кнопки Опубликовать
        txService.runInNewTx(() -> bcpService.edit(selection, Maps.of(QualityManagementSelection.STATUS,
                QualityManagementSelection.STATUS_PUBLISHING)));

        //Запускаем процесс поиска выборок для публикации
        qualityManagementSelectionProcessor.publish();

        txService.runInNewTx(() -> {
            Ticket ticket1 = dbService.get(gid1);
            Ticket ticket2 = dbService.get(gid2);

            String selectionStatus = ((QualityManagementSelection) dbService.get(selection.getGid())).getStatus();
            //Проверяем, что статус тикетов и выборки изменился
            Assertions.assertEquals(QualityManagementTicket.STATUS_CLOSED, ticket1.getStatus());
            Assertions.assertEquals(QualityManagementTicket.STATUS_PUBLISHED, ticket2.getStatus());
            Assertions.assertEquals(QualityManagementTicket.RESOLUTION_PUBLISHED, ticket2.getResolution().getCode());
            Assertions.assertEquals(QualityManagementSelection.STATUS_PUBLISHED, selectionStatus);
        });
    }

    //https://testpalm2.yandex-team.ru/testcase/ocrm-1053 Ошибка при завершении выборки
    @Test
    public void publishingErrorQualityManagementSelection() {
        //Создаем выборку и 2 тикета, подходящие под эту выборку
        QualityManagementSelection selection = createQualityManagementSelection();

        //Стартуем процесс создания Тикетов КК
        qualityManagementSelectionProcessor.process();

        Query query = Query.of(QualityManagementTicket.FQN)
                .withFilters(Filters.eq(QualityManagementTicket.ENTITY, selection));
        List<Ticket> tickets = txService.doInNewReadOnlyTx(() -> dbService.list(query));
        //Проверяем, что создалось имеено 2 тикета КК
        Assertions.assertEquals(2, tickets.size());

        String gid1 = tickets.get(0).getGid();
        String gid2 = tickets.get(1).getGid();

        //Искусственно!!!! меняем статус у выборки, чтобы вызвать ошибку
        txService.runInNewTx(() -> bcpService.edit(selection, Maps.of(QualityManagementSelection.STATUS,
                QualityManagementSelection.STATUS_GRADED)));

        //Инициируем нажатие кнопки Опубликовать
        txService.runInNewTx(() -> bcpService.edit(selection, Maps.of(QualityManagementSelection.STATUS,
                QualityManagementSelection.STATUS_PUBLISHING)));

        //Запускаем процесс поиска выборок для публикации
        qualityManagementSelectionProcessor.publish();

        txService.runInNewTx(() -> {
            String selectionStatus = ((QualityManagementSelection) dbService.get(selection.getGid())).getStatus();
            //Проверяем, что статус тикетов и выборки изменился
            Assertions.assertEquals(QualityManagementSelection.STATUS_PUBLISHING_ERROR, selectionStatus);
        });

        txService.runInNewTx(() -> {
            //Первый тикет скипаем
            bcpService.edit(gid1, Maps.of(Ticket.STATUS, QualityManagementTicket.STATUS_PROCESSING));
            bcpService.edit(gid1, Maps.of(Ticket.STATUS, QualityManagementTicket.STATUS_SKIPPED));
            //Второй тикет оцениваем
            bcpService.edit(gid2, Maps.of(Ticket.STATUS, QualityManagementTicket.STATUS_PROCESSING));
            bcpService.edit(gid2, Maps.of(Ticket.STATUS, Ticket.STATUS_RESOLVED,
                    QualityManagementTicket.SURVEY_RESPONSE_DATA, surveyText));
        });

        //Инициируем нажатие кнопки Опубликовать
        txService.runInNewTx(() -> bcpService.edit(selection, Maps.of(QualityManagementSelection.STATUS,
                QualityManagementSelection.STATUS_PUBLISHING)));

        //Запускаем процесс поиска выборок для публикации
        qualityManagementSelectionProcessor.publish();

        txService.runInNewTx(() -> {
            Ticket ticket1 = dbService.get(gid1);
            Ticket ticket2 = dbService.get(gid2);

            String selectionStatus = ((QualityManagementSelection) dbService.get(selection.getGid())).getStatus();
            //Проверяем, что статус тикетов и выборки изменился
            Assertions.assertEquals(QualityManagementTicket.STATUS_CLOSED, ticket1.getStatus());
            Assertions.assertEquals(QualityManagementTicket.STATUS_PUBLISHED, ticket2.getStatus());
            Assertions.assertEquals(QualityManagementTicket.RESOLUTION_PUBLISHED, ticket2.getResolution().getCode());
            Assertions.assertEquals(QualityManagementSelection.STATUS_PUBLISHED, selectionStatus);
        });
    }

    private QualityManagementSelection createQualityManagementSelection() {
        return txService.doInNewTx(() -> {
            //Пользователь создается именно здесь для уникальности тикетов и выборки
            var employee = ticketTestUtils.createEmployee(ou1);
            var ticket = createTicketProperties(ou1, service1, team1, channel1, category1, employee);
            createTicket(ticket);
            createTicket(ticket);

            var properties = createSimpleSelectionProperties(Set.of(channel1), employee, Set.of(employee));
            return bcpService.create(QualityManagementSelection.FQN, properties);
        });
    }

    private Ticket createTicket(Map<String, Object> properties) {
        var ticket = ticketTestUtils.createTicket(TEST_FQN, properties);
        ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_PROCESSING);
        return ticketTestUtils.editTicketStatus(ticket, Ticket.STATUS_CLOSED);
    }

    @Nonnull
    private Map<String, Object> createSimpleSelectionProperties(Set<Channel> channels, Employee employee,
                                                                Set<Employee> responsibles) {
        return Maps.of(
                QualityManagementSelection.TITLE, Randoms.string(),
                QualityManagementSelection.CHANNELS, channels,
                QualityManagementSelection.OPERATOR, employee,
                QualityManagementSelection.RESPONSIBLES, responsibles,
                QualityManagementSelection.TICKET_MAX_NUMBER, 10
        );
    }

    @Nonnull
    private Map<String, Object> createTicketProperties(
            Ou ou,
            Service service,
            Team team,
            Channel channel,
            TicketCategory category,
            Employee employee
    ) {
        return Map.of(
                Ticket.STATUS, Ticket.STATUS_REGISTERED,
                Ticket.RESPONSIBLE_OU, ou,
                Ticket.SERVICE, service,
                Ticket.RESPONSIBLE_TEAM, team,
                Ticket.CHANNEL, channel,
                Ticket.CATEGORIES, category,
                Ticket.RESPONSIBLE_EMPLOYEE, employee
        );
    }
}
