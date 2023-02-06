package ru.yandex.direct.core.entity.calltrackingphone.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.calltracking.model.CalltrackingPhone;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.junit.Assert.assertEquals;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalltrackingPhoneRepositoryTest {
    private final static String PHONE_1 = ClientPhoneTestUtils.getUniqPhone();
    private final static String PHONE_2 = ClientPhoneTestUtils.getUniqPhone();

    @Autowired
    private CalltrackingPhoneRepository calltrackingPhoneRepository;

    @Autowired
    private Steps steps;

    @Test
    public void addOrUpdate() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        steps.calltrackingPhoneSteps().add(PHONE_1, now.minusDays(10));

        CalltrackingPhone calltrackingPhone1 = new CalltrackingPhone()
                .withPhone(PHONE_1)
                .withLastUpdate(now);
        CalltrackingPhone calltrackingPhone2 = new CalltrackingPhone()
                .withPhone(PHONE_2)
                .withLastUpdate(now);

        List<CalltrackingPhone> calltrackingPhoneList = List.of(calltrackingPhone1, calltrackingPhone2);

        calltrackingPhoneRepository.addOrUpdate(calltrackingPhoneList);

        List<CalltrackingPhone> got = steps.calltrackingPhoneSteps().getAllPhones();
        assertEquals(calltrackingPhoneList, got);
    }

    @Test
    public void deleteOlderThan() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime minus5Days = now.minusDays(5);
        LocalDateTime minus7Days = now.minusDays(7);
        LocalDateTime minus10Days = now.minusDays(10);

        steps.calltrackingPhoneSteps().add(PHONE_1, minus10Days);
        steps.calltrackingPhoneSteps().add(PHONE_2, minus5Days);

        calltrackingPhoneRepository.deleteOlderThan(minus7Days);

        List<CalltrackingPhone> gotList = steps.calltrackingPhoneSteps().getAllPhones();
        assertEquals(1, gotList.size());

        CalltrackingPhone expected = new CalltrackingPhone()
                .withPhone(PHONE_2)
                .withLastUpdate(minus5Days);
        CalltrackingPhone got = gotList.get(0);
        assertEquals(expected, got);
    }
}
