package ru.yandex.market.checkout.checkouter.delivery;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.order.Platform;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.test.providers.AddressProvider;


public class DeliveryValidatorTest {

    private DeliveryValidator deliveryValidator;

    private final PersonalDataService personalDataService = Mockito.mock(PersonalDataService.class);

    public static Stream<Arguments> parameterizedTestData() {
        AddressImpl addressBigCity = new AddressImpl();
        addressBigCity.setCountry("Русь");
        addressBigCity.setPostcode("131488");
        addressBigCity.setCity("посёлок городского типа Томилино, Московская область");
        addressBigCity.setSubway("Петровско-Разумовская");
        addressBigCity.setStreet("Победы");
        addressBigCity.setHouse("13");
        addressBigCity.setBuilding("222");
        addressBigCity.setBlock("666");
        addressBigCity.setEntrance("404");
        addressBigCity.setEntryPhone("007");
        addressBigCity.setFloor("8");
        addressBigCity.setApartment("303");
        addressBigCity.setRecipient("000");
        addressBigCity.setPhone("02");
        addressBigCity.setLanguage(AddressLanguage.RUS);
        return Arrays.stream(new Object[][]{
                {
                        AddressProvider.getAddress(),
                        DeliveryType.PICKUP,
                        false,
                        false,
                },
                {
                        AddressProvider.getAnotherAddress(),
                        DeliveryType.PICKUP,
                        false,
                        false,
                },
                {
                        addressBigCity,
                        DeliveryType.PICKUP,
                        false,
                        false,
                },
                {
                        AddressProvider.getAddressWithoutPostcode(),
                        DeliveryType.PICKUP,
                        false,
                        false,
                },
                {
                        AddressProvider.getEnglishAddress(),
                        DeliveryType.PICKUP,
                        false,
                        false,
                },

                {
                        AddressProvider.getAddressBigHouseField(),
                        DeliveryType.PICKUP,
                        false,
                        false,
                },
        }).map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        deliveryValidator = new DeliveryValidator();

        deliveryValidator.setPersonalDataService(personalDataService);
    }


    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void validateAddressTest(Address address, DeliveryType deliveryType, boolean optional, boolean fullAddress) {
        Mockito.when(personalDataService.retrieve(Mockito.any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null,
                        PersAddress.convertToPersonal(address), null));

        Mockito.when(personalDataService.getPersAddress(Mockito.any()))
                .thenReturn(PersAddress.convertToPersonal(address));

        deliveryValidator.validateAddress(address, deliveryType, optional, fullAddress, Platform.UNKNOWN);
    }
}
