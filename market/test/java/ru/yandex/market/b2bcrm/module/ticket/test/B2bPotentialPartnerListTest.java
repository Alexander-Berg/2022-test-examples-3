package ru.yandex.market.b2bcrm.module.ticket.test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jdk.jfr.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.b2bcrm.module.account.Account;
import ru.yandex.market.b2bcrm.module.account.B2bAccountContactRelation;
import ru.yandex.market.b2bcrm.module.account.B2bContact;
import ru.yandex.market.b2bcrm.module.ticket.test.config.B2bTicketTests;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.timings.ServiceTime;

@B2bTicketTests
public class B2bPotentialPartnerListTest extends AbstractB2bMailProcessingCreationTest {

    protected static final String MAIL_CONNECTION = "b2b";

    @BeforeEach
    public void prepareData() {
        mailTestUtils.createMailConnection(MAIL_CONNECTION);
        createAccounts();
        createContacts();
        createB2bTicketRoutingRules();

        Entity st = dbService.getByNaturalId(ServiceTime.FQN, CatalogItem.CODE, "b2b_9_21");
        serviceTimeTestUtils.createPeriod(st, "monday", "09:00", "21:00");

        Team team0 = ticketTestUtils.createTeam("firstLineMail");
        ServiceTime serviceTime24x7 = serviceTimeTestUtils.createServiceTime24x7();
        Brand brand = ticketTestUtils.createBrand();

        ticketTestUtils.createService(team0, serviceTime24x7, brand, Optional.of("marketApiAffiliate"));

        configurationService.setValue("defaultRoutingMailServiceForB2bTicket", defaultService);
    }

    /**
     * Проверяем, что в списке потенциальных партнеров будут нужные значения
     * <p>
     * Пишем письмо с email-а, который указан у двух контактов. Эти контакты привязаны к разным партнерам.
     * В конечном списке должны получить этих двух партнеров.
     */
    @Test
    @Description("Проверяем наличие партнеров в списке потенциальных партнеров на карточке в UI")
    public void testPotentialPartnerList() {
        //Берем email, указанный у подходящих контактов
        String email = contact5.getEmails().get(0);

        //Ищем все контакты с указанным email
        List<String> contactGids = dbService.list(Query.of(Fqn.of("b2bContact")).withFilters(
                Filters.containsAll(B2bContact.EMAILS, Collections.singletonList(email))
        )).stream().map(HasGid::getGid).collect(Collectors.toList());

        //Ищем партнеров к которым они привязаны
        List<Account> accounts = dbService.list(Query.of(Fqn.of("b2bAccountContactRelation")).withFilters(
                Filters.in(B2bAccountContactRelation.CONTACT, contactGids),
                Filters.ne(B2bAccountContactRelation.ACCOUNT, null)
        )).stream().map((Entity t) -> ((B2bAccountContactRelation) t).getAccount()).collect(Collectors.toList());

        //Проверяем, что мы нашли 2ух партнеров и это указанные магазины
        Assertions.assertEquals(2, accounts.size(), "В списке 2 партнера");
        Assertions.assertTrue(accounts.contains(shop3), "Список содержит магазин " + shop3.getGid());
        Assertions.assertTrue(accounts.contains(shop2), "Список содержит магазин " + shop2.getGid());
    }
}
