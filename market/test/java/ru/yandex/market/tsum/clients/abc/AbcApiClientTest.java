package ru.yandex.market.tsum.clients.abc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.abc.models.AbcPerson;
import ru.yandex.market.tsum.clients.abc.models.AbcService;
import ru.yandex.market.tsum.clients.abc.models.AbcServiceMember;
import ru.yandex.market.tsum.clients.abc.models.AbcShift;

@Ignore
public class AbcApiClientTest {
    private final AbcApiClient client = new AbcApiClient("**", null);

    @Test
    public void getAbcServices() {
        Stream<AbcService> serviceStream = client.getAbcServices(ImmutableMap.of(
            "parent__with_descendants", String.valueOf(905)
        ));
        List<AbcService> services = serviceStream.collect(Collectors.toList());
        System.out.println(services);
    }

    @Test
    public void getAbcServiceById() {
        System.out.println(client.getAbcServiceById("2"));
    }

    @Test
    public void getAbcServiceBySlugExact() {
        List<AbcService> services = client.getAbcServicesBySlugExact("marketinfra").collect(Collectors.toList());
        System.out.println(services);
    }

    @Test
    public void getAbcServiceBySlugContains() {
        List<AbcService> services = client.getAbcServicesBySlugContains("infra").collect(Collectors.toList());
        System.out.println(services);
    }

    @Test
    public void getAbcServicesByNameExact() {
        List<AbcService> services =
            client.getAbcServicesByNameExact("Инфраструктурные сервисы Маркета").collect(Collectors.toList());
        System.out.println(services);
    }

    @Test
    public void getAbcServiceByNameContains() {
        List<AbcService> services =
            client.getAbcServicesByNameContains("Инфраструктурные сервисы").collect(Collectors.toList());
        System.out.println(services);
    }

    @Test
    public void getAbcServiceMembers() {
        List<AbcServiceMember> members = client.getAbcServiceMembers("marketinfra").collect(Collectors.toList());
        System.out.println(members);
    }

    @Test
    public void getAbcServiceByMember() {
        List<AbcService> services = client.getAbcServicesByMember("sid-hugo").collect(Collectors.toList());
        System.out.println(services);
    }

    @Test
    public void getAbcPersonsWithRobots() {
        List<AbcPerson> persons = client.getAbcServicePersons("marketinfra").collect(Collectors.toList());
        System.out.println(persons);
    }

    @Test
    public void getAbcPersonsWithoutRobots() {
        List<AbcPerson> persons = client.getAbcServicePersons("marketinfra", true).collect(Collectors.toList());
        System.out.println(persons);
    }

    @Test
    public void getAbcOnDuty() {
        List<AbcShift> shifts = client.getAbcOnDuty("incidentmanagement", "inc_duty");
        System.out.println(shifts);
    }
}
