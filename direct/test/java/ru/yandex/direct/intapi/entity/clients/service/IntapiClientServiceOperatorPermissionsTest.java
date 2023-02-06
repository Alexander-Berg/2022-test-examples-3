package ru.yandex.direct.intapi.entity.clients.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiClientServiceOperatorPermissionsTest {

    @Autowired
    private IntapiClientService service;

    @Autowired
    private Steps steps;
    private ClientInfo agencyInfo;
    private List<Long> usersUnderAgency;
    private List<Long> usersNotUnderAgency;

    private SoftAssertions softAssertions;

    @PostConstruct
    public void setUpUsers() {
        agencyInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        usersUnderAgency = IntStream.range(0, 10)
                .mapToObj(ignored -> steps.clientSteps().createDefaultClientUnderAgency(agencyInfo).getUid())
                .collect(Collectors.toList());
        usersNotUnderAgency = IntStream.range(0, 10)
                .mapToObj(ignored -> steps.clientSteps().createDefaultClient().getUid())
                .collect(Collectors.toList());
    }


    @Before
    public void setUpSoftAssertions() {
        softAssertions = new SoftAssertions();
    }

    @Test
    public void existingAgency_emptyList() {
        assertThat(service.massGetOperatorPermissions(agencyInfo.getUid(), List.of())).isEmpty();
    }

    @Test
    public void existingAgency_noPermissions() {
        var result = service.massGetOperatorPermissions(agencyInfo.getUid(), usersNotUnderAgency);

        for (Long uid : usersNotUnderAgency) {
            softAssertions.assertThat(result).containsKey(uid);
            if (result.containsKey(uid)) {
                softAssertions.assertThat(result.get(uid).getPermissions()).isEmpty();
            }
        }
        softAssertions.assertThat(result).hasSameSizeAs(usersNotUnderAgency);
        softAssertions.assertAll();
    }

    @Test
    public void existingAgency_allPermissions() {
        var result = service.massGetOperatorPermissions(agencyInfo.getUid(), usersUnderAgency);
        for (Long uid : usersUnderAgency) {
            softAssertions.assertThat(result).containsKey(uid);
            if (result.containsKey(uid)) {
                softAssertions.assertThat(result.get(uid).getPermissions()).isNotEmpty();
            }
        }
        softAssertions.assertThat(result).hasSameSizeAs(usersUnderAgency);
        softAssertions.assertAll();
    }


    @Test
    public void existingAgency_stress() {
        List<Long> allClients = new ArrayList<>();
        allClients.addAll(usersUnderAgency);
        allClients.addAll(usersNotUnderAgency);
        var result = service.massGetOperatorPermissions(agencyInfo.getUid(), allClients);

        for (Long uid : usersNotUnderAgency) {
            softAssertions.assertThat(result).containsKey(uid);
            if (result.containsKey(uid)) {
                softAssertions.assertThat(result.get(uid).getPermissions()).isEmpty();
            }
        }

        for (Long uid : usersUnderAgency) {
            softAssertions.assertThat(result).containsKey(uid);
            if (result.containsKey(uid)) {
                softAssertions.assertThat(result.get(uid).getPermissions()).isNotEmpty();
            }
        }

        softAssertions.assertThat(result).hasSameSizeAs(allClients);
        softAssertions.assertAll();
    }
}
