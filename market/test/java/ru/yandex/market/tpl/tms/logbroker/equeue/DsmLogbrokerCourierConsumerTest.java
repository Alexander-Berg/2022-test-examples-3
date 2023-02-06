package ru.yandex.market.tpl.tms.logbroker.equeue;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.user.UserRegistrationStatus;
import ru.yandex.market.tpl.api.model.user.UserStatus;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierRegistrationStatusDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierStatus;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerCourierTypeDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerPersonalDataDto;
import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerMessage;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.tms.logbroker.consumer.dsm.courier.DsmLogbrokerCourierConsumer;
import ru.yandex.market.tpl.tms.logbroker.consumer.dsm.courier.DsmLogbrokerCourierService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@RequiredArgsConstructor
public class DsmLogbrokerCourierConsumerTest extends TplTmsAbstractTest {
    private final DsmLogbrokerCourierConsumer dsmLogbrokerCourierConsumer;
    private final TestUserHelper testUserHelper;
    private final DsmLogbrokerCourierService dsmLogbrokerCourierService;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    private Company company;
    private final LogbrokerCourierStatus logbrokerStatus = LogbrokerCourierStatus.FIRED;
    private final UserStatus status = UserStatus.FIRED;

    @BeforeEach
    void before() {
        company = testUserHelper.findOrCreateCompany("COMPANY4575785924", "COMPANY43892477811");
    }


    @Test
    void accept_UpsertAccept() {
        var message = getMessage();

        assertDoesNotThrow(() -> dsmLogbrokerCourierConsumer.accept(message));
    }

    @Test
    void createPartnerUser() {
        LogbrokerCourierDto logbrokerCourierDto = getLogbrokerCourierDto("45678765",
                LogbrokerCourierTypeDto.PARTNER,
                LogbrokerCourierRegistrationStatusDto.REGISTERED, "d3453453535t3");
        dsmLogbrokerCourierService.process(logbrokerCourierDto);
        User user = userRepository.findByUid(Long.parseLong(logbrokerCourierDto.getUid())).orElseThrow();
        assertUserEqualLogbrokerCourier(user.getId(), logbrokerCourierDto);
        assertThat(user.getRegistrationStatus()).isEqualTo(null);
    }

    @Test
    void createSelfEmployedUser() {
        LogbrokerCourierDto logbrokerCourierDto = getLogbrokerCourierDto("45678766",
                LogbrokerCourierTypeDto.SELF_EMPLOYED,
                LogbrokerCourierRegistrationStatusDto.SELF_EMPLOYED_REGISTRATION_PROCESSING, "eko3453535");
        dsmLogbrokerCourierService.process(logbrokerCourierDto);
        User user = userRepository.findByUid(Long.parseLong(logbrokerCourierDto.getUid())).orElseThrow();
        assertUserEqualLogbrokerCourier(user.getId(), logbrokerCourierDto);
        assertThat(user.getRegistrationStatus()).isEqualTo(
                UserRegistrationStatus.SELF_EMPLOYED_REGISTRATION_PROCESSING);
    }

    @Test
    void updateUser() {
        String dsmId = "345353535";
        User user = testUserHelper.findOrCreateUser(453278495);
        user.setCompany(company);
        user.setDsmExternalId(dsmId);
        userRepository.save(user);
        LogbrokerCourierDto logbrokerCourierDto = getLogbrokerCourierDto(String.valueOf(user.getUid()),
                LogbrokerCourierTypeDto.PARTNER,
                LogbrokerCourierRegistrationStatusDto.REGISTERED, dsmId);

        dsmLogbrokerCourierService.process(logbrokerCourierDto);
        user = userRepository.findByUid(Long.parseLong(logbrokerCourierDto.getUid())).orElseThrow();
        assertUserEqualLogbrokerCourier(user.getId(), logbrokerCourierDto);
        assertThat(user.getRegistrationStatus()).isEqualTo(null);
    }

    private void assertUserEqualLogbrokerCourier(Long id, LogbrokerCourierDto logbrokerCourierDto) {
        transactionTemplate.execute(status -> {
            User user = userRepository.getById(id);
            LogbrokerPersonalDataDto logbrokerPersonalDataDto = logbrokerCourierDto.getPersonalData();
            assertThat(logbrokerCourierDto.getId()).isEqualTo(user.getDsmExternalId());
            assertThat(String.valueOf(user.getUid())).isEqualTo(logbrokerCourierDto.getUid());
            assertThat(user.isDeleted()).isEqualTo(logbrokerCourierDto.getDeleted());
            assertThat(user.getEmail()).isEqualTo(logbrokerPersonalDataDto.getEmail());
            assertThat(user.getName()).isEqualTo(logbrokerPersonalDataDto.getName());
            assertThat(user.getHasVaccination()).isEqualTo(logbrokerPersonalDataDto.getVaccinated());
            assertThat(user.getYaProId()).isEqualTo(logbrokerCourierDto.getYaProId());
            assertThat(user.getYaProDriverId()).isEqualTo(logbrokerCourierDto.getYaProDriverId());
            assertThat(user.getYaProParkId()).isEqualTo(logbrokerCourierDto.getYaProParkId());
            assertThat(user.getStatus()).isEqualTo(this.status);
            assertThat(user.getCompany().getDsmExternalId()).isEqualTo(logbrokerCourierDto.getEmployerId());
            return status;
        });
    }

    private LogbrokerCourierDto getLogbrokerCourierDto(String uid,
                                                       LogbrokerCourierTypeDto logbrokerCourierTypeDto,
                                                       LogbrokerCourierRegistrationStatusDto
                                                               logbrokerCourierRegistrationStatusDto,
                                                       String dsmId) {
        LogbrokerCourierDto logbrokerCourierDto = new LogbrokerCourierDto();
        logbrokerCourierDto.setEmployerId(company.getDsmExternalId());
        logbrokerCourierDto.setId(dsmId);
        logbrokerCourierDto.setStatus(logbrokerStatus);
        logbrokerCourierDto.setUid(uid);
        logbrokerCourierDto.setDeleted(true);
        logbrokerCourierDto.setYaProDriverId("45353535");
        logbrokerCourierDto.setYaProParkId("3464646");
        LogbrokerPersonalDataDto logbrokerPersonalDataDto = new LogbrokerPersonalDataDto();
        logbrokerPersonalDataDto.setEmail("test857499@mail.ru");
        logbrokerPersonalDataDto.setName("TESTOV TEST");
        logbrokerPersonalDataDto.setFirstName("TEST");
        logbrokerPersonalDataDto.setLastName("TESTOV");
        logbrokerPersonalDataDto.setVaccinated(true);
        logbrokerCourierDto.setPersonalData(logbrokerPersonalDataDto);
        logbrokerCourierDto.setCourierType(logbrokerCourierTypeDto);
        logbrokerCourierDto.setCourierRegistrationStatus(logbrokerCourierRegistrationStatusDto);
        logbrokerCourierDto.setYaProId("472409520508");
        return logbrokerCourierDto;
    }


    @NotNull
    private LogbrokerMessage getMessage() {
        return new LogbrokerMessage(
                "",
                "{" +
                        "\"id\":\"1783687\"," +
                        "\"status\":null," +
                        "\"uid\":\"378375644\"," +
                        "\"employerId\":\"27837876\"," +
                        "\"routingId\":\"478378566\"," +
                        "\"workplaceNumber\":\"9745894341\"," +
                        "\"deleted\":true" +
                        "}"
        );
    }
}
