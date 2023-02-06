package ru.yandex.market.tpl.carrier.core.domain.user;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.carrier.core.domain.company.SimpleCompanyDetails;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.citizenship.CitizenshipRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.data.PassportData;
import ru.yandex.market.tpl.carrier.core.domain.user.data.UserData;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@CoreTestV2
public class UserQueryServiceTest {

    private final TestUserHelper testUserHelper;
    private final UserFacade userFacade;
    private final UserQueryService userQueryService;
    private final CitizenshipRepository citizenshipRepository;
    private final DsRepository dsRepository;
    private final CompanyRepository companyRepository;

    private Company company1;
    private Company company2;
    private DeliveryService deliveryService1;

    @BeforeEach
    void init() {
        var localCompany1 = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        deliveryService1 = dsRepository.findByIdOrThrow(DeliveryService.DEFAULT_DS_ID);

        var localCompany2 = testUserHelper.findOrCreateCompany(
                TestUserHelper.CompanyGenerateParam.builder()
                        .campaignId(1235L)
                        .companyName("ANOTHER COMPANY NAME")
                        .login("anotherLogin@yandex.ru")
                        .deliveryServiceIds(Set.of())
                        .build()
        );

        company1 = companyRepository.findByIdOrThrow(localCompany1.getId());
        company2 = companyRepository.findByIdOrThrow(localCompany2.getId());
    }
    @Test
    void nextFakeIdSuccess() {
        Long uid = userQueryService.getNextFakeUid();
        assertThat(uid).isNotNull();
    }

    @Test
    void userDetailsFromDsm() {
        var citizenship = citizenshipRepository.findAll().stream().findFirst().orElseThrow();

        var passportData = PassportData.builder()
                .citizenship(citizenship.getId())
                .serialNumber("1234123456")
                .birthDate(LocalDate.of(2000, 1, 1))
                .issueDate(LocalDate.of(2014, 1, 1))
                .issuer("ТП УФМС Гондора в Минас Тирит")
                .build();

        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .passport(passportData)
                .source(UserSource.CARRIER)
                .build();

        User created = userFacade.createUser(userData, company1);

        var maybeUserDetails = userQueryService.findUserDetailsByIdAndCompaniesContains(created.getId(), company1);
        assertThat(maybeUserDetails).isNotEmpty();
        var userDetails = maybeUserDetails.get();

        assertThat(userDetails.getId()).isEqualTo(created.getId());
        assertThat(userDetails.getUid()).isEqualTo(created.getUid());
        assertThat(userDetails.getDsmId()).isNotNull();
        assertThat(userDetails.getCompanies())
                .containsExactlyInAnyOrder(new SimpleCompanyDetails(company1.getId(), company1.getName(), company1.getDsIds()));
        assertThat(userDetails.getStatus()).isEqualTo(created.getStatus());
        assertThat(userDetails.getName()).isEqualTo(created.getName());
        assertThat(userDetails.getLastName()).isEqualTo(created.getLastName());
        assertThat(userDetails.getFirstName()).isEqualTo(created.getFirstName());
        assertThat(userDetails.getPatronymic()).isEqualTo(created.getPatronymic());
        assertThat(userDetails.isDeleted()).isFalse();
        assertThat(userDetails.isBlackListed()).isFalse();
        assertThat(userDetails.getPassport()).isNotNull();
        var actualPassport = userDetails.getPassport();
        assertThat(actualPassport.getSerialNumber()).isEqualTo(passportData.getSerialNumber());
        assertThat(actualPassport.getCitizenship()).isEqualTo(passportData.getCitizenship());
        assertThat(actualPassport.getBirthDate()).isEqualTo(passportData.getBirthDate());
        assertThat(actualPassport.getIssueDate()).isEqualTo(passportData.getIssueDate());
        assertThat(actualPassport.getIssuer()).isEqualTo(passportData.getIssuer());
    }

    @Test
    void querySimpleUserWithCompanyListFromDsm() {
        assertThat(company1.getDsmId()).isNotNull();
        assertThat(company2.getDsmId()).isNotNull();

        var userData1 = validUserData("+79272403522", "1234123456");
        userFacade.createUser(userData1, company1);
        User created1 = userFacade.createUser(userData1, company2);

        var userData2 = validUserData("+79272403523", "1234123457");
        User created2 = userFacade.createUser(userData2, company2);

        var internalFilter = new InternalUserFilter()
                .setNamePart("Оло")
                .setPhonePart("+792724")
                .setCompanyId(company2.getId())
                .setBlacklisted(false)
                .setBirthDate(LocalDate.of(2000, 1, 1))
                .setDeliveryServiceId(deliveryService1.getId());

        var pageOfSimpleUserDetailsWithCompany = userQueryService.findAllSimpleUserDetailsWithCompany(
                internalFilter,
                PageRequest.of(0, 50)
        );

        var dtoList = pageOfSimpleUserDetailsWithCompany.getContent();

        assertThat(dtoList.size()).isEqualTo(2);
        dtoList.stream()
                .filter(dto -> dto.getId() == created1.getId())
                .peek(dto -> dto.getCompanies().sort(Comparator.comparingLong(SimpleCompanyDetails::getId)))
                .findFirst()
                .orElseThrow();

        assertThat(
                dtoList.stream()
                        .filter(dto -> dto.getId() == created1.getId())
                        .findFirst()
                        .orElseThrow()
        ).isEqualTo(simpleUser(userData1, created1, List.of(company1, company2), created1.getDsmId()));

        assertThat(
                dtoList.stream()
                        .filter(dto -> dto.getId() == created2.getId())
                        .findFirst()
                        .orElseThrow()
        ).isEqualTo(simpleUser(userData2, created2, List.of(company2), created2.getDsmId()));

        var dsmFilter = userQueryService.toDsmFilter(internalFilter);
        assertThat(dsmFilter.getDsmIds()).isNull();
        assertThat(dsmFilter.getEmployersDsmIds()).containsExactlyInAnyOrder(company1.getDsmId(), company2.getDsmId());
        assertThat(dsmFilter.getPhonePart()).isEqualTo("+792724");
        assertThat(dsmFilter.getNamePart()).isEqualTo("Оло");
        assertThat(dsmFilter.getBirthDay()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(dsmFilter.getBlacklisted()).isEqualTo(false);
    }

    private static UserData validUserData(String phone, String passport) {
        return UserData.builder()
                .phone(phone)
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .passport(
                        PassportData.builder()
                                .citizenship("RUS")
                                .serialNumber(passport)
                                .birthDate(LocalDate.of(2000, 1, 1))
                                .issueDate(LocalDate.of(2014, 1, 1))
                                .issuer("ТП УФМС Гондора в Минас Тирит")
                                .build()
                )
                .source(UserSource.CARRIER)
                .build();
    }

    private static SimpleUserDetailsWithCompanies simpleUser(UserData userData, User user, List<Company> companies, String dsmId) {
        var simple = new SimpleUserDetailsWithCompanies();
        simple.setId(user.getId());
        simple.setUid(user.getUid());
        simple.setName(user.getName());
        simple.setLastName(user.getLastName());
        simple.setFirstName(user.getFirstName());
        simple.setPatronymic(user.getPatronymic());
        simple.setPhone(userData.getPhone());
        simple.setBlackListed(user.isBlackListed());
        simple.setStatus(user.getStatus());
        simple.setSource(user.getSource());
        simple.setCompanies(
                companies.stream().map(company -> new SimpleCompanyDetails(company.getId(), company.getName(), company.getDsIds()))
                        .collect(Collectors.toList())
        );
        simple.setDsmId(dsmId);
        simple.setBirthday(Optional.ofNullable(userData.getPassport()).map(PassportData::getBirthDate).orElse(null));
        return simple;
    }

}
