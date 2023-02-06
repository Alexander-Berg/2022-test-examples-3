package ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.b2bcrm.module.account.Account;
import ru.yandex.market.b2bcrm.module.account.B2bAccountContactRelation;
import ru.yandex.market.b2bcrm.module.account.B2bContact;
import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointOwner;
import ru.yandex.market.b2bcrm.module.pickuppoint.PickupPointPotentialTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.PrePickupPointTicket;
import ru.yandex.market.b2bcrm.module.pickuppoint.config.B2bPickupPointTests;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.LegalPartnerPupEvent;
import ru.yandex.market.b2bcrm.module.pickuppoint.logbroker.model.PupEvent;
import ru.yandex.market.b2bcrm.module.utils.AccountModuleTestUtils;
import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.entity.test.assertions.EntityAssert;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.logic.def.HasCreationTime;
import ru.yandex.market.jmf.logic.wf.bcp.WfConstants;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.pvz.client.crm.dto.CrmPayloadType;
import ru.yandex.market.pvz.client.crm.dto.LegalPartnerCrmDto;
import ru.yandex.market.pvz.client.crm.dto.LegalPartnerCrmDto.Accountant;
import ru.yandex.market.pvz.client.crm.dto.LegalPartnerCrmDto.Delegate;
import ru.yandex.market.pvz.client.crm.dto.LegalPartnerCrmDto.Organization;
import ru.yandex.market.pvz.client.crm.dto.LegalPartnerCrmDto.PersonName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.jmf.entity.test.assertions.EntityAttributeMatcher.havingAttributes;

@B2bPickupPointTests
@ExtendWith(SpringExtension.class)
public class LegalPartnerPupEventProcessorTest {
    private static final PupEvent<?> EVENT;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        EVENT = Exceptions.sneakyRethrow(() ->
                objectMapper.readValue(
                        LegalPartnerPupEventProcessorTest.class.getResource(
                                "legalPartnerPupEvent.json"),
                        PupEvent.class
                )
        );
    }

    @Inject
    private LegalPartnerPupEventProcessor processor;

    @Inject
    private DbService dbService;

    @Inject
    private BcpService bcpService;

    @Inject
    private TicketTestUtils ticketTestUtils;

    @Inject
    private AccountModuleTestUtils accountModuleTestUtils;

    @Test
    public void shouldCreateNewAccount() {
        PickupPointOwner diffPupId = createAccount(2L, "Отличный pupId");
        PickupPointOwner noPupId = createAccount(null, "Не указан pupId");
        PickupPointOwner archived = bcpService.edit(
                createAccount(1L, "Архивный"),
                Maps.of(PickupPointOwner.STATUS, "archived"),
                Maps.of(WfConstants.SKIP_WF_STATUS_CHANGE_CHECKING_ATTRIBUTE, true)
        );
        processor.process(EVENT);
        assertAccountAttributes(getLastAccount());
        EntityCollectionAssert.assertThat(dbService.<PickupPointOwner>list(Query.of(PickupPointOwner.FQN)))
                .hasSize(4)
                .withFailMessage("Партнёры с отличным 'ID ПО ПВЗ' должны остаться без изменений")
                .anyHasAttributes(PickupPointOwner.TITLE, diffPupId.getTitle())
                .anyHasAttributes(PickupPointOwner.TITLE, noPupId.getTitle())

                .withFailMessage("Партнёры 'в архиве' должны остаться без изменений")
                .anyHasAttributes(PickupPointOwner.TITLE, archived.getTitle());
    }

    private void assertAccountAttributes(PickupPointOwner account) {
        EntityAssert.assertThat(account)
                .hasAttributes(
                        //innerIds
                        PickupPointOwner.PUP_ID, "1",
                        PickupPointOwner.CAMPAIGN_ID, 1001073997L,
                        PickupPointOwner.OWNER_UID, "47252849",
                        PickupPointOwner.VIRTUAL_ACCOUNT_NUMBER, "DOSTAVKA_P_228-A",
                        //contract
                        PickupPointOwner.CONTRACT_DATE, LocalDate.parse("2020-12-31"),
                        PickupPointOwner.CONTRACT_NUMBER, "234234",
                        //ordData
                        PickupPointOwner.POST_ADDRESS, "129626, Москва, Рижский проезд, 3, офис 215",
                        PickupPointOwner.LEGAL_ADDRESS, "129626, Москва, Рижский проезд, 3, офис 215",
                        PickupPointOwner.TAXATION, "Общая система налогообложения",
                        PickupPointOwner.COMMISSIONER, "Генеральный директор Радолин Денис Алексеевич на основании устава",
                        PickupPointOwner.OFFER_SIGNED_SINCE, LocalDate.parse("2021-01-20"),
                        PickupPointOwner.TITLE, "DenLab Solutions",
                        PickupPointOwner.INN, "5145762403",
                        PickupPointOwner.KPP, "297144842",
                        PickupPointOwner.OGRN, "5128594032463",
                        PickupPointOwner.ISSUE_DATE, LocalDate.parse("2017-05-29"),
                        //bankData
                        PickupPointOwner.BANK, "Тинькофф Банк",
                        PickupPointOwner.BANK_CITY, "Москва",
                        PickupPointOwner.BANK_KPP, "000000000",
                        PickupPointOwner.BANK_RCBIC, "111111111",
                        PickupPointOwner.BANK_CHECKING_ACCOUNT, "22312313123123123123",
                        PickupPointOwner.BANK_CORRESPONDENT_ACCOUNT, "12441235345645675745"
                );
    }

    @Test
    public void shouldEditExistingAccount() {
        PickupPointOwner account = createAccount(1L, "Должен измениться");
        processor.process(EVENT);
        assertAccountAttributes(account);
    }

    @Test
    public void shouldCreateNewContacts() {
        PickupPointOwner account = createAccount(1L, "someAccount");
        createContact(account, "PICKUP_POINT_OWNER", List.of(), List.of());
        processor.process(EVENT);
        EntityCollectionAssert.assertThat(dbService.list(Query.of(B2bAccountContactRelation.FQN)))
                .hasSize(3)
                .anyHasAttributes(
                        B2bAccountContactRelation.ACCOUNT, account.getGid(),
                        B2bAccountContactRelation.CONTACT_ROLE, "PICKUP_POINT_DELEGATE",
                        B2bAccountContactRelation.SOURCE_SYSTEM, "ПВЗ",
                        B2bAccountContactRelation.CONTACT, havingAttributes(
                                B2bContact.TITLE, "Радолин Денис Алексеевич (Представитель)",
                                B2bContact.LAST_NAME, "Радолин",
                                B2bContact.FIRST_NAME, "Денис",
                                B2bContact.SECOND_NAME, "Алексеевич",
                                B2bContact.SOURCE_SYSTEM, "ПВЗ",
                                B2bContact.PHONES, List.of("+79998205033"),
                                B2bContact.EMAILS, List.of("denr01_delegate@ya.ru"),
                                B2bContact.UPDATE_TIME, notNullValue(OffsetDateTime.class)
                        )
                )
                .anyHasAttributes(
                        B2bAccountContactRelation.ACCOUNT, account.getGid(),
                        B2bAccountContactRelation.CONTACT_ROLE, "PICKUP_POINT_ACCOUNTANT",
                        B2bAccountContactRelation.SOURCE_SYSTEM, "ПВЗ",
                        B2bAccountContactRelation.CONTACT, havingAttributes(
                                B2bContact.TITLE, "Радолин Денис Алексеевич (Бухгалтер)",
                                B2bContact.LAST_NAME, "Радолин",
                                B2bContact.FIRST_NAME, "Денис",
                                B2bContact.SECOND_NAME, "Алексеевич",
                                B2bContact.SOURCE_SYSTEM, "ПВЗ",
                                B2bContact.PHONES, List.of("+79998205033"),
                                B2bContact.EMAILS, List.of("denr01_accountant@ya.ru"),
                                B2bContact.UPDATE_TIME, notNullValue(OffsetDateTime.class)
                        )
                )

                .withFailMessage("Контакты с ролью отличной от 'Представитель организации (ПВЗ)' и 'Бухгалтер (ПВЗ)' " +
                        "должны остаться без изменений")
                .anyHasAttributes(
                        B2bAccountContactRelation.ACCOUNT, account.getGid(),
                        B2bAccountContactRelation.CONTACT_ROLE, "PICKUP_POINT_OWNER",
                        B2bAccountContactRelation.CONTACT, havingAttributes(
                                B2bContact.TITLE, "someContact",
                                B2bContact.PHONES, List.of(),
                                B2bContact.EMAILS, List.of(),
                                B2bContact.UPDATE_TIME, null
                        )
                );
    }

    @ParameterizedTest
    @CsvSource({
            "PICKUP_POINT_DELEGATE, denr01_delegate@ya.ru",
            "PICKUP_POINT_ACCOUNTANT, denr01_accountant@ya.ru",
    })
    public void shouldEditExistingContact(String role, String email) {
        PickupPointOwner account = createAccount(1L, "someAccount");
        B2bContact contact = createContact(account, role, List.of("a@ya.ru"), List.of("+79999999999"));
        processor.process(EVENT);
        EntityAssert.assertThat(contact)
                .hasAttributes(
                        B2bContact.PHONES, List.of("+79999999999", "+79998205033"),
                        B2bContact.EMAILS, List.of("a@ya.ru", email),
                        B2bContact.UPDATE_TIME, notNullValue(OffsetDateTime.class)
                );
    }

    @Test
    public void shouldEditPartnerOnTicket() {
        PickupPointPotentialTicket pickupPointPotentialTicket = ticketTestUtils.createTicket(
                PickupPointPotentialTicket.FQN,
                Maps.of(PickupPointPotentialTicket.PUP_LEGAL_PARTNER_ID, 1L)
        );
        PrePickupPointTicket prePickupPointTicket = ticketTestUtils.createTicket(
                PrePickupPointTicket.FQN,
                Maps.of(PrePickupPointTicket.PUP_LEGAL_PARTNER_ID, 1L)
        );
        processor.process(EVENT);
        PickupPointOwner account = getLastAccount();
        EntityAssert.assertThat(pickupPointPotentialTicket)
                .hasAttributes(PickupPointPotentialTicket.PARTNER, account.getGid());
        EntityAssert.assertThat(prePickupPointTicket)
                .hasAttributes(PickupPointPotentialTicket.PARTNER, account.getGid());
    }

    @Test
    public void shouldNotEditPartnerOnTicket() {
        PickupPointPotentialTicket pickupPointPotentialTicket = ticketTestUtils.createTicket(
                PickupPointPotentialTicket.FQN,
                Maps.of(PickupPointPotentialTicket.PUP_LEGAL_PARTNER_ID, 2L)
        );
        PrePickupPointTicket prePickupPointTicket = ticketTestUtils.createTicket(
                PrePickupPointTicket.FQN,
                Maps.of(PrePickupPointTicket.PUP_LEGAL_PARTNER_ID, null)
        );
        processor.process(EVENT);
        assertThat(pickupPointPotentialTicket.getPartner()).isNull();
        assertThat(prePickupPointTicket.getPartner()).isNull();
    }

    @Test
    public void shouldCreateContactWithEmptyFullName() {
        Accountant accountant = Accountant.builder()
                .accountantEmail("accountant@ya.ru")
                .accountantName(null)
                .accountant(PersonName.builder().lastName("Last").firstName("First").patronymic("Generated").build())
                .build();
        Delegate delegate = Delegate.builder()
                .delegateEmail("delegate@ya.ru")
                .delegateName(null)
                .delegate(PersonName.builder().lastName("  ").firstName("\n").patronymic(null).build())
                .build();
        processor.process(new LegalPartnerPupEvent(
                CrmPayloadType.LEGAL_PARTNER,
                Instant.now(),
                LegalPartnerCrmDto.builder()
                        .id(Randoms.longValue())
                        .organization(Organization.builder().name(Randoms.string()).build())
                        .accountant(accountant)
                        .delegate(delegate)
                        .build()
        ));
        PickupPointOwner account = getLastAccount();
        EntityCollectionAssert.assertThat(dbService.list(Query.of(B2bAccountContactRelation.FQN)))
                .hasSize(2)

                .withFailMessage("Title контакта должен быть сгенерирован из фамилии, имени и отчества")
                .anyHasAttributes(
                        B2bAccountContactRelation.ACCOUNT, account.getGid(),
                        B2bAccountContactRelation.CONTACT_ROLE, "PICKUP_POINT_ACCOUNTANT",
                        B2bAccountContactRelation.SOURCE_SYSTEM, "ПВЗ",
                        B2bAccountContactRelation.CONTACT, havingAttributes(
                                B2bContact.TITLE, "Last First Generated",
                                B2bContact.LAST_NAME, "Last",
                                B2bContact.FIRST_NAME, "First",
                                B2bContact.SECOND_NAME, "Generated",
                                B2bContact.SOURCE_SYSTEM, "ПВЗ",
                                B2bContact.PHONES, List.of(),
                                B2bContact.EMAILS, List.of("accountant@ya.ru"),
                                B2bContact.UPDATE_TIME, notNullValue(OffsetDateTime.class)
                        )
                )

                .withFailMessage("Если в фио нет данных, то Title контакта должен быть 'Не указано'")
                .anyHasAttributes(
                        B2bAccountContactRelation.ACCOUNT, account.getGid(),
                        B2bAccountContactRelation.CONTACT_ROLE, "PICKUP_POINT_DELEGATE",
                        B2bAccountContactRelation.SOURCE_SYSTEM, "ПВЗ",
                        B2bAccountContactRelation.CONTACT, havingAttributes(
                                B2bContact.TITLE, "Не указано",
                                B2bContact.LAST_NAME, null,
                                B2bContact.FIRST_NAME, null,
                                B2bContact.SECOND_NAME, null,
                                B2bContact.SOURCE_SYSTEM, "ПВЗ",
                                B2bContact.PHONES, List.of(),
                                B2bContact.EMAILS, List.of("delegate@ya.ru"),
                                B2bContact.UPDATE_TIME, notNullValue(OffsetDateTime.class)
                        )
                );
    }

    private PickupPointOwner createAccount(Long pupId, String title) {
        return bcpService.create(PickupPointOwner.FQN, Maps.of(
                PickupPointOwner.TITLE, title,
                PickupPointOwner.PUP_ID, pupId
        ));
    }

    private B2bContact createContact(Account account, String role, List<String> emails, List<String> phones) {
        return accountModuleTestUtils.createContactForAccount(
                account,
                role,
                "someContact",
                "ПВЗ",
                emails,
                phones
        );
    }

    private PickupPointOwner getLastAccount() {
        Query query = Query.of(PickupPointOwner.FQN)
                .withSortingOrder(SortingOrder.desc(HasCreationTime.CREATION_TIME));
        return dbService.<PickupPointOwner>list(query).get(0);
    }
}
