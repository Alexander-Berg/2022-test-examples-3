package ru.yandex.market.checkout.checkouter.delivery;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.storage.AddressDao;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeliveryAddressTest extends AbstractWebTestBase {

    @Autowired
    private AddressDao addressDao;

    /**
     * Тест проверяет, что в определение equals адреса попадают только те поля,
     * которые сохраняются и читаются из БД
     * При ином поведении нарушается логика сохранения адреса при перезаписи доставки
     * <p>
     * Тест отрабатывает при условии, что все поля адреса заполнены для
     * правильной работы при добавлении новых полей
     */
    @Test
    public void shouldIncludeInEqualityCheckOnlyStoredFields() {
        Address address = createAddress();
        for (Field field : AddressImpl.class.getDeclaredFields()) {
            try {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), AddressImpl.class);
                Object fieldValue = propertyDescriptor.getReadMethod().invoke(address);
                assertNotNull(fieldValue, () -> String.format("Field \"%s\" must not be null for test",
                        field.getName()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        Long addressId = transactionTemplate
                .execute(tc -> addressDao.insertAddress(address, order.getId()));
        Address storedAddress = addressDao.findOne(addressId);

        assertEquals(address, storedAddress);
    }

    private Address createAddress() {
        RecipientPerson recipientPerson = new RecipientPerson(
                "Leo",
                "Nikolayevich",
                "Tolstoy");
        Recipient recipient = new Recipient(
                recipientPerson,
                "cf637afe00d8be951c245162a93b7d84",
                "+71234567891",
                "0123456789abcdef0123456789abcdef",
                "leo@ya.ru",
                "fedcba9876543210fedcba9876543210");
        BusinessRecipient businessRecipient = new BusinessRecipient();
        businessRecipient.setInn("781234567");
        businessRecipient.setKpp("1234567");
        businessRecipient.setName("ООО Ромашка");
        AddressImpl address = new AddressImpl();
        address.setCountry("Русь");
        address.setPostcode("131488");
        address.setCity("Питер");
        address.setDistrict("Московский район");
        address.setSubway("Петровско-Разумовская");
        address.setStreet("Победы");
        address.setHouse("13");
        address.setBuilding("222");
        address.setEstate("111");
        address.setBlock("666");
        address.setEntrance("404");
        address.setEntryPhone("007");
        address.setFloor("8");
        address.setApartment("303");
        address.setPersonalAddressId("nn4n2nlj3n3d2jnc3");
        address.setGps("59.860360, 30.319488");
        address.setPersonalGpsId("jdnn3n32n3x3");
        address.setNotes("Звонок в дверь не работает");
        address.setRecipient(recipient.getPerson().getFormattedName());
        address.setPersonalFullNameId(recipient.getPersonalFullNameId());
        address.setPhone(recipient.getPhone());
        address.setPersonalPhoneId(recipient.getPersonalPhoneId());
        address.setScheduleString("11-12");
        address.setCalendarHolidaysString("XmlString");
        address.setRecipientEmail(recipient.getEmail());
        address.setPersonalEmailId(recipient.getPersonalEmailId());
        address.setRecipientPerson(recipient.getPerson());
        address.setLanguage(AddressLanguage.RUS);
        address.setOutletName("Планета здоровья");
        address.setAddressSource(AddressSource.NEW);
        address.setType(AddressType.SHOP);
        address.setPreciseRegionId(2L);
        address.setRecipientPerson(recipientPerson);
        address.setOutletName("ПВЗ СкажиКурьерамНет");
        address.setOutletPhones(new String[]{"+78125553535"});
        address.setYandexMapPermalink("-");
        address.setBusinessRecipient(businessRecipient);
        address.setKm("1");
        return address;
    }
}
