package ru.yandex.market.logistics.management.service.balance.utills;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import lombok.experimental.UtilityClass;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.management.domain.balance.ClientInfo;
import ru.yandex.market.logistics.management.domain.balance.ClientSearchParams;
import ru.yandex.market.logistics.management.domain.balance.CreateOfferInfo;
import ru.yandex.market.logistics.management.domain.balance.LinkIntegrationToClientInfo;
import ru.yandex.market.logistics.management.domain.balance.OfferInfo;
import ru.yandex.market.logistics.management.domain.balance.PersonInfo;
import ru.yandex.market.logistics.management.domain.balance.UpdateOfferInfo;
import ru.yandex.market.logistics.management.domain.balance.structure.ClientSearchParamsStructure;
import ru.yandex.market.logistics.management.domain.balance.structure.ClientStructure;
import ru.yandex.market.logistics.management.domain.balance.structure.CreateOfferStructure;
import ru.yandex.market.logistics.management.domain.balance.structure.LinkIntegrationToClientStructure;
import ru.yandex.market.logistics.management.domain.balance.structure.PersonStructure;
import ru.yandex.market.logistics.management.domain.balance.structure.UpdateOfferStructure;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class BalanceEntityUtils {
    public static final Integer CLIENT_ID = 123;
    public static final Integer PERSON_ID = 13132301;
    public static final Long OPERATOR_UID = 100L;
    public static final Long USER_UID = 123456L;
    private static final String ORG_NAME = "ТЕСТ";
    private static final String ORG_LONG_NAME = "ООО ТЕСТ";
    private static final String PHONE = "+7(800)700-00-00";
    private static final String EMAIL = "roga@kopyta.ru";
    private static final String POST_CODE = "633372";
    private static final String POST_ADDRESS =
        "фактическое село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2";
    private static final String LEGAL_ADDRESS =
        "юридическое село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2";
    private static final String INN = "7777777777";
    private static final String BIK = "112233";
    private static final String ACCOUNT = "account";
    private static final String KPP = "332211";
    private static final String ORG_TYPE = "ur";
    private static final Boolean IS_PARTNER_BOOLEAN = true;
    private static final Integer IS_PARTNER_INTEGER = 1;
    private static final String CURRENCY = "RUR";
    private static final Long FIRM_ID = 111L;
    private static final String INTEGRATION = "market_logistics_partner";
    private static final Long MANAGER_UID = 1120000000288121L;
    private static final Integer NDS = 18;
    private static final List<Integer> SERVICES = List.of(725);
    private static final Boolean IS_SIGNED = true;
    private static final Date START_DATE = new Calendar.Builder()
        .setDate(2021, 1, 1)
        .setTimeZone(TimeZone.getTimeZone(DateTimeUtils.MOSCOW_ZONE))
        .build()
        .getTime();
    private static final Integer OFFER_ID = 133;
    private static final String OFFER_EXTERNAL_ID = "offerExternalId";

    @Nonnull
    public static ClientSearchParamsStructure searchParamsRequest() {
        ClientSearchParamsStructure params = new ClientSearchParamsStructure();
        params.setPassportId(USER_UID.toString());
        return params;
    }

    @Nonnull
    public static ClientSearchParams clientSearchParams() {
        return ClientSearchParams.builder().passportUid(USER_UID).build();
    }

    @Nonnull
    public static ClientInfo client(Integer id) {
        return ClientInfo.builder()
            .id(id)
            .name("ООО ТЕСТ")
            .url("https://test.ru")
            .phone("+7(800)700-00-00")
            .build();
    }

    @Nonnull
    public static Map<String, Object> clientResponse(Integer id) {
        return Map.of(
            "CLIENT_ID", id,
            "NAME", "ООО ТЕСТ",
            "URL", "https://test.ru",
            "PHONE", "+7(800)700-00-00"
        );
    }

    @Nonnull
    public static Map<String, Object> clientResponse() {
        return Map.of(
            "CLIENT_ID", CLIENT_ID,
            "NAME", "ООО ТЕСТ",
            "URL", "https://test.ru",
            "PHONE", "+7(800)700-00-00"
        );
    }

    @Nonnull
    public static ClientStructure clientRequest() {
        ClientStructure client = new ClientStructure();
        client.setName("ООО ТЕСТ");
        client.setUrl("https://test.ru");
        client.setPhone("+7(800)700-00-00");
        return client;
    }

    /**
     * По каким-то причинам плательщика Баланс отдает именно в виде Map<String, String>, а не Map<String, Object>.
     * Поэтому заполняем специально строковыми значениями.
     */
    @Nonnull
    public static Map<String, String> personResponse() {
        return ImmutableMap.<String, String>builder()
            .put("ID", PERSON_ID.toString())
            .put("PERSON_ID", PERSON_ID.toString())
            .put("CLIENT_ID", CLIENT_ID.toString())
            .put("TYPE", ORG_TYPE)
            .put("NAME", ORG_NAME)
            .put("LONGNAME", ORG_LONG_NAME)
            .put("PHONE", PHONE)
            .put("EMAIL", EMAIL)
            .put("POSTCODE", POST_CODE)
            .put("POSTADDRESS", POST_ADDRESS)
            .put("LEGALADDRESS", LEGAL_ADDRESS)
            .put("INN", INN)
            .put("BIK", BIK)
            .put("ACCOUNT", ACCOUNT)
            .put("KPP", KPP)
            .put("IS_PARTNER", IS_PARTNER_INTEGER.toString())
            .build();
    }

    @Nonnull
    public static PersonStructure createPersonRequest() {
        PersonStructure person = new PersonStructure();
        person.setClientId(CLIENT_ID.toString());
        person.setType(ORG_TYPE);
        person.setName(ORG_NAME);
        person.setLongName(ORG_LONG_NAME);
        person.setPhone(PHONE);
        person.setEmail(EMAIL);
        person.setPostCode(POST_CODE);
        person.setPostAddress(POST_ADDRESS);
        person.setLegalAddress(LEGAL_ADDRESS);
        person.setInn(INN);
        person.setBik(BIK);
        person.setAccount(ACCOUNT);
        person.setKpp(KPP);
        person.setIsPartner(IS_PARTNER_BOOLEAN);
        return person;
    }

    @Nonnull
    public static PersonStructure updatePersonRequest() {
        PersonStructure person = new PersonStructure();
        person.setClientId(CLIENT_ID.toString());
        person.setPersonId(PERSON_ID.toString());
        person.setType(ORG_TYPE);
        person.setName(ORG_NAME);
        person.setLongName(ORG_LONG_NAME);
        person.setPhone(PHONE);
        person.setEmail(EMAIL);
        person.setPostCode(POST_CODE);
        person.setPostAddress(POST_ADDRESS);
        person.setLegalAddress(LEGAL_ADDRESS);
        person.setInn(INN);
        person.setBik(BIK);
        person.setAccount(ACCOUNT);
        person.setKpp(KPP);
        person.setIsPartner(IS_PARTNER_BOOLEAN);
        return person;
    }

    @Nonnull
    public static PersonInfo createdPerson() {
        return PersonInfo.builder()
            .clientId(CLIENT_ID)
            .type(PersonInfo.Type.UR)
            .name(ORG_NAME)
            .longName(ORG_LONG_NAME)
            .phone(PHONE)
            .email(EMAIL)
            .postCode(POST_CODE)
            .postAddress(POST_ADDRESS)
            .legalAddress(LEGAL_ADDRESS)
            .inn(INN)
            .bik(BIK)
            .account(ACCOUNT)
            .kpp(KPP)
            .isPartner(IS_PARTNER_BOOLEAN)
            .build();
    }

    @Nonnull
    public static PersonInfo person() {
        return PersonInfo.builder()
            .id(PERSON_ID)
            .personId(PERSON_ID)
            .clientId(CLIENT_ID)
            .type(PersonInfo.Type.UR)
            .name(ORG_NAME)
            .longName(ORG_LONG_NAME)
            .phone(PHONE)
            .email(EMAIL)
            .postCode(POST_CODE)
            .postAddress(POST_ADDRESS)
            .legalAddress(LEGAL_ADDRESS)
            .inn(INN)
            .bik(BIK)
            .account(ACCOUNT)
            .kpp(KPP)
            .isPartner(IS_PARTNER_BOOLEAN)
            .build();
    }

    @Nonnull
    public static LinkIntegrationToClientStructure linkIntegrationToClientStructure() {
        LinkIntegrationToClientStructure linkIntegrationToClientStructure = new LinkIntegrationToClientStructure();
        linkIntegrationToClientStructure.setClientId(CLIENT_ID);
        linkIntegrationToClientStructure.setConfigurationCc("market_logistics_partner_default_conf");
        linkIntegrationToClientStructure.setIntegrationCc("market_logistics_partner");
        return linkIntegrationToClientStructure;
    }

    @Nonnull
    public static LinkIntegrationToClientInfo linkIntegrationToClientInfo() {
        return LinkIntegrationToClientInfo.builder()
            .clientId(CLIENT_ID)
            .configurationCc("market_logistics_partner_default_conf")
            .integrationCc("market_logistics_partner")
            .build();
    }

    @Nonnull
    public static Map<String, Object> createOfferResponse() {
        return Map.of(
            "ID", OFFER_ID,
            "EXTERNAL_ID", OFFER_EXTERNAL_ID
        );
    }

    @Nonnull
    public static OfferInfo offerInfo() {
        return OfferInfo.builder()
            .id(OFFER_ID)
            .externalId(OFFER_EXTERNAL_ID)
            .build();
    }

    @Nonnull
    public static CreateOfferStructure createOfferStructure() {
        return createOfferStructure(START_DATE);
    }

    @Nonnull
    public static CreateOfferStructure createOfferStructure(Date date) {
        return new CreateOfferStructure()
            .setClientId(CLIENT_ID)
            .setCurrency(CURRENCY)
            .setFirmId(FIRM_ID)
            .setIntegration(INTEGRATION)
            .setManagerUid(MANAGER_UID)
            .setNds(NDS)
            .setPersonId(PERSON_ID)
            .setServices(SERVICES)
            .isSigned(IS_SIGNED)
            .setStartDt(date);
    }

    @Nonnull
    public static CreateOfferInfo createOfferInfo() {
        return CreateOfferInfo.builder()
            .clientId(CLIENT_ID)
            .currency(CURRENCY)
            .firmId(FIRM_ID)
            .integration(INTEGRATION)
            .managerUid(MANAGER_UID)
            .nds(NDS)
            .personId(PERSON_ID)
            .services(SERVICES)
            .signed(IS_SIGNED)
            .startDate(START_DATE)
            .build();

    }

    @Nonnull
    public static UpdateOfferInfo updateOfferInfo() {
        return UpdateOfferInfo.builder().nds(0).build();
    }

    @Nonnull
    public static UpdateOfferStructure updateOfferStructure() {
        return new UpdateOfferStructure().setNds(0);
    }
}
