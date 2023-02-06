package ru.yandex.market.tpl.partner.carrier.controller.partner.user.draft;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.CarrierUserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserQueryService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.util.UsersUtil;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.mock.DriverApiEmulator;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserCreateDraftDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserDraftDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserPassportDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserUpdateDraftDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler.COMPANY_HEADER;

/**
 * Тест-кейсы расписаны на странице <br>
 * https://wiki.yandex-team.ru/users/belkinmike/drafts/kejjsy-zavedenija-vodil-v-magistrali/
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierUserDraftControllerCreateUserTest extends BaseTplPartnerCarrierWebIntTest {
    private static final LocalDate DEFAULT_ISSUE_DATE = LocalDate.of(2022, 2, 22);
    private static final String PHONE_1 = "+70000000001";
    private static final String PHONE_2 = "+70000000002";
    private static final String SERIAL_NUMBER_1 = "0000000001";
    private static final String SERIAL_NUMBER_2 = "0000000002";
    private static final String TAXI_ID_1 = "TAXI_ID-1";
    private static final String TAXI_ID_2 = "TAXI_ID-2";


    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final UserRepository userRepository;
    private final UserQueryService userQueryService;
    private final CarrierUserCommandService carrierUserCommandService;
    private final DriverApiEmulator driverApiEmulator;

    @SneakyThrows
    private PartnerUserDraftDto createUser(PartnerUserCreateDraftDto dto, Company company) {
        var responseString = mockMvc.perform(post("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return tplObjectMapper.readValue(responseString, PartnerUserDraftDto.class);
    }

    @SneakyThrows
    private PartnerUserDraftDto updateUser(long userId, PartnerUserUpdateDraftDto dto, Company company) {
        var responseString = mockMvc.perform(put("/internal/partner/users/drafts/{id}", userId)
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return tplObjectMapper.readValue(responseString, PartnerUserDraftDto.class);
    }

    @SneakyThrows
    private PartnerUserDraftDto deleteUser(long userId, Company company) {
        var responseString = mockMvc.perform(delete("/internal/partner/users/drafts/{id}", userId)
                        .header(COMPANY_HEADER, company.getCampaignId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return tplObjectMapper.readValue(responseString, PartnerUserDraftDto.class);
    }

    private void emulatePostTaxiProfileTask(long userId, String taxiId) {
        assertThat(dbQueueTestUtil.getTasks(QueueType.SYNC_WITH_TAXI_ACCOUNT)).hasSize(1);

        var user = userRepository.findByIdOrThrow(userId);
        var yaProdId = UsersUtil.taxiIdToYaProId(taxiId);
        carrierUserCommandService.setYaProId(new UserCommand.SetYaProId(user.getId(), yaProdId));
        dbQueueTestUtil.clear(QueueType.SYNC_WITH_TAXI_ACCOUNT);
    }

    private PartnerUserPassportDto dto(String serialNumber) {
        LocalDate birthDate = LocalDate.of(2000, 5, 6);
        String citizenship = "RUS";
        String issuer = "МВД по г. Москве";
        return new PartnerUserPassportDto()
                .withSerialNumber(serialNumber)
                .withBirthDate(birthDate)
                .withCitizenship(citizenship)
                .withIssuer(issuer)
                .withIssueDate(DEFAULT_ISSUE_DATE);
    }

    private PartnerUserCreateDraftDto createDto(String phone, String serialNumber) {
        String firstName = "Иван";
        String lastName = "Иванов";
        String patronymic = "Иванович";
        return new PartnerUserCreateDraftDto()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withPatronymic(patronymic)
                .withPhone(phone)
                .withPassport(dto(serialNumber));
    }

    private PartnerUserUpdateDraftDto updateDto(String phone, String serialNumber) {
        String firstName = "Иван";
        String lastName = "Иванов";
        String patronymic = "Иванович";
        return new PartnerUserUpdateDraftDto()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withPatronymic(patronymic)
                .withPhone(phone)
                .withPassport(dto(serialNumber));
    }

    private void removePassportAndAssertPassportIsNull(long userId) {
        var userBefore = userRepository.findByIdOrThrow(userId);
        // in DSM
        driverApiEmulator.removePassportTest(userBefore.getDsmId());

        var driver = driverApiEmulator.getByIdTest(userBefore.getDsmId());
        assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isNull();
    }

    Company company1;
    Company company2;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.TAXIMETER_USER_SYNC_VERSION, 2);

        company1 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(4321L)
                .companyName("Компания 1")
                .login("login@yandex.ru")
                .build());
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .campaignId(1234L)
                .companyName("Компания 2")
                .login("anotherLogin@yandex.ru")
                .build());
    }

    @Test
    void createUser_alreadyExistsByPassportInSameCompany() {
        var createUserDto1 = createDto(PHONE_1, SERIAL_NUMBER_1);
        var userDto1 = createUser(createUserDto1, company1);

        var createUserDto2 = createDto(PHONE_2, SERIAL_NUMBER_1);
        var userDto2 = createUser(createUserDto2, company1);

        assertThat(userDto2.getId()).isEqualTo(userDto1.getId());
        assertThat(userDto2.getPhone()).isEqualTo(PHONE_2);

        transactionTemplate.execute(tc -> {
            var createdUser = userRepository.findByIdOrThrow(userDto2.getId());
            assertThat(createdUser.getCompanies()).containsExactly(company1);

            var driver = driverApiEmulator.getByIdTest(createdUser.getDsmId());
            assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);
            assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_2);
            return null;
        });
    }

    @Test
    void createUser_alreadyExistByPassportInAnotherCompany() {
        var createUserDto1 = createDto(PHONE_1, SERIAL_NUMBER_1);
        var userDto1 = createUser(createUserDto1, company1);

        var createUserDto2 = createDto(PHONE_2, SERIAL_NUMBER_1);
        var userDto2 = createUser(createUserDto2, company2);

        assertThat(userDto2.getId()).isEqualTo(userDto1.getId());
        assertThat(userDto2.getPhone()).isEqualTo(PHONE_2);

        transactionTemplate.execute(tc -> {
            var createdUser = userRepository.findByIdOrThrow(userDto2.getId());
            assertThat(createdUser.getCompanies()).containsExactly(company1, company2);

            var driver = driverApiEmulator.getByIdTest(createdUser.getDsmId());
            assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);
            assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_2);
            return null;
        });
    }

    /**
     * паспорта может не быть у водителя созданного давно, либо через быструю тачку
     */
    @Test
    void createUser_alreadyExistsByPhoneAndDoesNotHavePassportAndHasSameFIOInSameCompany() {
        var userDto1 = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);

        removePassportAndAssertPassportIsNull(userDto1.getId());

        var userDto2 = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);

        assertThat(userDto2.getId()).isEqualTo(userDto1.getId());
        assertThat(userDto2.getPhone()).isEqualTo(PHONE_1);
        assertThat(userDto2.getPassport()).isNotNull();
        assertThat(userDto2.getPassport().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);

        transactionTemplate.execute(tx -> {
            var createdUser = userRepository.findByIdOrThrow(userDto2.getId());
            assertThat(createdUser.getCompanies()).containsExactly(company1);

            var driver = driverApiEmulator.getByIdTest(createdUser.getDsmId());
            assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);
            assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_1);
            return null;
        });
    }

    @Test
    void createUser_alreadyExistsByPhoneAndDoesNotHavePassportAndHasSameFIOInAnotherCompany() {
        var userDto1 = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);

        removePassportAndAssertPassportIsNull(userDto1.getId());

        var userDto2 = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company2);

        assertThat(userDto2.getId()).isEqualTo(userDto1.getId());
        assertThat(userDto2.getPhone()).isEqualTo(PHONE_1);
        assertThat(userDto2.getPassport()).isNotNull();
        assertThat(userDto2.getPassport().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);

        transactionTemplate.execute(tx -> {
            var createdUser = userRepository.findByIdOrThrow(userDto2.getId());
            assertThat(createdUser.getCompanies()).contains(company1, company2);
            var driver = driverApiEmulator.getByIdTest(createdUser.getDsmId());
            assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);
            assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_1);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void createUser_alreadyExistsByPhoneAndDoesNotHavePassportAndHasDifferentFIO() {
        var userDto1 = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);

        removePassportAndAssertPassportIsNull(userDto1.getId());

        var createUserDto = createDto(PHONE_1, SERIAL_NUMBER_1);
        createUserDto.setFirstName("НеИван");

        mockMvc.perform(post("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company1.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(createUserDto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message",
                        is("Пользователь с таким телефоном уже существует!")));
    }

    @SneakyThrows
    @Test
    void createUser_alreadyExistsByPhoneAndHasPassport() {
        createUser(createDto(PHONE_1, SERIAL_NUMBER_2), company1);

        var createUserDto = createDto(PHONE_1, SERIAL_NUMBER_1);

        mockMvc.perform(post("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company1.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(createUserDto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message",
                        is("Пользователь с таким телефоном и паспортом уже существует!")));
    }

    /**
     * User, либо уволился из всех ТК и пришел снова,
     * либо пришел новый пользователь с телефоном другого пользователя.
     * Мы должны взять старый телефон, так как на него заведен профиль в такси
     */
    @Test
    void createUser_alreadyExistsByPhoneAndUserDeletedFromAllCompanies() {
        var oldUserDto = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);
        deleteUser(oldUserDto.getId(), company1);

        transactionTemplate.execute(tx -> {
            var oldUser = userRepository.findByIdOrThrow(oldUserDto.getId());
            assertThat(oldUser.isDeleted()).isTrue();

            var driver = driverApiEmulator.getByIdTest(oldUser.getDsmId());
            assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);
            assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_1);
            return null;
        });

        var newCreateUserDto = createDto(PHONE_1, SERIAL_NUMBER_2);
        newCreateUserDto.setLastName(newCreateUserDto.getLastName() + "_anotherName");
        var newUserDto = createUser(newCreateUserDto, company1);
        transactionTemplate.execute(tx -> {
            var createdUser = userRepository.findByIdOrThrow(newUserDto.getId());
            assertThat(createdUser.getCompanies()).containsExactly(company1);
            assertThat(createdUser.getId()).isNotEqualTo(oldUserDto.getId());

            var driver = driverApiEmulator.getByIdTest(createdUser.getDsmId());
            assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(SERIAL_NUMBER_2);
            assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_1);
            return null;
        });

    }

    @Test
    void createUser_doesNotExistByPassportAndByPhone() {
        var createUserDto = createDto(PHONE_1, SERIAL_NUMBER_1);
        var userDto = createUser(createUserDto, company1);

        assertThat(userDto.getFirstName()).isEqualTo(createUserDto.getFirstName());
        assertThat(userDto.getLastName()).isEqualTo(createUserDto.getLastName());
        assertThat(userDto.getPatronymic()).isEqualTo(createUserDto.getPatronymic());
        assertThat(userDto.getPassport()).isNotNull();
        assertThat(userDto.getPassport().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);
        assertThat(userDto.getPhone()).isEqualTo(PHONE_1);

        transactionTemplate.execute(tx -> {
            var user = userRepository.findByIdOrThrow(userDto.getId());
            assertThat(user.getFirstName()).isEqualTo(createUserDto.getFirstName());
            assertThat(user.getLastName()).isEqualTo(createUserDto.getLastName());
            assertThat(user.getPatronymic()).isEqualTo(createUserDto.getPatronymic());
            assertThat(user.getCompanies()).containsExactly(company1);

            var driver = driverApiEmulator.getByIdTest(user.getDsmId());
            assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(SERIAL_NUMBER_1);
            assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_1);
            return null;
        });
    }

    @Test
    void updateUser_phoneChangedAndNewPhoneAlreadyExistsAndDoesNotBelongToAnotherUser() {
        var created = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);
        emulatePostTaxiProfileTask(created.getId(), TAXI_ID_1);
        var deletedUserDto = deleteUser(created.getId(), company1);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SYNC_WITH_TAXI_ACCOUNT, 0);

        var deletedUser = userRepository.findByIdOrThrow(deletedUserDto.getId());
        assertThat(deletedUser.getTaxiId()).isEqualTo(TAXI_ID_1);

        var deletedDriver = driverApiEmulator.getByIdTest(deletedUser.getDsmId());
        assertThat(deletedDriver.getPersonalData().getPhone()).isEqualTo(PHONE_1);

        // создаем нового водителя phone2
        var newUserDto = createUser(createDto(PHONE_2, SERIAL_NUMBER_2), company1);
        emulatePostTaxiProfileTask(newUserDto.getId(), TAXI_ID_2);

        var maybeUserWithNewPhone = userQueryService.findByPhoneSingle(PHONE_2);
        assertThat(maybeUserWithNewPhone).isPresent();
        var userWithNewPhone = maybeUserWithNewPhone.get();
        assertThat(userWithNewPhone.getTaxiId()).isEqualTo(TAXI_ID_2);

        var driverWithNewPhone = driverApiEmulator.getByIdTest(userWithNewPhone.getDsmId());
        assertThat(driverWithNewPhone.getPersonalData().getPhone()).isEqualTo(PHONE_2);



        // изменяем новому водителю телефон на phone1
        var newUserDtoUpdated = updateUser(newUserDto.getId(), updateDto(PHONE_1, SERIAL_NUMBER_2), company1);

        var newUserUpdated = userRepository.findByIdOrThrow(newUserDtoUpdated.getId());
        assertThat(newUserUpdated.getTaxiId()).isEqualTo(TAXI_ID_2);

        var newDriverUpdated = driverApiEmulator.getByIdTest(newUserUpdated.getDsmId());
        assertThat(newDriverUpdated.getPersonalData().getPhone()).isEqualTo(PHONE_1);

        // идем обновить Taxi Profile
        dbQueueTestUtil.assertQueueHasSize(QueueType.SYNC_WITH_TAXI_ACCOUNT, 1);

        var deletedUserAfter = userRepository.findByIdOrThrow(deletedUserDto.getId());
        assertThat(deletedUserAfter.getTaxiId()).isEqualTo(TAXI_ID_1);

        var deletedDriverAfter = driverApiEmulator.getByIdTest(deletedUserAfter.getDsmId());
        assertThat(deletedDriverAfter.getPersonalData().getPhone()).isEqualTo(PHONE_1);
    }

    @SneakyThrows
    @Test
    void updateUser_phoneChangedAndNewPhoneAlreadyExistsAndNewPhoneUsedByAnotherUser() {
        var user1 = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);

        var user2 = createUser(createDto(PHONE_2, SERIAL_NUMBER_2), company1);
        dbQueueTestUtil.clear(QueueType.SYNC_WITH_TAXI_ACCOUNT);


        var updateDtoUser1 = updateDto(PHONE_2, SERIAL_NUMBER_1);

        mockMvc.perform(put("/internal/partner/users/drafts/{id}", user1.getId())
                        .header(COMPANY_HEADER, company1.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(updateDtoUser1))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message",
                        is("Пользователь с таким телефоном уже существует!")));

        dbQueueTestUtil.assertQueueHasSize(QueueType.SYNC_WITH_TAXI_ACCOUNT, 0);
    }

    @Test
    void updateUser_phoneChangedAndItDoesNotExist() {
        var newUserDto = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);
        emulatePostTaxiProfileTask(newUserDto.getId(), TAXI_ID_1);

        // given
        var user = userRepository.findByIdOrThrow(newUserDto.getId());
        assertThat(user.getTaxiId()).isEqualTo(TAXI_ID_1);

        var driver = driverApiEmulator.getByIdTest(user.getDsmId());
        assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_1);

        // when
        var newUserDtoUpdated = updateUser(newUserDto.getId(), updateDto(PHONE_2, SERIAL_NUMBER_2), company1);
        var newUserUpdated = userRepository.findByIdOrThrow(newUserDtoUpdated.getId());

        // then
        var newDriverUpdated = driverApiEmulator.getByIdTest(newUserUpdated.getDsmId());
        assertThat(newDriverUpdated.getPersonalData().getPhone()).isEqualTo(PHONE_2);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SYNC_WITH_TAXI_ACCOUNT, 1);
    }

    @Test
    void updateUser_phoneDidNotChange() {
        var newUserDto = createUser(createDto(PHONE_1, SERIAL_NUMBER_1), company1);
        emulatePostTaxiProfileTask(newUserDto.getId(), TAXI_ID_1);

        // given
        var user = userRepository.findByIdOrThrow(newUserDto.getId());
        assertThat(user.getTaxiId()).isEqualTo(TAXI_ID_1);

        var driver = driverApiEmulator.getByIdTest(user.getDsmId());
        assertThat(driver.getPersonalData().getPhone()).isEqualTo(PHONE_1);

        // when
        var newUserDtoUpdated = updateUser(newUserDto.getId(), updateDto(PHONE_1, SERIAL_NUMBER_1), company1);
        var newUserUpdated = userRepository.findByIdOrThrow(newUserDtoUpdated.getId());

        //then
        var newDriverUpdated = driverApiEmulator.getByIdTest(newUserUpdated.getDsmId());
        assertThat(newDriverUpdated.getPersonalData().getPhone()).isEqualTo(PHONE_1);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SYNC_WITH_TAXI_ACCOUNT, 0);
    }

}
