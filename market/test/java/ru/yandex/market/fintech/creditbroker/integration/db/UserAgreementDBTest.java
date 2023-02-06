package ru.yandex.market.fintech.creditbroker.integration.db;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fintech.creditbroker.exception.UserAgreementNotFoundException;
import ru.yandex.market.fintech.creditbroker.mapper.useragreement.UserAgreementMapper;
import ru.yandex.market.fintech.creditbroker.model.SopdScenario;
import ru.yandex.market.fintech.creditbroker.model.UserAgreement;
import ru.yandex.market.fintech.creditbroker.model.UserAgreementStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class UserAgreementDBTest
//        extends AbstractFunctionalTest
{

    @Autowired
    private UserAgreementMapper userAgreementMapper;
    @Autowired
    private Clock clock;

    @Test
    @Disabled
    public void shouldInsertAndFindUserAgreement() {
        UUID creditId = UUID.randomUUID();
        UserAgreement userAgreement = new UserAgreement()
                .setConfirmedAt(LocalDateTime.now(clock).minusMinutes(4L).truncatedTo(ChronoUnit.MILLIS))
                .setCreditId(creditId)
                .setPhoneId("43345-388")
                .setSmsSentAt(LocalDateTime.now(clock).minusMinutes(6L).truncatedTo(ChronoUnit.MILLIS))
                .setSopdScenario(SopdScenario.CREDIT_BROKER)
                .setStatus(UserAgreementStatus.CONFIRMED)
                .setUid(6699L)
                .setVersion(1L);
        userAgreementMapper.save(userAgreement);

        UserAgreement foundUserAgreement = userAgreementMapper.findByCreditId(creditId)
                .orElseThrow(() -> new UserAgreementNotFoundException(creditId));

        assertEquals(userAgreement, foundUserAgreement);
    }

    @Test
    @Disabled
    public void updateTest() {
        UUID creditId = UUID.randomUUID();
        UserAgreement userAgreement = new UserAgreement().setCreditId(creditId).setStatus(null);
        userAgreementMapper.save(userAgreement);

        userAgreement.setStatus(UserAgreementStatus.CONFIRMED);

        UserAgreement updatedUserAgreement = userAgreementMapper.save(userAgreement);
        assertEquals(userAgreement, updatedUserAgreement);
        assertNotSame(userAgreement, updatedUserAgreement);
    }

}
