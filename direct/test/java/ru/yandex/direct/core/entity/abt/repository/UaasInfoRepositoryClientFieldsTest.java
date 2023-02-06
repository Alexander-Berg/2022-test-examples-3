package ru.yandex.direct.core.entity.abt.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UaasInfoRepositoryClientFieldsTest {
    private static final TemporalUnitOffset OFFSET = new TemporalUnitWithinOffset(1, SECONDS);
    @Autowired
    private Steps steps;
    @Autowired
    private UaasInfoRepository uaasInfoRepository;

    @Test
    public void calculateClientFieldsTest() {
        var clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        var paramsMap = uaasInfoRepository.getUaasConditionsParams(List.of(clientInfo.getClientId().asLong()));
        assertThat(paramsMap).hasSize(1);
        assertThat(paramsMap).containsKeys(clientInfo.getClientId().asLong());
        var clientParams = paramsMap.get(clientInfo.getClientId().asLong());
        assertThat(clientInfo.getClient().getCreateDate())
                .isCloseTo(LocalDateTime.parse(clientParams.getClientCreateDate()), OFFSET);
        assertThat(RbacRole.toSource(clientInfo.getClient().getRole()).getLiteral()).isEqualTo(clientParams.getClientRole());
    }

    @Test
    public void calculateClientFields_ClientWithSeveralCampaignsTest() {
        var clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        var camp1 = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        steps.campaignSteps().createCampaign(camp1, clientInfo);
        var camp2 = activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());
        steps.campaignSteps().createCampaign(camp2, clientInfo);
        var paramsMap = uaasInfoRepository.getUaasConditionsParams(List.of(clientInfo.getClientId().asLong()));
        assertThat(paramsMap).hasSize(1);
        assertThat(paramsMap).containsKeys(clientInfo.getClientId().asLong());
        var clientParams = paramsMap.get(clientInfo.getClientId().asLong());
        assertThat(clientInfo.getClient().getCreateDate())
                .isCloseTo(LocalDateTime.parse(clientParams.getClientCreateDate()), OFFSET);
        assertThat(RbacRole.toSource(clientInfo.getClient().getRole()).getLiteral()).isEqualTo(clientParams.getClientRole());
    }
}
