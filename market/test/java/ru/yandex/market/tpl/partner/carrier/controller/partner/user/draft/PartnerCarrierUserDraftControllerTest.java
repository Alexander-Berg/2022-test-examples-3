package ru.yandex.market.tpl.partner.carrier.controller.partner.user.draft;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.db.QueryCountAssertions;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserBlackListReason;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserFacade;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.domain.user.citizenship.CitizenshipRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.data.PassportData;
import ru.yandex.market.tpl.carrier.core.domain.user.data.UserData;
import ru.yandex.market.tpl.mock.DriverApiEmulator;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.FrontColor;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserCreateDraftDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserDraftDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserPassportDto;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserTag;
import ru.yandex.market.tpl.partner.carrier.model.user.partner.PartnerUserUpdateDraftDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.company.Company.DEFAULT_COMPANY_NAME;
import static ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler.COMPANY_HEADER;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PartnerCarrierUserDraftControllerTest extends BaseTplPartnerCarrierWebIntTest {
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;

    private final UserFacade userFacade;
    private final UserCommandService userCommandService;
    private final UserRepository userRepository;
    private final CitizenshipRepository citizenshipRepository;
    private final DriverApiEmulator driverApiEmulator;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(DEFAULT_COMPANY_NAME);
    }

    @Test
    @SneakyThrows
    void shouldGetDrafts() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        userFacade.createUser(userData, company);

        QueryCountAssertions.assertQueryCountTotalEqual(
                5,
                () -> mockMvc.perform(get("/internal/partner/users/drafts")
                                .param("sort", "name,asc")
                                .header(COMPANY_HEADER, company.getCampaignId())
                        ).andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
        );
    }

    @Test
    @SneakyThrows
    void shouldNotGetDeletedDraft() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User created = userFacade.createUser(userData, company);

        userCommandService.deleteFromCompany(new UserCommand.DeleteFromCompany(created.getId(), company.getId()));

        mockMvc.perform(get("/internal/partner/users/drafts")
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @SneakyThrows
    void shouldGetDraft() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);

        mockMvc.perform(get("/internal/partner/users/drafts/{id}", draft.getId())
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.id").value(draft.getId()));
    }

    @Test
    @SneakyThrows
    void shouldGetDraftWithBlackListedUser() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);

        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(draft.getId(), UserBlackListReason.NOT_USING_APP));

        mockMvc.perform(get("/internal/partner/users/drafts/{id}", draft.getId())
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.id").value(draft.getId()))
                .andExpect(jsonPath("$.blackListed").value(true));
    }

    @Test
    @SneakyThrows
    void shouldGetDraftsWithBlackListedUser() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);

        var userData2 = UserData.builder()
                .phone("+79272403529")
                .firstName("Ололош2")
                .lastName("Ололоев2")
                .patronymic("Ололоевич2")
                .source(UserSource.CARRIER)
                .build();

        User draft2 = userFacade.createUser(userData2, company);

        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(draft.getId(),
               null));

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[*].blackListed").value(Matchers.containsInAnyOrder(true, false)));
    }

    @Test
    @SneakyThrows
    void shouldGetDraftsWithBlackListedUserAndShowTag() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .passport(
                        PassportData.builder()
                                .citizenship("RUS")
                                .serialNumber("1234123456")
                                .birthDate(LocalDate.of(2000, 1, 1))
                                .issueDate(LocalDate.of(2014, 1, 1))
                                .issuer("ТП УФМС Гондора в Минас Тирит")
                                .build()
                )
                .build();

        User draft = userFacade.createUser(userData, company);

        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(draft.getId(), UserBlackListReason.OTHER));

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].blackListed").value(true))
                .andExpect(jsonPath("$.content[0].tags[0].tag").value(PartnerUserTag.BLACKLISTED.name()))
                .andExpect(jsonPath("$.content[0].tags[0].tooltip").value(UserBlackListReason.OTHER.name()))
                .andExpect(jsonPath("$.content[0].tags[0].color")
                        .value(FrontColor.DANGER.getFrontRepresentation()));
    }

    @Test
    @SneakyThrows
    void shouldGetDraftsWithBlackListedUserWithReason() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);

        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(draft.getId(),
                UserBlackListReason.NOT_USING_APP));

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].blackListReason")
                        .value(UserBlackListReason.NOT_USING_APP.name()));
    }

    @Test
    @SneakyThrows
    void shouldGetDraftsWithBlackOnlyListedUser() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);

        var userData2 = UserData.builder()
                .phone("+79272403523")
                .firstName("Ололоша")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft2 = userFacade.createUser(userData2, company);

        userCommandService.markBlackListed(new UserCommand.MarkBlackListed(draft.getId(),
                UserBlackListReason.NOT_USING_APP));

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("blackListed", "true")
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].blackListReason")
                        .value(UserBlackListReason.NOT_USING_APP.name()));
    }


    @Test
    @SneakyThrows
    void shouldCreateDraft() {
        PartnerUserCreateDraftDto partnerUserDraftDto = defaultCreateUserDto();

        var response = mockMvc.perform(
                post("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerUserDraftDto created = tplObjectMapper.readValue(response, PartnerUserDraftDto.class);
        transactionTemplate.execute(tc -> {
            User user = userRepository.findByIdOrThrow(created.getId());
            Assertions.assertThat(user).isNotNull();
            Assertions.assertThat(user.getCompanies()).isNotEmpty();

            var driver = driverApiEmulator.getByIdTest(user.getDsmId());
            assertThat(driver.getPersonalData().getPhone()).isNotBlank();
            return null;
        });
    }


    @Test
    @SneakyThrows
    void shouldCreateDraftWithPassport() {
        var serialAsIs = "1234 123456";
        var serialNoSpaces = serialAsIs.replaceAll("\\s+", "");
        var partnerUserDraftDto = userDraftWithPassport(serialAsIs, "RUS");

        var response = mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerUserDraftDto created = tplObjectMapper.readValue(response, PartnerUserDraftDto.class);
        transactionTemplate.execute(tc -> {
            User user = userRepository.findByIdOrThrow(created.getId());
            Assertions.assertThat(user).isNotNull();
            Assertions.assertThat(user.getCompanies()).isNotEmpty();

            var passportDto = partnerUserDraftDto.getPassport();
            var expectedCitizenship = citizenshipRepository.findByIdOrThrow(passportDto.getCitizenship());
            var driver = driverApiEmulator.getByIdTest(user.getDsmId());
            assertThat(driver.getPersonalData().getPassportData()).isNotNull();

            var storedPassportData = driver.getPersonalData().getPassportData();
            assertThat(storedPassportData.getSerialNumber()).isEqualTo(serialNoSpaces);
            assertThat(storedPassportData.getCitizenship()).isEqualTo(expectedCitizenship.getNationality());
            assertThat(storedPassportData.getBirthDate()).isEqualTo(passportDto.getBirthDate());
            assertThat(storedPassportData.getIssueDate()).isEqualTo(passportDto.getIssueDate());
            assertThat(storedPassportData.getIssuer()).isEqualTo(passportDto.getIssuer());

            return null;
        });
    }

    @Test
    @SneakyThrows
    void shouldCreateDraftWithPassportAndSendToDsm() {
        String passportNumber = "1234 123456";
        String normalizedPassportNumber = "1234123456";
        var partnerUserDraftDto = userDraftWithPassport(passportNumber, "RUS");
        var response = mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerUserDraftDto created = tplObjectMapper.readValue(response, PartnerUserDraftDto.class);

        User user = userRepository.findByIdOrThrow(created.getId());
        var driver = driverApiEmulator.getByIdTest(user.getDsmId());

        assertThat(driver).isNotNull();
        assertThat(driver.getId()).isEqualTo(user.getDsmId());
        assertThat(driver.getUid()).isEqualTo(String.valueOf(created.getUid()));
        assertThat(driver.getPersonalData().getPhone()).isEqualTo(created.getPhone());

        assertThat(driver.getPersonalData().getPassportData()).isNotNull();
        var storedPassportData = driver.getPersonalData().getPassportData();
        assertThat(storedPassportData.getFirstName()).isEqualTo(created.getFirstName());
        assertThat(storedPassportData.getLastName()).isEqualTo(created.getLastName());
        assertThat(storedPassportData.getPatronymic()).isEqualTo(created.getPatronymic());
        assertThat(storedPassportData.getCitizenship())
                .isEqualTo(citizenshipRepository.findByIdOrThrow(created.getPassport().getCitizenship()).getNationality());
        assertThat(storedPassportData.getSerialNumber()).isEqualTo(normalizedPassportNumber);
        assertThat(storedPassportData.getBirthDate()).isEqualTo(created.getPassport().getBirthDate());
        assertThat(storedPassportData.getIssueDate()).isEqualTo(created.getPassport().getIssueDate());
        assertThat(storedPassportData.getIssuer()).isEqualTo(created.getPassport().getIssuer());
    }

    @Test
    @SneakyThrows
    void shouldCreateDraftWithPassportWithoutBirthDate() {
        PartnerUserPassportDto passport = new PartnerUserPassportDto();
        passport.setCitizenship("RUS");
        passport.setSerialNumber("1234 123456");
        passport.setIssueDate(LocalDate.of(2014, 1, 1));
        passport.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserCreateDraftDto partnerUserDraftDto = new PartnerUserCreateDraftDto();
        partnerUserDraftDto.setFirstName("Водитель");
        partnerUserDraftDto.setLastName("Третий");
        partnerUserDraftDto.setPhone("+79272403522");
        partnerUserDraftDto.setPassport(passport);

        var response = mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerUserDraftDto created = tplObjectMapper.readValue(response, PartnerUserDraftDto.class);
        User user = userRepository.findByIdOrThrow(created.getId());
        Assertions.assertThat(user).isNotNull();

        var driver = driverApiEmulator.getByIdTest(user.getDsmId());
        assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isNotNull();
        assertThat(driver.getPersonalData().getPassportData().getBirthDate()).isNull();

        mockMvc.perform(get("/internal/partner/users/drafts/{id}", user.getId())
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.passport.serialNumber").value("1234123456"))
                .andExpect(jsonPath("$.passport.birthDate").doesNotExist());
    }

    @Test
    @SneakyThrows
    void shouldNotCreateDuplicateDraftWithPassport() {
        PartnerUserPassportDto passportDto = new PartnerUserPassportDto();
        passportDto.setCitizenship("RUS");
        passportDto.setSerialNumber("1234 123456");
        passportDto.setBirthDate(LocalDate.of(2000, 1, 1));
        passportDto.setIssueDate(LocalDate.of(2014, 1, 1));
        passportDto.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserCreateDraftDto partnerUserDraftDto = new PartnerUserCreateDraftDto();
        partnerUserDraftDto.setFirstName("Водитель");
        partnerUserDraftDto.setLastName("Третий");
        partnerUserDraftDto.setPhone("+79272403522");
        partnerUserDraftDto.setPassport(passportDto);

        mockMvc.perform(
                post("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        PartnerUserPassportDto passportDto2 = new PartnerUserPassportDto();
        passportDto2.setCitizenship("RUS");
        passportDto2.setSerialNumber("3456 789012");
        passportDto2.setBirthDate(LocalDate.of(2000, 1, 1));
        passportDto2.setIssueDate(LocalDate.of(2014, 1, 1));
        passportDto2.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserCreateDraftDto partnerUserDraftDto2 = new PartnerUserCreateDraftDto();
        partnerUserDraftDto2.setFirstName("Водитель");
        partnerUserDraftDto2.setLastName("Третий");
        partnerUserDraftDto2.setPhone("+79272403522");
        partnerUserDraftDto2.setPassport(passportDto2);

         mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto2))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void shouldNotCreateDuplicateDraftWithSamePassport() {
        PartnerUserPassportDto passportDto = new PartnerUserPassportDto();
        passportDto.setCitizenship("RUS");
        passportDto.setSerialNumber("1234 123456");
        passportDto.setBirthDate(LocalDate.of(2000, 1, 1));
        passportDto.setIssueDate(LocalDate.of(2014, 1, 1));
        passportDto.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserCreateDraftDto partnerUserDraftDto = new PartnerUserCreateDraftDto();
        partnerUserDraftDto.setFirstName("Водитель");
        partnerUserDraftDto.setLastName("Третий");
        partnerUserDraftDto.setPhone("+79272403522");
        partnerUserDraftDto.setPassport(passportDto);

        PartnerUserCreateDraftDto partnerUserDraftDto2 = new PartnerUserCreateDraftDto();
        partnerUserDraftDto2.setFirstName("Водитель");
        partnerUserDraftDto2.setLastName("Третий");
        partnerUserDraftDto2.setPhone("+79272403523");
        partnerUserDraftDto2.setPassport(passportDto);

        var response1 = mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var response2 = mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto2))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerUserDraftDto user1 = tplObjectMapper.readValue(response1, PartnerUserDraftDto.class);
        PartnerUserDraftDto user2 = tplObjectMapper.readValue(response2, PartnerUserDraftDto.class);

        Assertions.assertThat(user1.getId()).isEqualTo(user2.getId());
    }

    @Test
    @SneakyThrows
    void shouldTranslateCyrillicCharsToLatinChars() {
        String cyrillic = "ВМ 2769471";
        String latin = "BM2769471";

        PartnerUserCreateDraftDto partnerUserDraftDto = userDraftWithPassport(cyrillic, "BLR");

        var response = mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerUserDraftDto created = tplObjectMapper.readValue(response, PartnerUserDraftDto.class);
        User user = userRepository.findByIdOrThrow(created.getId());
        Assertions.assertThat(user).isNotNull();

        var driver = driverApiEmulator.getByIdTest(user.getDsmId());
        assertThat(driver.getPersonalData().getPassportData().getSerialNumber()).isEqualTo(latin);
    }


    @SneakyThrows
    @Test
    void shouldReturn400OnInvalidPassport() {
        PartnerUserCreateDraftDto partnerUserDraftDto = userDraftWithPassport("FV 1234 123456", "RUS");

        mockMvc.perform(
                post("/internal/partner/users/drafts")
                    .header(COMPANY_HEADER, company.getCampaignId())
                    .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
            )
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.message")
                    .value("Серийный номер FV1234123456 не соответствует формату " +
                            "должно быть 10 цифр, например 123456789"));
    }


    @SneakyThrows
    @Test
    void shouldReturn400InvalidCharactersInPassport() {
        PartnerUserCreateDraftDto partnerUserDraftDto = userDraftWithPassport("BB 111 Ы", "BLR");

        mockMvc.perform(
                post("/internal/partner/users/drafts")
                    .header(COMPANY_HEADER, company.getCampaignId())
                    .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
            )
            .andExpect(status().is4xxClientError())
            .andExpect(jsonPath("$.message")
                    .value("Символ 'Ы' в позиции 7 строки 'BB 111 Ы' не является латинской буквой"));
    }

    @Test
    @SneakyThrows
    void shouldUpdateDraft() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);


        PartnerUserCreateDraftDto partnerUserDraftDto = new PartnerUserCreateDraftDto();
        partnerUserDraftDto.setFirstName("Водитель");
        partnerUserDraftDto.setLastName("Третий");
        partnerUserDraftDto.setPhone("+79272403522");

        mockMvc.perform(put("/internal/partner/users/drafts/{id}", draft.getId())
                .header(COMPANY_HEADER, company.getCampaignId())
                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Водитель"))
            .andExpect(jsonPath("$.lastName").value("Третий"))
            .andExpect(jsonPath("$.phone").value("+79272403522"))
        ;
    }

    @Test
    @SneakyThrows
    void shouldNormalizePhoneOnUpdate() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);

        PartnerUserCreateDraftDto partnerUserDraftDto = new PartnerUserCreateDraftDto();
        partnerUserDraftDto.setFirstName("Водитель");
        partnerUserDraftDto.setLastName("Третий");
        partnerUserDraftDto.setPhone("79111111111");

        mockMvc.perform(put("/internal/partner/users/drafts/{id}", draft.getId())
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Водитель"))
                .andExpect(jsonPath("$.lastName").value("Третий"))
                .andExpect(jsonPath("$.phone").value("+79111111111"))
        ;

        User updated = userRepository.findByIdOrThrow(draft.getId());
        var driver = driverApiEmulator.getByIdTest(updated.getDsmId());
        assertThat(driver.getPersonalData().getPhone()).isEqualTo("+79111111111");
    }



    @Test
    @SneakyThrows
    void shouldUpdateDraftWithPassport() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .passport(PassportData.builder()
                        .citizenship("RUS")
                        .serialNumber("1234123456")
                        .birthDate(LocalDate.of(2000, 1, 1))
                        .issueDate(LocalDate.of(2014, 1, 1))
                        .issuer("ТП УФМС Гондора в Минас Тирит")
                        .build())
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);

        PartnerUserPassportDto passportDto = new PartnerUserPassportDto();
        passportDto.setCitizenship("RUS");
        passportDto.setSerialNumber("1234 987654");
        passportDto.setBirthDate(LocalDate.of(2000, 1, 1));
        passportDto.setIssueDate(LocalDate.of(2014, 1, 1));
        passportDto.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserUpdateDraftDto partnerUserDraftDto = new PartnerUserUpdateDraftDto();
        partnerUserDraftDto.setFirstName("Водитель");
        partnerUserDraftDto.setLastName("Третий");
        partnerUserDraftDto.setPhone("+79272403522");
        partnerUserDraftDto.setPassport(passportDto);

        mockMvc.perform(put("/internal/partner/users/drafts/{id}", draft.getId())
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Водитель"))
                .andExpect(jsonPath("$.lastName").value("Третий"))
                .andExpect(jsonPath("$.phone").value("+79272403522"))
                .andExpect(jsonPath("$.passport.citizenship").value("RUS"))
                .andExpect(jsonPath("$.passport.serialNumber").value("1234987654"))
                .andExpect(jsonPath("$.passport.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.passport.issueDate").value("2014-01-01"))
                .andExpect(jsonPath("$.passport.issuer").value("ТП УФМС Гондора в Минас Тирит"));
    }

    @Test
    @SneakyThrows
    void shouldUpdateDraftWithPhone() {
        PartnerUserCreateDraftDto partnerUserDraftDto = defaultCreateUserDto();
        partnerUserDraftDto.setFirstName("Ололош");
        partnerUserDraftDto.setLastName("Ололоев");
        partnerUserDraftDto.setPhone("+79272403522");

        var response = mockMvc.perform(
                        post("/internal/partner/users/drafts")
                                .header(COMPANY_HEADER, company.getCampaignId())
                                .content(tplObjectMapper.writeValueAsString(partnerUserDraftDto))
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PartnerUserDraftDto created = tplObjectMapper.readValue(response, PartnerUserDraftDto.class);
        User draft = userRepository.findByIdOrThrow(created.getId());

        var driver = driverApiEmulator.getByIdTest(draft.getDsmId());
        assertThat(driver.getPersonalData().getPhone()).isEqualTo("+79272403522");

        PartnerUserPassportDto passportDto = new PartnerUserPassportDto();
        passportDto.setCitizenship("RUS");
        passportDto.setSerialNumber("1234 987654");
        passportDto.setBirthDate(LocalDate.of(2000, 1, 1));
        passportDto.setIssueDate(LocalDate.of(2014, 1, 1));
        passportDto.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserUpdateDraftDto updateDraftDto = new PartnerUserUpdateDraftDto();
        updateDraftDto.setFirstName("Водитель");
        updateDraftDto.setLastName("Третий");
        updateDraftDto.setPassport(passportDto);
        updateDraftDto.setPhone("+74950000000");

        mockMvc.perform(put("/internal/partner/users/drafts/{id}", draft.getId())
                        .header(COMPANY_HEADER, company.getCampaignId())
                        .content(tplObjectMapper.writeValueAsString(updateDraftDto))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Водитель"))
                .andExpect(jsonPath("$.lastName").value("Третий"))
                .andExpect(jsonPath("$.phone").value("+74950000000"));

        var driverAfter = driverApiEmulator.getByIdTest(draft.getDsmId());
        assertThat(driverAfter.getPersonalData().getPhone()).isEqualTo("+74950000000");
    }

    @Test
    @SneakyThrows
    void shouldDeleteUserFromCompany() {
        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .source(UserSource.CARRIER)
                .build();

        User draft = userFacade.createUser(userData, company);


        mockMvc.perform(delete("/internal/partner/users/drafts/{id}", draft.getId())
                        .header(COMPANY_HEADER, company.getCampaignId()))
                        .andExpect(jsonPath("$.deleted").value(true))
                .andReturn()
                .getResponse();

        mockMvc.perform(
                get("/internal/partner/users/drafts/{id}", draft.getId())
                        .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().is4xxClientError());

    }

    @Test
    @SneakyThrows
    void shouldGetFilteredDrafts() {
        Company otherCompany = testUserHelper.findOrCreateCompany(
                TestUserHelper.CompanyGenerateParam.builder()
                        .companyName("Галера Индастриз")
                        .login("ceo@galera.inc")
                        .campaignId(2)
                        .build());

        userFacade.createUser(
                UserData.builder()
                        .phone("+79272403522")
                        .firstName("Ололош")
                        .lastName("Ололоев")
                        .patronymic("Ололоевич")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userFacade.createUser(
                UserData.builder()
                        .phone("+79057910573")
                        .firstName("Ваня")
                        .lastName("Викторов")
                        .patronymic("Иванович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userFacade.createUser(
                UserData.builder()
                        .phone("+79222220573")
                        .firstName("Алеша")
                        .lastName("Дубнов")
                        .patronymic("Владимирович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userFacade.createUser(
                UserData.builder()
                        .phone("+79056937573")
                        .firstName("Ололош")
                        .lastName("Скайуокер")
                        .patronymic("Дартвейдерович")
                        .source(UserSource.CARRIER)
                        .build(),
                otherCompany
        );

        var deletedUser = userFacade.createUser(
                UserData.builder()
                        .phone("+79052400012")
                        .firstName("Юзер")
                        .lastName("Ололоев")
                        .patronymic("Удаленович")
                        .source(UserSource.CARRIER)
                        .build(),
                otherCompany
        );

        userCommandService.deleteFromCompany(
                new UserCommand.DeleteFromCompany(deletedUser.getId(), otherCompany.getId()));

        mockMvc.perform(get("/internal/partner/users/drafts")
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(3)));

       mockMvc.perform(get("/internal/partner/users/drafts")
                .param("phone", "7905")
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("$.content[0].phone")
                    .value(Matchers.containsString("7905")));


        mockMvc.perform(get("/internal/partner/users/drafts")
                .param("phone", "73")
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[0].phone")
                        .value(Matchers.containsString("73")))
                .andExpect(jsonPath("$.content[1].phone")
                        .value(Matchers.containsString("73")));

        mockMvc.perform(get("/internal/partner/users/drafts")
                .param("phone", "73")
                .param("name", "ва")
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].phone").value("+79057910573"))
                .andExpect(jsonPath("$.content[0].firstName").value("Ваня"))
                .andExpect(jsonPath("$.content[0].lastName").value("Викторов"));

        mockMvc.perform(get("/internal/partner/users/drafts")
                .param("name", "ЛОЛ")
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Ололош"))
                .andExpect(jsonPath("$.content[0].lastName").value("Ололоев"));
    }


    @Test
    @SneakyThrows
    void shouldSuggestDraftsByName() {
        userFacade.createUser(
                UserData.builder()
                        .phone("+79272403521")
                        .firstName("Василий")
                        .lastName("Сидоров")
                        .patronymic("Иванович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        var blackListedUser = userFacade.createUser(
                UserData.builder()
                        .phone("+79272403526")
                        .firstName("Ололош")
                        .lastName("Ололоев")
                        .patronymic("Ололоевич")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userCommandService.markBlackListed(
                new UserCommand.MarkBlackListed(blackListedUser.getId(), UserBlackListReason.NOT_USING_APP));


        mockMvc.perform(get("/internal/partner/users/drafts/name")
                .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Сидоров Василий"));
    }


    @Test
    @SneakyThrows
    void shouldSuggestDraftsPhones() {
        userFacade.createUser(
                UserData.builder()
                        .phone("+79272400291")
                        .firstName("Василий")
                        .lastName("Сидоров")
                        .patronymic("Иванович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userFacade.createUser(
                UserData.builder()
                        .phone("+79272401234")
                        .firstName("Василий")
                        .lastName("Викторов")
                        .patronymic("Олегович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userFacade.createUser(
                UserData.builder()
                        .phone("+79272423523")
                        .firstName("Ололош")
                        .lastName("Ололоев")
                        .patronymic("Ололоевич")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        mockMvc.perform(get("/internal/partner/users/drafts/phone")
                        .param("phone", "24235")
                        .header(COMPANY_HEADER, company.getCampaignId())
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$[*].phone")
                        .value(Matchers.containsInAnyOrder("+79272423523")));

        mockMvc.perform(get("/internal/partner/users/drafts/phone")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(3)))
                .andExpect(jsonPath("$[*].phone")
                        .value(Matchers.containsInAnyOrder("+79272400291", "+79272401234", "+79272423523")));
    }

    @Test
    @SneakyThrows
    void shouldFilterDraftsConsideringLowercase() {
        userFacade.createUser(
                UserData.builder()
                        .phone("+79272400291")
                        .firstName("ыть")
                        .lastName("Яндекс")
                        .patronymic("Леонидович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userFacade.createUser(
                UserData.builder()
                        .phone("+79272401233")
                        .firstName("Ыван")
                        .lastName("ЫТалия")
                        .patronymic("Ываныч")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        userFacade.createUser(
                UserData.builder()
                        .phone("+79272401234")
                        .firstName("Василий")
                        .lastName("Викторов")
                        .patronymic("Олегович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        mockMvc.perform(get("/internal/partner/users/drafts")
                .header(COMPANY_HEADER, company.getCampaignId())
                .param("name", "ЫТ")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/partner/users/drafts")
                .header(COMPANY_HEADER, company.getCampaignId())
                .param("name", "ыт")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void enterPassportTagIsPresentInPartnerUserGridViewCarrier() {
        userFacade.createUser(
                UserData.builder()
                        .phone("+79272400291")
                        .firstName("ыть")
                        .lastName("Яндекс")
                        .patronymic("Леонидович")
                        .source(UserSource.CARRIER)
                        .build(),
                company
        );

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].passport").doesNotExist())
                .andExpect(jsonPath("$.content[0].tags[0].tag").value(PartnerUserTag.NO_PASSPORT.name()))
                .andExpect(jsonPath("$.content[0].tags[0].color")
                        .value(FrontColor.DANGER.getFrontRepresentation()));
    }

    @SneakyThrows
    @Test
    void enterPassportTagIsNotPresentInPartnerUserGridViewCarrier() {
        userFacade.createUser(
                UserData.builder()
                        .phone("+79272400291")
                        .firstName("ыть")
                        .lastName("Яндекс")
                        .patronymic("Леонидович")
                        .source(UserSource.CARRIER)
                        .passport(PassportData.builder()
                                .citizenship("RUS")
                                .serialNumber("1234123456")
                                .birthDate(LocalDate.of(2000, 1, 1))
                                .issueDate(LocalDate.of(2014, 1, 1))
                                .issuer("ТП УФМС Гондора в Минас Тирит")
                                .build())
                        .build(),
                company
        );

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].passport").exists())
                .andExpect(jsonPath("$.content[0].passport.serialNumber").exists())
                .andExpect(jsonPath("$.content[0].tags").value(Matchers.hasSize(0)));
    }

    @SneakyThrows
    @Test
    void enterPassportTagIsPresentInPartnerUserGridViewDsm() {
        var userData = UserData.builder()
                .phone("+79272400291")
                .firstName("ыть")
                .lastName("Яндекс")
                .patronymic("Леонидович")
                .source(UserSource.CARRIER)
                .build();
        User created = userFacade.createUser(userData, company);

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].passport").doesNotExist())
                .andExpect(jsonPath("$.content[0].tags[0].tag").value(PartnerUserTag.NO_PASSPORT.name()))
                .andExpect(jsonPath("$.content[0].tags[0].color")
                        .value(FrontColor.DANGER.getFrontRepresentation()));
    }

    @SneakyThrows
    @Test
    void enterPassportTagIsNotPresentInPartnerUserGridViewDsm() {
        User created = userFacade.createUser(
                UserData.builder()
                        .phone("+79272400291")
                        .firstName("ыть")
                        .lastName("Яндекс")
                        .patronymic("Леонидович")
                        .source(UserSource.CARRIER)
                        .passport(PassportData.builder()
                                .citizenship("RUS")
                                .serialNumber("1234123456")
                                .birthDate(LocalDate.of(2000, 1, 1))
                                .issueDate(LocalDate.of(2014, 1, 1))
                                .issuer("ТП УФМС Гондора в Минас Тирит")
                                .build())
                        .build(),
                company
        );

        mockMvc.perform(get("/internal/partner/users/drafts")
                        .header(COMPANY_HEADER, company.getCampaignId())
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].passport").exists())
                .andExpect(jsonPath("$.content[0].passport.serialNumber").exists())
                .andExpect(jsonPath("$.content[0].tags").value(Matchers.hasSize(0)));
    }

    private static PartnerUserCreateDraftDto userDraftWithPassport(String serialNumber, String citizenship) {
        PartnerUserPassportDto passportDto = new PartnerUserPassportDto();
        passportDto.setCitizenship(citizenship);
        passportDto.setSerialNumber(serialNumber);
        passportDto.setBirthDate(LocalDate.of(2000, 1, 1));
        passportDto.setIssueDate(LocalDate.of(2014, 1, 1));
        passportDto.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserCreateDraftDto partnerUserDraftDto = new PartnerUserCreateDraftDto();
        partnerUserDraftDto.setFirstName("Водитель");
        partnerUserDraftDto.setLastName("Третий");
        partnerUserDraftDto.setPhone("+79272403522");
        partnerUserDraftDto.setPassport(passportDto);

        return partnerUserDraftDto;
    }

    private static PartnerUserCreateDraftDto defaultCreateUserDto() {
        PartnerUserPassportDto passportDto = new PartnerUserPassportDto();
        passportDto.setCitizenship("RUS");
        passportDto.setSerialNumber("1234 987654");
        passportDto.setBirthDate(LocalDate.of(2000, 1, 1));
        passportDto.setIssueDate(LocalDate.of(2014, 1, 1));
        passportDto.setIssuer("ТП УФМС Гондора в Минас Тирит");

        PartnerUserCreateDraftDto partnerUserCreateDraftDto = new PartnerUserCreateDraftDto();
        partnerUserCreateDraftDto.setFirstName("Водитель");
        partnerUserCreateDraftDto.setLastName("Третий");
        partnerUserCreateDraftDto.setPhone("+79272403522");
        partnerUserCreateDraftDto.setPassport(passportDto);

        return partnerUserCreateDraftDto;
    }

}
