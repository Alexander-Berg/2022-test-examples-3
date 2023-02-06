package ru.yandex.market.crm.operatorwindow.toloka;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.crm.operatorwindow.AbstractModuleOwTest;
import ru.yandex.market.crm.operatorwindow.integration.Brands;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketVersion;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.module.toloka.AssessmentConfiguration;
import ru.yandex.market.jmf.module.toloka.AssessmentRule;
import ru.yandex.market.jmf.module.toloka.TolokaClient;
import ru.yandex.market.jmf.module.toloka.TolokaClients;
import ru.yandex.market.jmf.module.toloka.model.TolokaServer;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.tx.TxService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public abstract class TolokaAbstractTest extends AbstractModuleOwTest {

    @Inject
    protected DbService dbService;
    @Inject
    protected ConfigurationService configurationService;
    @Inject
    protected BcpService bcpService;
    @Inject
    protected TicketTestUtils ticketTestUtils;
    @Inject
    protected EntityStorageService entityStorage;
    @Inject
    protected TriggerServiceImpl triggerService;
    @Inject
    protected TolokaClient tolokaClient;
    @Inject
    protected TolokaClients tolokaClients;
    @Inject
    protected TxService txService;

    @BeforeEach
    public void cleanUpTickets() {
        // Зачищаем обращения, на случай если кто-то не прибрался
        txService.runInNewTx(() -> Stream.of(Ticket.FQN, TicketVersion.FQN)
                .map(Query::of)
                .map(dbService::list)
                .flatMap(Collection::stream)
                .forEach(dbService::delete));
        when(tolokaClients.get(any())).thenReturn(tolokaClient);
    }

    @AfterEach
    public void resetTolokaClient() {
        reset(tolokaClient);
    }

    protected void setTolokaExchangeEnabled(boolean isEnabled) {
        configurationService.setValue(AssessmentConfiguration.TOLOKA_EXCHANGE_ENABLED.key(), isEnabled);
    }

    public AssessmentRule[] generateAssessmentRules(int limit) {
        return Stream.generate(this::createAssessmentRule).limit(limit).toArray(AssessmentRule[]::new);
    }

    public AssessmentRule createAssessmentRule() {
        return bcpService.create(AssessmentRule.FQN, Map.of(
                AssessmentRule.SERVER_ID, TolokaServer.TOLOKA,
                AssessmentRule.PROJECT_ID, Randoms.string(),
                AssessmentRule.POOL_ID, Randoms.string(),
                AssessmentRule.PENDING_TIME, Duration.ofMinutes(60),
                AssessmentRule.ASSESSMENT_TIME, Duration.ofMinutes(60),
                AssessmentRule.TITLE, Randoms.string(),
                AssessmentRule.INPUT_FIELD_NAME, Randoms.string()));
    }

    public Service[] generateServices(int limit) {
        return Stream.generate(this::createService).limit(limit).toArray(Service[]::new);
    }

    public Service createService() {
        return ticketTestUtils.createService24x7(
                ticketTestUtils.createTeam(),
                entityStorage.getByNaturalId(Brand.FQN, Brands.BERU_SMM));
    }

}
