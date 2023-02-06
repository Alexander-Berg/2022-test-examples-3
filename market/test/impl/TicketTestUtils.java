package ru.yandex.market.jmf.module.ticket.test.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.wf.bcp.ValidateWfRequiredAttributesOperationHandler;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.DistributionAlgorithm;
import ru.yandex.market.jmf.module.ticket.Employee;
import ru.yandex.market.jmf.module.ticket.OuCallCenter;
import ru.yandex.market.jmf.module.ticket.Priority;
import ru.yandex.market.jmf.module.ticket.Resolution;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.TicketCategory;
import ru.yandex.market.jmf.module.ticket.test.TestChannels;
import ru.yandex.market.jmf.script.ScriptContextVariablesService;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.misc.lang.StringUtils;

@Component
public class TicketTestUtils {

    @Inject
    protected DbService dbService;
    @Inject
    BcpService bcpService;
    @Inject
    ScriptContextVariablesService scriptContextVariablesService;
    @Inject
    ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    TxService txService;
    @Inject
    private EntityStorageService entityStorageService;

    public TestContext createInTx() {
        return txService.doInTx(this::create);
    }

    public TestContext create() {
        TestContext ctx = new TestContext();

        ctx.serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();

        ctx.team0 = createTeam();
        ctx.team1 = createTeam();

        ctx.brand = createBrand();

        createResolution(Resolution.AUTO_REOPENED, (Brand) ctx.brand);

        ctx.service0 = createService(ctx.team0, ctx.serviceTime24x7, ctx.brand, Optional.empty());
        ctx.service1 = createService(ctx.team0, ctx.serviceTime24x7, ctx.brand, Optional.empty());

        ctx.parentOu = createOu();
        ctx.ou = createSubOu(ctx.parentOu);
        ctx.employee0 = createEmployee(ctx.ou, ctx.service0);
        ctx.employee1 = createEmployee(ctx.ou, ctx.service0);
        ctx.employee2 = createEmployee(ctx.ou, ctx.service0);
        ctx.employee3 = createEmployee(ctx.ou);
        ctx.employee4 = createEmployee(ctx.ou, ctx.service1);
        ctx.employee5 = createEmployee(ctx.ou, ctx.service1);

        bcpService.edit(ctx.employee0, ImmutableMap.of("teams", ctx.team0));
        bcpService.edit(ctx.employee1, ImmutableMap.of("teams", ctx.team0));
        bcpService.edit(ctx.employee2, ImmutableMap.of("teams", ctx.team1));
        bcpService.edit(ctx.employee3, ImmutableMap.of("teams", Arrays.asList(ctx.team0, ctx.team1)));
        bcpService.edit(ctx.employee4, ImmutableMap.of("teams", Arrays.asList(ctx.team0, ctx.team1)));
        bcpService.edit(ctx.employee5, ImmutableMap.of("teams", Arrays.asList(ctx.team0, ctx.team1)));

        ctx.supervisor = createEmployee(ctx.parentOu, ctx.service0);
        bcpService.edit(ctx.parentOu, ImmutableMap.of(OuCallCenter.SUPERVISORS, ImmutableSet.of(ctx.supervisor)));

        return ctx;
    }

    /**
     * Для указанной очереди устанавливает время обслуживания и поддержки в 24х7
     *
     * @param serviceCode
     */
    public void setServiceTime24x7(String serviceCode) {
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        setServiceTime(serviceCode, serviceTime24x7);
    }

    public void setServiceTime(String serviceCode, ServiceTime serviceTime) {
        Entity service = dbService.getByNaturalId(Service.FQN, Service.CODE, serviceCode);
        bcpService.edit(service, ru.yandex.market.jmf.utils.Maps.of(
                Service.SERVICE_TIME, serviceTime,
                Service.SUPPORT_TIME, serviceTime
        ));
    }

    public Team createTeamIfNotExists(String teamCode) {
        return (Team) txService.doInNewTx(() ->
                Optional.ofNullable(getTeam(teamCode))
                        .orElseGet(() -> createTeam(teamCode)));
    }

    private Entity getTeam(String teamCode) {
        return dbService.getByNaturalId(Team.FQN, Team.CODE, teamCode);
    }

    /**
     * В указанной очереди удаляет всех ответственных и тикеты ждущих распределения
     *
     * @param serviceCode
     */
    public void resetService(String serviceCode) {
        Service service = dbService.getByNaturalId(Service.FQN, Service.CODE, serviceCode);
        service.getResponsibleEmployees().forEach(employee -> {
            bcpService.edit(employee, Maps.of(Employee.SERVICES, List.of()));
        });

        Query q = Query.of(Ticket.FQN).withFilters(
                Filters.eq(Ticket.WAIT_DISTRIBUTION, true),
                Filters.eq(Ticket.SERVICE, service)
        );
        dbService.list(q).forEach(ticket -> {
                    bcpService.edit(ticket, Maps.of(Ticket.WAIT_DISTRIBUTION, false));
                }
        );
    }

    public Resolution createResolution(String code, Brand brand) {
        try {
            Resolution resolution = dbService.getByNaturalId(Resolution.FQN, Resolution.CODE, code);
            if (null != dbService.getByNaturalId(Resolution.FQN, Resolution.CODE, code)) {
                return resolution;
            }
            return bcpService.create(Resolution.FQN, Map.of(
                    Resolution.CODE, code,
                    Resolution.TITLE, code,
                    Resolution.BRAND, brand));
        } catch (ValidationException ex) {
            // игнорируем, если пытались создать повторно
            return null;
        }
    }

    public Brand createBrand() {
        return createBrand(Randoms.string());
    }

    public Brand createBrand(String brandCode) {
        Brand existsBrand = entityStorageService.getByNaturalId(Brand.FQN, brandCode);
        return existsBrand != null ? existsBrand : bcpService.create(Fqn.of("brand"), Map.of(
                "title", "Test",
                "code", brandCode,
                "emailSignature", "sign",
                "emailSender", "email@example.com",
                "icon", "url",
                "incomingPhone", "+7912" + StringUtils.leftPad(Randoms.stringNumber().substring(0, 7), 7, '1'),
                "outgoingPhone", "+79121112233"
        ));
    }

    public Employee createEmployee(Entity ou,
                                   Long uid,
                                   Entity... service) {
        ImmutableMap<String, Object> initialAttributes = ImmutableMap.of(
                Employee.TITLE, Randoms.string(),
                Employee.OU, ou,
                "services", service
        );
        Map<String, Object> attributes = new HashMap<>();
        attributes.putAll(initialAttributes);
        if (null != uid) {
            attributes.put(Employee.UID, uid);
        }
        return bcpService.create(Employee.FQN_DEFAULT, attributes);
    }

    public Employee createEmployee(Entity ou,
                                   Entity... service) {
        return createEmployee(ou, Randoms.longValue(), service);
    }

    public Ou createOu(Employee... supervisors) {
        Map<String, Object> attributes = Maps.of(
                Ou.TITLE, Randoms.string()
        );
        if (supervisors.length > 0) {
            attributes.put(OuCallCenter.SUPERVISORS, supervisors);
        }
        return bcpService.create(OuCallCenter.FQN_CALL_CENTER, attributes);
    }

    public Ou createSubOu(Ou parent, Employee... supervisors) {
        Map<String, Object> attributes = Maps.of(
                Ou.TITLE, Randoms.string(),
                Ou.PARENT, parent
        );
        if (supervisors.length > 0) {
            attributes.put(OuCallCenter.SUPERVISORS, supervisors);
        }
        return bcpService.create(OuCallCenter.FQN_CALL_CENTER, attributes);
    }

    public Team createTeam(Map<String, Object> properties) {
        Map<String, Object> attributes = Maps.of(
                Team.TITLE, Randoms.string(),
                Team.CODE, Randoms.string(),
                Team.DISTRIBUTION_ALGORITHM, DistributionAlgorithm.UNIFORM
        );
        attributes.putAll(properties);
        return bcpService.create(Team.FQN_DEFAULT, attributes);
    }

    public Team createTeam() {
        return createTeam(Randoms.string());
    }

    public Team createTeam(String code) {
        ImmutableMap<String, Object> attributes = ImmutableMap.of(
                Team.TITLE, Randoms.string(),
                Team.CODE, code,
                Team.DISTRIBUTION_ALGORITHM, DistributionAlgorithm.UNIFORM
        );
        return bcpService.create(Team.FQN_DEFAULT, attributes);
    }

    public Service createService(Entity team, Entity serviceTime, Entity brand, Optional<String> serviceCode) {
        return createService(team, serviceTime, brand, serviceCode, Service.FQN_DEFAULT, Map.of());
    }

    public Service createService(Entity team,
                                 Entity serviceTime,
                                 Entity brand,
                                 Optional<String> serviceCode,
                                 Map<String, Object> additionalAttributes) {
        return createService(team, serviceTime, brand, serviceCode, Service.FQN_DEFAULT, additionalAttributes);
    }

    public Service createService(Entity team,
                                 Entity serviceTime,
                                 Entity brand,
                                 Optional<String> serviceCode,
                                 Fqn serviceFqn,
                                 Map<String, Object> properties) {
        Map<String, Object> attributes = Maps.of(
                Service.TITLE, Randoms.string(),
                Service.CODE, serviceCode.orElse(Randoms.string()),
                Service.RESPONSIBLE_TEAM, team,
                Service.SERVICE_TIME, serviceTime,
                Service.SUPPORT_TIME, serviceTime,
                Service.RESOLUTION_TIME, "PT4H",
                Service.TAKING_TIME, "PT1H",
                Service.DEFAULT_PRIORITY, "50",
                Service.ALERT_TAKING_TIME, "PT1H",
                Service.BRAND, brand
        );
        attributes.putAll(properties);
        return bcpService.create(serviceFqn, attributes);
    }

    public Service createService() {
        return createService(Service.FQN_DEFAULT, Map.of());
    }

    public Service createService(Map<String, Object> properties) {
        return createService(Service.FQN_DEFAULT, properties);
    }

    public Service createService(Fqn serviceFqn, Map<String, Object> properties) {
        Map<String, Object> attributes = Maps.of(
                Service.TITLE, Randoms.string(),
                Service.CODE, Randoms.string(),
                Service.RESOLUTION_TIME, "PT4H",
                Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime(),
                Service.TAKING_TIME, "PT1H",
                Service.DEFAULT_PRIORITY, "50",
                Service.ALERT_TAKING_TIME, "PT1H",
                Service.BRAND, createBrand(),
                Service.RESPONSIBLE_TEAM, createTeam()
        );
        attributes.putAll(properties);
        return bcpService.create(serviceFqn, attributes);
    }

    public Priority createPriority() {
        return bcpService.create(Priority.FQN, ImmutableMap.of(
                Priority.CODE, Randoms.stringNumber(),
                Priority.TITLE, Randoms.string(),
                Priority.LEVEL, Randoms.longValue()
        ));
    }

    public <T extends Ticket> T createTicket(Fqn fqn, Map<String, Object> attributes) {
        final Team team = createTeam();
        final Service service = createService24x7(team);
        return createTicket(fqn,
                team,
                service,
                attributes);
    }

    public Service createService24x7(Team team, Brand brand) {
        return createService(Map.of(
                Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7(),
                Service.RESPONSIBLE_TEAM, team,
                Service.BRAND, brand
        ));
    }

    public Service createService24x7(Team team) {
        return createService24x7(Service.FQN_DEFAULT, team);
    }

    public Service createService24x7(Fqn fqn, Team team) {
        return createService(fqn, Map.of(
                Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7(),
                Service.RESPONSIBLE_TEAM, team
        ));
    }

    public Service createService24x7() {
        return createService24x7(createTeam());
    }

    public Service createService24x7(Fqn fqn) {
        return createService24x7(fqn, createTeam());
    }

    public <T extends Ticket> T createTicket(Fqn fqn,
                                             Service service,
                                             Map<String, Object> additionalAttributes) {
        return createTicket(fqn, createTeam(), service, additionalAttributes);
    }

    public <T extends Ticket> T createTicket(Fqn fqn,
                                             Team team,
                                             Service service,
                                             Map<String, Object> additionalAttributes) {
        Map<String, Object> initialAttributes = Map.of(
                Ticket.TITLE, Randoms.string(),
                Ticket.DESCRIPTION, Randoms.string(),
                Ticket.CHANNEL, TestChannels.CH1,
                Ticket.CLIENT_NAME, Randoms.string(),
                Ticket.CLIENT_EMAIL, Randoms.email(),
                Ticket.CLIENT_PHONE, Randoms.phoneNumber(),
                Ticket.SERVICE, service,
                Ticket.PRIORITY, createPriority(),
                Ticket.RESPONSIBLE_TEAM, team
        );

        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.putAll(initialAttributes);
        attributes.putAll(CrmCollections.nullToEmpty(additionalAttributes));
        return bcpService.create(fqn, attributes);
    }

    public TicketCategory createTicketCategory(Brand brand) {
        return createTicketCategory(Randoms.string(), brand);
    }

    public TicketCategory createTicketCategory(String code, Brand brand) {
        return scriptContextVariablesService.doWithVariables(Maps.of(
                ScriptContextVariablesService.ContextVariables.CARD_OBJECT,
                brand
        ), () -> bcpService.create(TicketCategory.FQN, Map.of(
                TicketCategory.CODE, code,
                TicketCategory.TITLE, Randoms.string()
        )));
    }

    public Ticket editTicketStatus(Ticket ticket, String status) {
        return bcpService.edit(ticket,
                Map.of(Ticket.STATUS, status),
                Map.of(
                        WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true,
                        ValidateWfRequiredAttributesOperationHandler.SKIP_WF_REQUIRED_ATTRIBUTES_VALIDATION, true
                )
        );
    }

    /**
     * Проверяет, что по заданному {@code fqn} есть только одно обращение и возвращает его
     */
    public <T extends Entity> T getSingleOpenedTicket(Fqn fqn) {
        List<T> tickets = getAllActiveTickets(fqn);
        Assertions.assertEquals(1, tickets.size());
        return tickets.get(0);
    }

    public <T extends Entity> List<T> getAllActiveTickets(Fqn fqn) {
        return dbService.list(Query.of(fqn).withFilters(
                Filters.ne(Ticket.STATUS, Ticket.STATUS_CLOSED)
        ));
    }

    public static class TestContext {

        public ServiceTime serviceTime24x7;

        public Service service0;
        public Service service1;

        public Ou ou;
        public Ou parentOu;

        public Employee employee0;
        public Employee employee1;
        public Employee employee2;
        public Employee employee3;
        public Employee employee4;
        public Employee employee5;
        public Employee supervisor;

        public Team team0;
        public Team team1;
        public Entity brand;

    }

}
