package ru.yandex.market.delivery.transport_manager.service.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.dto.AddZperDto;
import ru.yandex.market.delivery.transport_manager.exception.ResourceNotFoundException;
import ru.yandex.market.delivery.transport_manager.service.request.AdminAddZperService;

public class AdminAddZperServiceTest extends AbstractContextualTest {

    @Autowired
    AdminAddZperService adminAddZperService;


    @Test
    @DatabaseSetup("/repository/add_zper/transportation_with_zp.xml")
    @ExpectedDatabase(
        value = "/repository/add_zper/transportation_with_zp_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkHappyPathZp() {
        var validZp = "Зп-123456";
        var dummyZper = "ЗПер123123";

        var addZperDto = createAddZperDto(validZp, dummyZper, null);

        adminAddZperService.addZper(addZperDto);
    }

    @Test
    @DatabaseSetup("/repository/add_zper/transportation_with_wms.xml")
    @ExpectedDatabase(
        value = "/repository/add_zper/transportation_with_wms_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkHappyPathWms() {
        var validWms = "0000123456";
        var dummyZper = "ЗПер123123";

        var addZperDto = createAddZperDto(null, dummyZper, validWms);

        adminAddZperService.addZper(addZperDto);
    }

    @Test
    void checkWmsAndZpAreNull() {
        var dummyZper = "ЗПер123123";
        var addZperDto = createAddZperDto(null, dummyZper, null);

        IllegalStateException exception =
            Assertions.assertThrows(IllegalStateException.class, () -> adminAddZperService.addZper(addZperDto));
        Assertions.assertEquals(exception.getMessage(), "Зп и WMS id не могут быть пустыми");
    }

    @Test
    void checkWmsAndZpAreBothNotNull() {
        var dummyZper = "ЗПер123123";
        var validWms = "0000123456";
        var validZp = "Зп-123456";
        var addZperDto = createAddZperDto(validZp, dummyZper, validWms);

        IllegalStateException exception =
            Assertions.assertThrows(IllegalStateException.class, () -> adminAddZperService.addZper(addZperDto));
        Assertions.assertEquals(exception.getMessage(), "Зп и WMS id не могут быть заполнены одновременно");
    }

    @Test
    void checkWmsIsInvalid() {
        var invalidWms = "a0000123456";
        var dummyZper = "ЗПер123123";
        var addZperDto = createAddZperDto(null, dummyZper, invalidWms);

        IllegalStateException exception =
            Assertions.assertThrows(IllegalStateException.class, () -> adminAddZperService.addZper(addZperDto));
        Assertions.assertEquals(exception.getMessage(), "Неправильно заполнен WMS id. Пример: 00001234");
    }

    @Test
    void checkZpIsInvalid() {
        var validZp = "Зп3456";
        var dummyZper = "ЗПер123123";

        var addZperDto = createAddZperDto(validZp, dummyZper, null);

        IllegalStateException exception =
            Assertions.assertThrows(IllegalStateException.class, () -> adminAddZperService.addZper(addZperDto));
        Assertions.assertEquals(exception.getMessage(), "Неправильно заполнен Зп. Пример: Зп-01234567");
    }


    @Test
    void checkZperIsInvalid() {
        var validZp = "Зп-3456";
        var dummyZper = "ЗПер123123asd";

        var addZperDto = createAddZperDto(validZp, dummyZper, null);

        IllegalStateException exception =
            Assertions.assertThrows(IllegalStateException.class, () -> adminAddZperService.addZper(addZperDto));
        Assertions.assertEquals(exception.getMessage(), "Неправильно заполнен ЗПер. Пример: ЗПер01234567");
    }

    @Test
    @DatabaseSetup("/repository/add_zper/transportation_with_zp.xml")
    void checkTransportationNotFound() {
        var validZp = "Зп-666";
        var dummyZper = "ЗПер123123";

        var addZperDto = createAddZperDto(validZp, dummyZper, null);

        ResourceNotFoundException exception =
            Assertions.assertThrows(ResourceNotFoundException.class, () -> adminAddZperService.addZper(addZperDto));
        Assertions.assertEquals(exception.getMessage(), "Failed to find [TRANSPORTATION] with ids [[Зп-666]]");
    }

    @Test
    @DatabaseSetup("/repository/add_zper/transportations_with_same_zp.xml")
    void checkMoreThanOneTransportationsFound() {
        var validZp = "Зп-123456";
        var dummyZper = "ЗПер123123";

        var addZperDto = createAddZperDto(validZp, dummyZper, null);

        IllegalStateException exception =
            Assertions.assertThrows(IllegalStateException.class, () -> adminAddZperService.addZper(addZperDto));
        Assertions.assertEquals(exception.getMessage(), "Найдено больше одного перемещения с зп - Зп-123456");
    }

    @Test
    @DatabaseSetup("/repository/add_zper/several_transporations_happy_path.xml")
    @ExpectedDatabase(
        value = "/repository/add_zper/several_transprotations_happy_path_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkThatNeededTransportationChoosed() {
        var validZp = "Зп-123456";
        var dummyZper = "ЗПер123123";

        var addZperDto = createAddZperDto(validZp, dummyZper, null);

        adminAddZperService.addZper(addZperDto);
    }

    private AddZperDto createAddZperDto(String zp, String zper, String wms) {
        var res = new AddZperDto();
        res.setZpTextField(zp);
        res.setZperTextField(zper);
        res.setWmsTextField(wms);
        return res;
    }
}
