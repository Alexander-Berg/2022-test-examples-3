package ru.yandex.market.tpl.common.personal.client.tpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.common.personal.client.HasPersonalAddress;
import ru.yandex.market.tpl.common.personal.client.HasPersonalData;
import ru.yandex.market.tpl.common.personal.client.HasPersonalEmail;
import ru.yandex.market.tpl.common.personal.client.HasPersonalFio;
import ru.yandex.market.tpl.common.personal.client.HasPersonalGpsCoords;
import ru.yandex.market.tpl.common.personal.client.HasPersonalPhone;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalFindApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalStoreApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveRequestItem;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalAddressKeys;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveRequest;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveResponse;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@ContextConfiguration(classes = {EnrichPersonalDataService.class, PersonalExternalService.class})
@ExtendWith(SpringExtension.class)
class EnrichPersonalDataServiceTest {

    @Autowired
    private EnrichPersonalDataService enrichPersonalDataService;
    @MockBean
    private DefaultPersonalRetrieveApi personalRetrieveApi;
    @MockBean
    private DefaultPersonalStoreApi personalStoreApi;
    @MockBean
    private DefaultPersonalFindApi personalFindApi;

    @Test
    @DisplayName("Проверка, что персональные данные верно наполняются, использую multi type механизм")
    void personalDataCorrectlyEnrich_MultiType() {
        //given
        PersonalMultiTypeRetrieveRequest request = new PersonalMultiTypeRetrieveRequest().items(
                List.of(new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.EMAIL).id("4321"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.PHONE).id("1234"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.FULL_NAME).id("5678"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.ADDRESS).id("90"),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.GPS_COORD).id("09"))
        );
        PersonalMultiTypeRetrieveResponse response = new PersonalMultiTypeRetrieveResponse().items(
                List.of(new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).id("1234")
                                .value(new CommonType().phone("+71112223344")),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.EMAIL).id("4321")
                                .value(new CommonType().email("some@mail.ru")),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.FULL_NAME).id("5678")
                                .value(new CommonType().fullName(new FullName().forename("Василий").surname("Пупкин"))),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.ADDRESS).id("90")
                                .value(new CommonType().address(
                                        Map.of(
                                                "test", "test",
                                                PersonalAddressKeys.LOCALITY.getName(), "city",
                                                PersonalAddressKeys.STREET.getName(), "street",
                                                PersonalAddressKeys.HOUSE.getName(), "house",
                                                PersonalAddressKeys.BUILDING.getName(), "building",
                                                PersonalAddressKeys.HOUSING.getName(), "housing",
                                                PersonalAddressKeys.ROOM.getName(), "room",
                                                PersonalAddressKeys.FLOOR.getName(), "floor",
                                                PersonalAddressKeys.PORCH.getName(), "porch",
                                                PersonalAddressKeys.INTERCOM.getName(), "intercom"
                                        )
                                )),
                        new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.GPS_COORD).id("09")
                                .value(new CommonType().gpsCoord(
                                        new GpsCoord().latitude(BigDecimal.valueOf(12)).longitude(BigDecimal.valueOf(11))
                                        ))
                )
        );
        Mockito.when(personalRetrieveApi.v1MultiTypesRetrievePost(request)).thenReturn(response);
        List<TestHasPersonalDataFields> items = new ArrayList<>();
        TestHasPersonalDataFields item = new TestHasPersonalDataFields("1234", "4321", "5678", "90", "09");
        items.add(item);
        TestHasPersonalData testHasPersonalData = new TestHasPersonalData(items);

        //when
        enrichPersonalDataService.enrichNotAllowTransaction(testHasPersonalData, true);

        //then
        TestHasPersonalDataFields updatedItem = testHasPersonalData.listOfData.get(0);
        assertThat(updatedItem.recipientEmail).isEqualTo(response.getItems().get(1).getValue().getEmail());
        assertThat(updatedItem.recipientPhone).isEqualTo(response.getItems().get(0).getValue().getPhone());
        assertThat(updatedItem.recipientFio).isEqualTo("Пупкин Василий");
        assertThat(updatedItem.recipientFullName).isEqualTo(response.getItems().get(2).getValue().getFullName());
        assertThat(updatedItem.address).isEqualTo("г. city, ул. street, д. house");
        assertThat(updatedItem.city).isEqualTo("city");
        assertThat(updatedItem.street).isEqualTo("street");
        assertThat(updatedItem.latitude).isEqualTo(response.getItems().get(4).getValue().getGpsCoord().getLatitude());
        assertThat(updatedItem.longitude).isEqualTo(response.getItems().get(4).getValue().getGpsCoord().getLongitude());
    }

    @AllArgsConstructor
    private class TestHasPersonalData implements HasPersonalData {

        private List<TestHasPersonalDataFields> listOfData = new ArrayList<>();

        @Override
        public <T> StreamEx<T> streamHolders(Function<HasPersonalData, Stream<T>> streamHolderSupplier,
                                             Class<T> availableClass) {
            return StreamEx.of(listOfData).select(availableClass);
        }

    }

    @Setter
    public class TestHasPersonalDataFields implements HasPersonalPhone, HasPersonalEmail, HasPersonalFio,
            HasPersonalAddress, HasPersonalGpsCoords {

        private String recipientPhone;
        private String recipientEmail;
        private String recipientFio;
        private FullName recipientFullName;
        private String address;
        private String city;
        private String street;
        private BigDecimal latitude;
        private BigDecimal longitude;
        @Getter
        private String personalPhoneId;
        @Getter
        private String personalEmailId;
        @Getter
        private String personalFioId;
        @Getter
        private String personalAddressId;
        @Getter
        private String personalGpsId;

        public TestHasPersonalDataFields(String phoneId, String emailId, String fioId, String addressId, String gpsId) {
            this.personalPhoneId = phoneId;
            this.personalEmailId = emailId;
            this.personalFioId = fioId;
            this.personalAddressId = addressId;
            this.personalGpsId = gpsId;
        }

        @Override
        public void setRecipientFio(FullName fio) {
            this.recipientFullName = fio;
        }

        @Override
        public void setRecipientFio(String fio) {
            this.recipientFio = fio;
        }

        @Override
        public void setCity(String city) {
            this.city = city;
        }

        @Override
        public void setStreet(String street) {
            this.street = street;
        }

        @Override
        public List<PersonalAddressKeys> getAvailableAddressKeys() {
            return List.of(
                    PersonalAddressKeys.LOCALITY,
                    PersonalAddressKeys.STREET,
                    PersonalAddressKeys.HOUSE
            );
        }

        @Override
        public void setGpsCoords(GpsCoord gpsCoord) {
            this.latitude = gpsCoord.getLatitude();
            this.longitude = gpsCoord.getLongitude();
        }
    }
}
