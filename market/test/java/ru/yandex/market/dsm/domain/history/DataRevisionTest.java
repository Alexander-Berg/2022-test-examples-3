package ru.yandex.market.dsm.domain.history;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.dsm.core.test.AbstractTest;
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationProperties;
import ru.yandex.market.dsm.domain.configuration.db.ConfigurationPropertiesDboRepository;
import ru.yandex.market.dsm.domain.courier.db.CourierDbo;
import ru.yandex.market.dsm.domain.courier.db.CourierDboRepository;
import ru.yandex.market.dsm.domain.courier.model.Courier;
import ru.yandex.market.dsm.domain.courier.model.CourierType;
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory;
import ru.yandex.market.dsm.domain.driver.db.DriverDbo;
import ru.yandex.market.dsm.domain.driver.db.DriverDboRepository;
import ru.yandex.market.dsm.domain.driver.model.Driver;
import ru.yandex.market.dsm.domain.driver.test.DriverTestFactory;
import ru.yandex.market.dsm.domain.employer.EmployersTestFactory;
import ru.yandex.market.dsm.domain.employer.db.Employer;
import ru.yandex.market.dsm.domain.employer.db.EmployerDbo;
import ru.yandex.market.dsm.domain.employer.db.EmployerDboRepository;
import ru.yandex.market.dsm.domain.employer.model.EmployerType;
import ru.yandex.market.dsm.domain.passportdata.db.PassportDataDbo;
import ru.yandex.market.dsm.domain.passportdata.db.PassportDataDboRepository;
import ru.yandex.market.dsm.domain.personaldata.db.PersonalDataDbo;
import ru.yandex.market.dsm.domain.personaldata.db.PersonalDataDboRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DataRevisionTest extends AbstractTest {
    private final EntityManagerFactory entityManagerFactory;
    private final PassportDataDboRepository passportDataDboRepository;
    private final PersonalDataDboRepository personalDataDboRepository;
    private final EmployerDboRepository employerDboRepository;
    private final ConfigurationPropertiesDboRepository configurationPropertiesDboRepository;
    private final CourierDboRepository courierDboRepository;
    private final DriverDboRepository driverDboRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EmployersTestFactory employersTestFactory;
    private final CourierTestFactory courierTestFactory;
    private final DriverTestFactory driverTestFactory;
    private final TransactionTemplate transactionTemplate;


    @Test
    void checkPassportDataAudited() {
        PassportDataDbo passportDataDbo = getPassportDataDbo("736548798");
        passportDataDboRepository.save(passportDataDbo);

        passportDataDbo.setPassportNumber("222222222");
        passportDataDboRepository.save(passportDataDbo);

        checkAudit(PassportDataDbo.class, passportDataDbo.getId(), 2);

        passportDataDbo = passportDataDboRepository.findById(passportDataDbo.getId()).orElseThrow();

        passportDataDboRepository.delete(passportDataDbo);
    }

    @Test
    void checkConfigurationPropertiesAudited() {
        ConfigurationProperties config = new ConfigurationProperties(UUID.randomUUID().toString(), "KEY2", "VALUE2");
        configurationPropertiesDboRepository.save(config);

        config.setValue("VALUE3");
        configurationPropertiesDboRepository.save(config);

        checkAudit(ConfigurationProperties.class, config.getId(), 2);

        config = configurationPropertiesDboRepository.findById(config.getId()).orElseThrow();
        configurationPropertiesDboRepository.delete(config);
    }

    @Test
    void checkPersonalDataAudited() {
        PersonalDataDbo personalDataDbo = getPersonalData("654378432", getPassportDataDbo("736548798"));

        personalDataDboRepository.save(personalDataDbo);

        personalDataDbo.setVaccinated(true);
        personalDataDboRepository.save(personalDataDbo);

        checkAudit(PersonalDataDbo.class, personalDataDbo.getId(), 2);

        personalDataDbo = personalDataDboRepository.findById(personalDataDbo.getId()).orElseThrow();

        personalDataDboRepository.delete(personalDataDbo);
    }

    @Test
    void checkEmployerAudited() {
        Employer employer = employersTestFactory.createAndSave("ASDHFSGSJM",
                "SSKJFHLSKFSSDF",
                "DSHKJFLSFLDSF",
                "EPVYVBTKTSRUO",
                EmployerType.LINEHAUL,
                true);

        EmployerDbo employerDbo = transactionTemplate.execute(status -> {
            EmployerDbo employerDbo2 = employerDboRepository.getOne(employer.getId());
            employerDbo2.setName("Новое имя");
            employerDboRepository.save(employerDbo2);
            return employerDbo2;
        });

        checkAudit(EmployerDbo.class, employerDbo.getId(), 2);

        transactionTemplate.execute(status -> {
            EmployerDbo employerDbo3 = employerDboRepository.findById(employerDbo.getId()).orElseThrow();
            employerDboRepository.delete(employerDbo3);
            return status;
        });
    }

    @Test
    void checkEmployerAuditedWithDeleteHistory() {
        Employer employer = employersTestFactory.createAndSave("ASDHFSGSJMA",
                "SSKJFHLSKFSSDFA",
                "DSHKJFLSFLDSFA",
                "EPVYVBTKTSRUO",
                EmployerType.LINEHAUL,
                true);

        transactionTemplate.execute(status -> {
            EmployerDbo employerDbo = employerDboRepository.getOne(employer.getId());
            String sqlDeleteEmployerHistory = "delete from employer_history where id = '" + employerDbo.getId() + "'";
            jdbcTemplate.execute(sqlDeleteEmployerHistory, PreparedStatement::execute);

            employerDbo = employerDboRepository.findById(employerDbo.getId()).orElseThrow();
            employerDbo.setName("Новое имя");
            employerDboRepository.save(employerDbo);

            checkAudit(EmployerDbo.class, employerDbo.getId(), 1);

            employerDbo = employerDboRepository.findById(employerDbo.getId()).orElseThrow();
            employerDboRepository.delete(employerDbo);
            return status;
        });
    }

    @Test
    void checkCourierAudited() {
        Employer employer = employersTestFactory.createAndSave("ASDHFSGSJMAA",
                "SSKJFHLSKFSSDFAA",
                "DSHKJFLSFLDSFAA",
                "EPVYVBTKTSRUO",
                EmployerType.LINEHAUL,
                true);

        EmployerDbo employerDbo = employerDboRepository.getOne(employer.getId());
        Courier courier = courierTestFactory.create(
                employerDbo.getId(), "4875305487", "sjdasfaf", CourierType.PARTNER, false
        );

        CourierDbo courierDbo = courierDboRepository.findById(courier.getId()).orElseThrow();
        courierDbo.setDeleted(true);
        courierDboRepository.save(courierDbo);

        checkAudit(CourierDbo.class, courierDbo.getId(), 2);
    }

    @Test
    void checkDriverAudited() {
        Employer employer = employersTestFactory.createAndSave("ASDHFSGSJMAAA",
                "SSKJFHLSKFSSDFAAA",
                "DSHKJFLSFLDSFAAA",
                "EPVYVBTKTSRUOO",
                EmployerType.LINEHAUL,
                true);

        EmployerDbo employerDbo = employerDboRepository.getOne(employer.getId());
        Driver driver = driverTestFactory.create(
                employerDbo.getId(),
                "101232",
                "test@test.ru",
                null,
                null,
                null,
                "lastName",
                "firstName",
                "patronymic"
        );

        DriverDbo driverDbo = driverDboRepository.findById(driver.getId()).orElseThrow();
        driverDbo.setUid("101555");
        driverDboRepository.save(driverDbo);

        checkAudit(DriverDbo.class, driverDbo.getId(), 2);
    }

    private <T> void checkAudit(Class<T> entityClass, String id, Integer size) {
        AuditReader auditReader = createAuditReader();
        List<Number> revisions = auditReader.getRevisions(
                entityClass, id);
        assertThat(auditReader.isEntityClassAudited(entityClass)).isTrue();
        assertThat(revisions).hasSize(size);
    }


    private PassportDataDbo getPassportDataDbo(String id) {
        return new PassportDataDbo(
                id,
                "Пупкин",
                "Василий",
                "Александрович",
                "111111111",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }


    private PersonalDataDbo getPersonalData(String id, PassportDataDbo passportDataDbo) {
        return new PersonalDataDbo(
                id,
                passportDataDbo,
                "Курьер Курьерович Курьеров",
                "pochta@mail.ru",
                null,
                null,
                false,
                null,
                null,
                null
        );
    }

    private AuditReader createAuditReader() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        return AuditReaderFactory.get(entityManager);
    }

}
