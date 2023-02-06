package ru.yandex.market.checkout.checkouter.service.personal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.feature.CachedService;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.service.personal.model.FullName;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersonalMultiTypeRetrieveRequest;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersonalRetrieveRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_ADDRESS_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_EMAIL_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_FULL_NAME_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_GPS_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;

public class CachedPersonalDataServiceTest extends AbstractServicesTestBase {

    @Autowired
    protected PersonalDataService personalDataService;

    @Autowired
    @Qualifier("personalDataService")
    private CachedService cachedPersonalDataService;

    @SpyBean
    private PersonalClient personalClient;

    @BeforeEach
    public void beforeEach() {
        cachedPersonalDataService.invalidateAll();
    }

    @AfterEach
    public void afterEach() {
        cachedPersonalDataService.invalidateAll();
    }

    @Test
    public void getBuyerPhoneSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        personalMockConfigurer.mockV1PhonesRetrieve();
        Buyer buyer = EntityHelper.getBuyer();

        String buyerPhone = personalDataService.getBuyerPhone(buyer);

        assertEquals("+74952234562", buyerPhone);

        //cache test
        String buyerPhone2 = personalDataService.getBuyerPhone(buyer);
        assertEquals("+74952234562", buyerPhone2);
        Mockito.verify(personalClient, Mockito.times(1))
                .retrieve(Mockito.any(), Mockito.any(PersonalRetrieveRequest.class));
    }

    @Test
    public void getBuyerPhoneFailNotFound() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        personalMockConfigurer.mockV1PhonesRetrieveNotFound();
        Buyer buyer = EntityHelper.getBuyer();

        assertThrows(HttpClientErrorException.NotFound.class,
                () -> personalDataService.getBuyerPhone(buyer)
        );
    }

    /**
     * Удалить в MARKETCHECKOUT-27094
     */
    @Test
    public void getBuyerPhoneFallbackSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        personalMockConfigurer.mockUnavailable(502);
        Buyer buyer = EntityHelper.getBuyer();

        String buyerPhone = personalDataService.getBuyerPhone(buyer);

        assertEquals(buyer.getPhone(), buyerPhone);
    }

    /**
     * Удалить в MARKETCHECKOUT-27094
     */
    @Test
    public void getBuyerPhoneDontRequestPersonalSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);

        Buyer buyer = EntityHelper.getBuyer();
        buyer.setPersonalPhoneId(null);

        String buyerPhone = personalDataService.getBuyerPhone(buyer);

        assertEquals(buyer.getPhone(), buyerPhone);
        personalMockConfigurer.verifyNoRequests();
    }

    @Test
    public void getBuyerEmailSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, true);
        personalMockConfigurer.mockV1EmailsRetrieve();
        Buyer buyer = EntityHelper.getBuyer();

        String buyerEmail = personalDataService.getBuyerEmail(buyer);

        assertEquals("asd2@gmail.com", buyerEmail);

        //cache test
        String buyerEmail2 = personalDataService.getBuyerEmail(buyer);
        assertEquals("asd2@gmail.com", buyerEmail2);
        Mockito.verify(personalClient, Mockito.times(1))
                .retrieve(Mockito.any(), Mockito.any(PersonalRetrieveRequest.class));
    }

    @Test
    public void getBuyerEmailFailNotFound() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, true);
        personalMockConfigurer.mockV1EmailsRetrieveNotFound();
        Buyer buyer = EntityHelper.getBuyer();

        assertThrows(HttpClientErrorException.NotFound.class,
                () -> personalDataService.getBuyerEmail(buyer)
        );
    }

    /**
     * Удалить в MARKETCHECKOUT-27094
     */
    @Test
    public void getBuyerEmailFallbackSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, false);
        personalMockConfigurer.mockUnavailable(502);
        Buyer buyer = EntityHelper.getBuyer();

        String buyerEmail = personalDataService.getBuyerEmail(buyer);

        assertEquals(buyer.getEmail(), buyerEmail);
    }

    /**
     * Удалить в MARKETCHECKOUT-27094
     */
    @Test
    public void getBuyerEmailDontRequestPersonalSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, false);

        Buyer buyer = EntityHelper.getBuyer();
        buyer.setPersonalEmailId(null);

        String buyerEmail = personalDataService.getBuyerEmail(buyer);

        assertEquals(buyer.getEmail(), buyerEmail);
        personalMockConfigurer.verifyNoRequests();
    }

    @Test
    public void storeSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_ADDRESS_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_GPS_ID, true);
        personalMockConfigurer.mockV1MultiTypesStore();

        PersAddress address = new PersAddress();
        address.setCountry("Russia");
        address.setStreet("Vavilova");

        PersGps persGps = new PersGps();
        persGps.setLatitude("123.45");
        persGps.setLongitude("67.89");

        PersonalDataStoreRequestBuilder requestBuilder = new PersonalDataStoreRequestBuilder()
                .withFullName("Leo", null, "Tolstoy")
                .withPhone("+71234567891")
                .withEmail("a@b.com")
                .withAddress(address)
                .withGps(persGps);

        PersonalDataStoreResult result = personalDataService.store(requestBuilder);

        assertEquals("81e33d098f095f67b1622ccde7a4a5b4", result.getFullNameId());
        assertEquals("0123456789abcdef0123456789abcdef", result.getPhoneId());
        assertEquals("4621897c54fd9ef81e33c0502bd6ab7a", result.getEmailId());
        assertEquals("fgdfg43fc343x23w2c5345w", result.getAddressId());
        assertEquals("mgflk5jng5erfnjerfmk3n", result.getGpsId());

        //cache test
        PersonalDataRetrieveRequestBuilder requestBuilder2 = PersonalDataRetrieveRequestBuilder
                .create()
                .withFullNameId("81e33d098f095f67b1622ccde7a4a5b4")
                .withPhoneId("0123456789abcdef0123456789abcdef")
                .withEmailId("4621897c54fd9ef81e33c0502bd6ab7a")
                .withAddressId("fgdfg43fc343x23w2c5345w")
                .withGpsId("mgflk5jng5erfnjerfmk3n");

        var result2 = personalDataService.retrieve(requestBuilder2);
        assertEquals("Leo", result2.getFullName().getForename());
        assertNull(result2.getFullName().getPatronymic());
        assertEquals("Tolstoy", result2.getFullName().getSurname());
        assertEquals("+71234567891", result2.getPhone());
        assertEquals("a@b.com", result2.getEmail());

        PersAddress address2 = result2.getAddress();
        assertNotNull(address2);
        assertEquals("Russia", address2.getCountry());
        assertEquals("Vavilova", address2.getStreet());

        PersGps gps = result2.getGps();
        assertNotNull(gps);
        assertEquals("123.45", gps.getLatitude());
        assertEquals("67.89", gps.getLongitude());

        Mockito.verify(personalClient, Mockito.never())
                .retrieveMultiTypes(Mockito.any(PersonalMultiTypeRetrieveRequest.class));
    }

    private static void checkRetrieveResult(PersonalDataRetrieveResult result) {
        assertEquals("Leo", result.getFullName().getForename());
        assertNull(result.getFullName().getPatronymic());
        assertEquals("Tolstoy", result.getFullName().getSurname());
        assertEquals("+71234567891", result.getPhone());
        assertEquals("a@b.com", result.getEmail());

        PersAddress address = result.getAddress();
        assertNotNull(address);
        assertEquals("Russia", address.getCountry());
        assertEquals("347660", address.getPostcode());
        assertEquals("Moscow", address.getCity());
        assertEquals("left", address.getDistrict());
        assertEquals("13", address.getSubway());
        assertEquals("Vavilova", address.getStreet());
        assertEquals("35", address.getKm());
        assertEquals("3/6", address.getHouse());
        assertEquals("9", address.getBuilding());
        assertEquals("3", address.getEstate());
        assertEquals("2", address.getBlock());
        assertEquals("4", address.getEntrance());
        assertEquals("331", address.getEntryPhone());
        assertEquals("8", address.getFloor());
        assertEquals("43", address.getApartment());

        PersGps gps = result.getGps();
        assertNotNull(gps);
        assertEquals("123.45", gps.getLatitude());
        assertEquals("67.8759", gps.getLongitude());
    }

    @Test
    public void storeInvalidPhoneFailure() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        personalMockConfigurer.mockV1MultiTypesStoreInvalidNumber();

        PersonalDataStoreRequestBuilder requestBuilder = new PersonalDataStoreRequestBuilder()
                .withPhone("+71234567891");

        assertThrows(PersonalServiceException.class,
                () -> personalDataService.store(requestBuilder)
        );
    }

    /**
     * Удалить в MARKETCHECKOUT-27942
     */
    @Test
    public void storeFallbackSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        personalMockConfigurer.mockUnavailable(502);

        PersonalDataStoreRequestBuilder requestBuilder = new PersonalDataStoreRequestBuilder()
                .withPhone("+71234567891");

        PersonalDataStoreResult result = personalDataService.store(requestBuilder);

        assertNull(result.getPhoneId());
    }

    @Test
    public void retrieveSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_ADDRESS_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_GPS_ID, true);
        personalMockConfigurer.mockV1MultiTypesRetrieve();

        PersonalDataRetrieveRequestBuilder requestBuilder = PersonalDataRetrieveRequestBuilder
                .create()
                .withFullNameId("81e33d098f095f67b1622ccde7a4a5b4")
                .withPhoneId("0123456789abcdef0123456789abcdef")
                .withEmailId("4621897c54fd9ef81e33c0502bd6ab7a")
                .withAddressId("fgdfg43fc343x23w2c5345w3")
                .withGpsId("mgflk5jng5erfnjerfmk3n4");

        PersonalDataRetrieveResult result = personalDataService.retrieve(requestBuilder);
        checkRetrieveResult(result);

        //cache test
        requestBuilder = PersonalDataRetrieveRequestBuilder
                .create()
                .withFullNameId("81e33d098f095f67b1622ccde7a4a5b4")
                .withPhoneId("0123456789abcdef0123456789abcdef")
                .withEmailId("4621897c54fd9ef81e33c0502bd6ab7a")
                .withAddressId("fgdfg43fc343x23w2c5345w3")
                .withGpsId("mgflk5jng5erfnjerfmk3n4");
        PersonalDataRetrieveResult result2 = personalDataService.retrieve(requestBuilder);
        checkRetrieveResult(result2);
        Mockito.verify(personalClient, Mockito.times(1))
                .retrieveMultiTypes(Mockito.any(PersonalMultiTypeRetrieveRequest.class));
    }

    @Test
    public void retrieveNotFoundFailure() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        personalMockConfigurer.mockV1MultiTypesRetrieve();

        PersonalDataRetrieveRequestBuilder requestBuilder = PersonalDataRetrieveRequestBuilder
                .create()
                .withPhoneId("ffffffffffffffffffffffffffffffff");

        assertThrows(PersonalServiceException.class,
                () -> personalDataService.retrieve(requestBuilder)
        );
    }

    /**
     * Удалить в MARKETCHECKOUT-27942
     */
    @Test
    public void retrieveNoPersonalIdFallbackSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);

        PersAddress persAddress = new PersAddress();

        persAddress.setCountry("Russia");
        persAddress.setPostcode("347660");
        persAddress.setCity("Moscow");
        persAddress.setDistrict("left");
        persAddress.setSubway("13");
        persAddress.setStreet("Vavilova");
        persAddress.setKm("35");
        persAddress.setHouse("3/6");
        persAddress.setBuilding("9");
        persAddress.setEstate("3");
        persAddress.setBlock("2");
        persAddress.setEntrance("4");
        persAddress.setEntryPhone("331");
        persAddress.setFloor("8");
        persAddress.setApartment("43");

        PersGps persGps = new PersGps();
        persGps.setLatitude("123.45");
        persGps.setLongitude("67.8759");

        PersonalDataRetrieveRequestBuilder requestBuilder = PersonalDataRetrieveRequestBuilder
                .create()
                .withFullNameId(null, new FullName().forename("Leo").patronymic("N").surname("Tolstoy"))
                .withPhoneId(null, "+71234567891")
                .withEmailId(null, "a@b.com")
                .withAddressId(null, persAddress)
                .withGpsId(null, persGps);

        PersonalDataRetrieveResult result = personalDataService.retrieve(requestBuilder);

        assertEquals("Leo", result.getFullName().getForename());
        assertEquals("N", result.getFullName().getPatronymic());
        assertEquals("Tolstoy", result.getFullName().getSurname());
        assertEquals("+71234567891", result.getPhone());
        assertEquals("a@b.com", result.getEmail());
        assertEquals(persAddress, result.getAddress());
        assertEquals(persGps, result.getGps());
        personalMockConfigurer.verifyNoRequests();
    }

    /**
     * Удалить в MARKETCHECKOUT-27942
     */
    @Test
    public void retrieveFallbackSuccess() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
        personalMockConfigurer.mockUnavailable(502);

        PersonalDataRetrieveRequestBuilder requestBuilder = PersonalDataRetrieveRequestBuilder
                .create()
                .withPhoneId("0123456789abcdef0123456789abcdef", "+71234567891");

        PersonalDataRetrieveResult result = personalDataService.retrieve(requestBuilder);

        assertEquals("+71234567891", result.getPhone());
    }

    @Test
    public void checkUsePersonalToggle() {
        checkouterFeatureWriter.writeValue(USE_PERSONAL, false);

        PersonalDataRetrieveRequestBuilder requestBuilder = PersonalDataRetrieveRequestBuilder
                .create()
                .withPhoneId("0123456789abcdef0123456789abcdef", "+71234567891");

        PersonalDataRetrieveResult result = personalDataService.retrieve(requestBuilder);

        assertEquals("+71234567891", result.getPhone());
    }
}
