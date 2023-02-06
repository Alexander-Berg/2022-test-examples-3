package ru.yandex.market.b2bcrm.module.ticket.test.utils;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.EntityValue;
import ru.yandex.market.jmf.module.ticket.Autoresponse;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.script.ScriptService;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class AutoresponseTestUtils {
    @Inject
    BcpService bcpService;

    @Inject
    ScriptService scriptService;

    @Inject
    TicketTestUtils ticketTestUtils;

    @Inject
    TxService txService;

    @Inject
    DbService dbService;

    @Inject
    ServiceTimeTestUtils serviceTimeTestUtils;

    public AutoresponseTestContext createContext() {
        AutoresponseTestContext ctx = new AutoresponseTestContext();

        ctx.serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();

        ctx.team0 = ticketTestUtils.createTeam();

        ctx.brand0 = ticketTestUtils.createBrand("Brand1");
        ctx.brand1 = ticketTestUtils.createBrand("Brand2");
        ctx.brand2 = ticketTestUtils.createBrand("Brand3");

        ctx.service0 = ticketTestUtils.createService(ctx.team0, ctx.serviceTime24x7, ctx.brand0, Optional.of(
                "testService0"), Map.of("emailSignatureOverride", "signOverride"));
        ctx.service1 = ticketTestUtils.createService(ctx.team0, ctx.serviceTime24x7, ctx.brand1, Optional.of(
                "testService1"));
        ctx.service2 = ticketTestUtils.createService(ctx.team0, ctx.serviceTime24x7, ctx.brand2, Optional.of(
                "testService2"));

        //Автоответы для комбинации Бренд + Очередь + Создание объекта
        ctx.responseBrandServiceCreate0 = createAutoresponse(ctx.brand0, ctx.service0, "create");
        ctx.responseBrandServiceCreate1 = createAutoresponse(ctx.brand1, ctx.service1, "create");

        //Автоответы для комбинации Бренд + Создание объекта (без очереди)
        ctx.responseBrandCreate0 = createAutoresponse(ctx.brand0, "create");
        ctx.responseBrandCreate1 = createAutoresponse(ctx.brand1, "create");

        //Автоответы для комбинации Бренд + Очередь + Публикация комментария
        ctx.responseBrandServiceCreateComment0 = createAutoresponse(ctx.brand0, ctx.service0, "createPublicComment");
        ctx.responseBrandServiceCreateComment1 = createAutoresponse(ctx.brand1, ctx.service1, "createPublicComment");

        //Автоответы для комбинации Бренд + Публикация комментария (без очереди)
        ctx.responseBrandCreateComment0 = createAutoresponse(ctx.brand0, "createPublicComment");
        ctx.responseBrandCreateComment1 = createAutoresponse(ctx.brand1, "createPublicComment");

        //Автоответы для массовки
        ctx.response0 = createAutoresponse(ctx.brand2, "create");
        ctx.response1 = createAutoresponse(ctx.brand2, "createPublicComment");

        return ctx;
    }

    public Autoresponse createAutoresponse(Brand brand, String event) {
        return bcpService.create(Autoresponse.FQN, Maps.of(
                Autoresponse.BRAND, brand,
                Autoresponse.SERVICE, null,
                Autoresponse.EVENT_TYPE, event,
                Autoresponse.EMAIL_BODY, Randoms.string()
        ));
    }

    public Autoresponse createAutoresponse(Brand brand, Service service, String event) {
        return bcpService.create(Autoresponse.FQN, Map.of(
                Autoresponse.BRAND, brand,
                Autoresponse.EVENT_TYPE, event,
                Autoresponse.SERVICE, service,
                Autoresponse.EMAIL_BODY, Randoms.string()
        ));
    }

    public Autoresponse getAutoresponseForTicket(Ticket ticket, String event) {
        Map<String, ?> map = Maps.of("ticket", ticket, "event", event);
        String script = "modules.autoresponse.getAutoresponseEntity(ticket, event)";

        EntityValue entity = scriptService.execute(script, map);

        return dbService.get(entity.getGid());
    }

    public void archiveAutoresponse(Autoresponse response) {
        bcpService.edit(response, Map.of("status", "archived"));
    }

    public static class AutoresponseTestContext {
        public ServiceTime serviceTime24x7;

        public Brand brand0;
        public Brand brand1;
        public Brand brand2;

        public Team team0;

        public Service service0;
        public Service service1;
        public Service service2;

        public Autoresponse responseBrandServiceCreate0;
        public Autoresponse responseBrandServiceCreate1;
        public Autoresponse responseBrandServiceCreateComment0;
        public Autoresponse responseBrandServiceCreateComment1;
        public Autoresponse responseBrandCreateComment0;
        public Autoresponse responseBrandCreateComment1;
        public Autoresponse responseBrandCreate0;
        public Autoresponse responseBrandCreate1;
        public Autoresponse response0;
        public Autoresponse response1;
    }
}
