package ru.yandex.market.tpl.carrier.core.domain.user;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.citizenship.CitizenshipRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.NewDriverData;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.data.PassportData;
import ru.yandex.market.tpl.mock.DriverApiEmulator;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@CoreTestV2
public class UserCommandServiceTest {

    private final TestUserHelper testUserHelper;
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final CitizenshipRepository citizenshipRepository;
    private final TransactionTemplate transactionTemplate;
    private final DriverApiEmulator driverApiEmulator;

    private Company company;

    @BeforeEach
    void init() {
        company = testUserHelper.findOrCreateCompany(
                TestUserHelper.CompanyGenerateParam.builder()
                        .campaignId(1234L)
                        .companyName(Company.DEFAULT_COMPANY_NAME)
                        .login("anotherLogin@yandex.ru")
                        .build()
        );
    }


    @Test
    void createUserBothDsmAndCarrier() {
        transactionTemplate.execute(tc -> {
            var newUserData = validNewDriverData();

            User user = userCommandService.create(new UserCommand.CreateDriver(newUserData));
            assertThat(user).isNotNull();
            assertThat(user.getDsmId()).isNotNull();
            assertThat(user.getUid()).isEqualTo(newUserData.getUid());
            assertThat(user.getEmail()).isEqualTo(newUserData.getEmail());
            assertThat(user.getFirstName()).isEqualTo(newUserData.getFirstName());
            assertThat(user.getLastName()).isEqualTo(newUserData.getLastName());
            assertThat(user.getPatronymic()).isEqualTo(newUserData.getPatronymic());
            assertThat(user.getCompanies().stream().findFirst().map(Company::getId).orElseThrow()).isEqualTo(company.getId());


            var driver = driverApiEmulator.getByIdTest(user.getDsmId());

            assertThat(driver).isNotNull();
            assertThat(driver.getId()).isEqualTo(user.getDsmId());
            assertThat(driver.getUid()).isEqualTo(String.valueOf(newUserData.getUid()));
            assertThat(driver.getEmployerIds()).containsExactlyInAnyOrder(company.getDsmId());

            assertThat(driver.getPersonalData()).isNotNull();
            var storedPersonalData = driver.getPersonalData();
            assertThat(storedPersonalData.getPhone()).isEqualTo(newUserData.getPhone());
            assertThat(storedPersonalData.getEmail()).isEqualTo(newUserData.getEmail());

            assertThat(storedPersonalData.getPassportData()).isNotNull();
            var storedPassportData = storedPersonalData.getPassportData();

            assertThat(storedPassportData.getFirstName()).isEqualTo(newUserData.getFirstName());
            assertThat(storedPassportData.getLastName()).isEqualTo(newUserData.getLastName());
            assertThat(storedPassportData.getPatronymic()).isEqualTo(newUserData.getPatronymic());
            assertThat(storedPassportData.getCitizenship())
                    .isEqualTo(citizenshipRepository.findByIdOrThrow(newUserData.getPassport().getCitizenship()).getNationality());
            assertThat(storedPassportData.getSerialNumber()).isEqualTo(newUserData.getPassport().getSerialNumber());
            assertThat(storedPassportData.getBirthDate()).isEqualTo(newUserData.getPassport().getBirthDate());
            assertThat(storedPassportData.getIssueDate()).isEqualTo(newUserData.getPassport().getIssueDate());
            assertThat(storedPassportData.getIssuer()).isEqualTo(newUserData.getPassport().getIssuer());
            return null;
        });


    }

    private NewDriverData validNewDriverData() {
        var uid = userQueryService.getNextFakeUid();
        var email = uid + "@mail.com";
        return NewDriverData.builder()
                .role(UserRole.COURIER)
                .phone("+79272401234")
                .uid(uid)
                .email(email)
                .firstName("Василий")
                .lastName("Викторов")
                .patronymic("Олегович")
                .company(company)
                .source(UserSource.CARRIER)
                .passport(PassportData.builder()
                        .citizenship("RUS")
                        .serialNumber("1234123456")
                        .birthDate(LocalDate.of(2000, 1, 1))
                        .issueDate(LocalDate.of(2014, 1, 1))
                        .issuer("ТП УФМС Гондора в Минас Тирит")
                        .build())
                .build();
    }

}
