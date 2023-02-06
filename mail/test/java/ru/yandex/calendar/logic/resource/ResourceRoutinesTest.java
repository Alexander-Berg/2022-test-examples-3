package ru.yandex.calendar.logic.resource;

import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.beans.generated.ResourceFields;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.blackbox.PassportAuthDomain;
import ru.yandex.inside.passport.blackbox.PassportDomain;
import ru.yandex.misc.db.q.SqlCondition;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.ip.InternetDomainName;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class ResourceRoutinesTest extends AbstractConfTest {
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceTestManager resourceTestManager;
    @Autowired
    private TestManager testManager;

    @Test
    public void fakeResourceEmail() {
        long id = 123;
        String domain = "test-domain.ru";
        Resource resource = new Resource();
        resource.setId(id);
        resource.setDomain(domain);
        Email email = ResourceRoutines.getResourceEmail(resource);
        Assert.A.equals("calendar-resource-" + id + "@" + domain, email.getEmail());
        Option<Long> parsedIdO = resourceRoutines.parseIdFromEmail(email);
        Assert.A.equals(id, parsedIdO.get().longValue());
    }

    @Test
    public void talkRoomEmail() {
        String exchangeName = "smolny";
        String ytDomain = PassportAuthDomain.YANDEX_TEAM_RU.getDomain().getDomain();
        Resource resource = new Resource();
        resource.setId(12345L);
        resource.setExchangeName(exchangeName);
        resource.setDomain(ytDomain);
        Email email = ResourceRoutines.getResourceEmail(resource);
        Assert.A.equals(exchangeName + "@" + ytDomain, email.getEmail());
        // TODO to verify parsing, we need to have a real resource in db
    }

    @Test
    public void getDomainResourcesWithLayersSortedByPos() {
        PassportDomain domain = PassportDomain.cons("fgfg.ru");

        Office office = resourceTestManager.saveSomeOffices(domain).first();

        Random2 r = new Random2(new Random(112345));
        for (int i = 0; i < 20; ++i) {
            Resource template = new Resource();
            template.setPos(r.nextInt(100) + 1);
            template.setDomain(domain.getDomain().getDomain());
            template.setOfficeId(office.getId());
            template.setName("res " + i);
            resourceRoutines.createResource(template);
        }

        ListF<Integer> posList = resourceDao
            .findDomainResourcesWithLayersAndOffices(domain, SqlCondition.trueCondition(), SqlCondition.trueCondition())
            .map(ResourceInfo.resourceF())
            .map(ResourceFields.POS.getF());
        Assert.A.equals(posList.sorted(), posList);
    }

    @Test
    public void selectResourceEmails() {
        InternetDomainName myDomain = new InternetDomainName("my.com");
        Office myOffice = testManager.createDefaultOffice(myDomain);

        Resource pigs = testManager.cleanAndCreateResource("pigs", "pigs", myDomain, myOffice);
        Resource smolny = testManager.cleanAndCreateResource("smolny", "smolny", myDomain, myOffice);
        Resource resource = testManager.cleanAndCreateResource("resource", "resource", myDomain, myOffice);

        testManager.cleanAndCreateResource("excluded", "excluded", myDomain, myOffice);

        Email pigsEmail = new Email(pigs.getExchangeName().get() + "@" + pigs.getDomain());
        Email smolnyEmail = new Email(smolny.getExchangeName().get() + "@" + pigs.getDomain());
        Email resourceEmail = new Email(resource.getExchangeName().get() + "@" + resource.getDomain());
        Email someParticipant = new Email("participant@my.com");

        ListF<Email> result = resourceRoutines.selectResourceEmails(
                Cf.<Email>list(pigsEmail, someParticipant, smolnyEmail, resourceEmail));
        Assert.A.sizeIs(3, result);
    }

    @Test
    public void resolveUpperCaseEmails () {
        InternetDomainName myDomain = new InternetDomainName("my.com");
        Office myOffice = testManager.createDefaultOffice(myDomain);

        Resource pigs = testManager.cleanAndCreateResource("pigs", "pigs", myDomain, myOffice);

        testManager.cleanAndCreateResource("excluded", "excluded", myDomain, myOffice);

        Email pigsEmail = new Email("pIgS" + "@" + pigs.getDomain());

        Assert.equals(resourceRoutines.selectResourceEmails(Cf.list(pigsEmail)), Cf.list(pigsEmail));
    }
} //~
