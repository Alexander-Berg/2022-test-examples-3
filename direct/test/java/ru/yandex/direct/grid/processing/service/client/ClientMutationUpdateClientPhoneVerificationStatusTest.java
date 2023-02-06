package ru.yandex.direct.grid.processing.service.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.antifraud.client.Verdict;
import ru.yandex.direct.antifraud.client.model.Action;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;

import static ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus.NEED_VERIFICATION;
import static ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus.VERIFICATION_IN_PROGRESS;
import static ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus.VERIFIED;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class ClientMutationUpdateClientPhoneVerificationStatusTest {

    @Rule
    public SpringMethodRule stringMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private ClientMutationService clientMutationService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientOptionsRepository clientOptionsRepository;

    private ClientId clientId;
    private int shard;

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "client becomes `verified`, when `ALLOW`, challenge is empty " +
                                "and previous phoneVerificationStatus=`veification_in_progress`",
                        VERIFICATION_IN_PROGRESS,
                        new Verdict(Action.ALLOW, null),
                        VERIFIED,
                },
                {
                        "client becomes `verified`, when `ALLOW`, challenge is empty " +
                                "and previous phoneVerificationStatus=`need_verification`",
                        NEED_VERIFICATION,
                        new Verdict(Action.ALLOW, null),
                        VERIFIED,
                },
                {
                        "client gets phoneVerificationStatus=`verification_in_progress` " +
                                "when `ALLOW`, challenge not empty " +
                                "and previous phoneVerificationStatus=`need_verification`",
                        NEED_VERIFICATION,
                        new Verdict(Action.ALLOW, "challenge"),
                        VERIFICATION_IN_PROGRESS,
                },
                {
                        "client phoneVerificationStatus not change " +
                                "when `ALLOW`, challenge not empty " +
                                "and previous phoneVerificationStatus=`verification_in_progress`",
                        VERIFICATION_IN_PROGRESS,
                        new Verdict(Action.ALLOW, "challenge"),
                        VERIFICATION_IN_PROGRESS,
                },
                {
                        "client phoneVerificationStatus not change, when `DENY` " +
                                "and previous phoneVerificationStatus=`verification_in_progress`",
                        VERIFICATION_IN_PROGRESS,
                        new Verdict(Action.DENY, null),
                        VERIFICATION_IN_PROGRESS,
                },
                {
                        "client phoneVerificationStatus not change, when `DENY` " +
                                "and previous phoneVerificationStatus=`need_verification`",
                        NEED_VERIFICATION,
                        new Verdict(Action.DENY, null),
                        NEED_VERIFICATION,
                },
        });
    }

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}")
    public void test(String testName, PhoneVerificationStatus curVerificationStatus, Verdict verdict,
                     PhoneVerificationStatus expectedVerificationStatus) {
        clientOptionsRepository.updatePhoneVerificationStatus(shard, clientId, curVerificationStatus);
        clientMutationService.updateClientPhoneVerificationStatus(verdict, curVerificationStatus, clientId);

        Client updatedClient = clientRepository.get(shard, List.of(clientId)).get(0);
        Assertions.assertThat(updatedClient.getPhoneVerificationStatus()).isEqualTo(expectedVerificationStatus);
    }
}
