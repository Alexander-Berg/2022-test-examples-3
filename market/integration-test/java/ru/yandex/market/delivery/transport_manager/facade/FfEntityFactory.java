package ru.yandex.market.delivery.transport_manager.facade;

import java.util.List;

import ru.yandex.market.ff.client.dto.CarDTO;
import ru.yandex.market.ff.client.dto.ClientResourceIdDTO;
import ru.yandex.market.ff.client.dto.CourierDTO;
import ru.yandex.market.ff.client.dto.LegalEntityDTO;
import ru.yandex.market.ff.client.dto.PartyDTO;
import ru.yandex.market.ff.client.dto.PersonDTO;
import ru.yandex.market.ff.client.dto.PhoneDTO;
import ru.yandex.market.ff.client.enums.LegalFormType;

public class FfEntityFactory {

    private FfEntityFactory() {
        throw new UnsupportedOperationException();
    }

    static CourierDTO createCourier() {
        var courierDto = new CourierDTO();
        var personDto = new PersonDTO();
        personDto.setName("Олег");
        personDto.setSurname("Егоров");
        personDto.setPatronymic("Васильевич");
        var carDto = new CarDTO();
        carDto.setDescription("Белый форд транзит");
        carDto.setNumber("О123НО790");
        courierDto.setPersons(List.of(
            personDto
        ));
        courierDto.setCar(carDto);
        var legalEntity = new LegalEntityDTO();
        legalEntity.setLegalName("ИП Mover");
        legalEntity.setName("Mover");
        legalEntity.setOgrn("12345678");
        legalEntity.setInn("1234567890");
        legalEntity.setLegalForm(LegalFormType.IP);
        courierDto.setMarketId(15L);
        courierDto.setLegalEntity(legalEntity);
        ClientResourceIdDTO clientResourceIdDTO = new ClientResourceIdDTO();
        clientResourceIdDTO.setPartnerId("default");
        clientResourceIdDTO.setYandexId("7");
        courierDto.setPartnerId(clientResourceIdDTO);
        var phone = new PhoneDTO();
        phone.setPhoneNumber("+7(904) 444-44-44");
        courierDto.setPhone(phone);
        return courierDto;
    }

    static LegalEntityDTO createInbounder() {
        var legalInfo = new LegalEntityDTO();
        legalInfo.setName("Inbounder");
        legalInfo.setLegalName("ИП Inbounder");
        legalInfo.setLegalForm(LegalFormType.IP);
        legalInfo.setOgrn("12345778");
        legalInfo.setInn("1234565890");
        return legalInfo;
    };

    static PartyDTO createReceiver() {
        PartyDTO partyDTO = new PartyDTO();
        partyDTO.setLegalEntity(createInbounder());
        partyDTO.setLogisticsPointId(2L);
        return partyDTO;
    }

    static PartyDTO createShipper() {
        PartyDTO partyDTO = new PartyDTO();
        partyDTO.setLegalEntity(createOutbounder());
        partyDTO.setLogisticsPointId(2L);
        return partyDTO;
    }

    private static LegalEntityDTO createOutbounder() {
        var legalInfo = new LegalEntityDTO();
        legalInfo.setName("Outbounder");
        legalInfo.setLegalName("ИП Outbounder");
        legalInfo.setLegalForm(LegalFormType.IP);
        legalInfo.setOgrn("12345678");
        legalInfo.setInn("1234567890");
        return legalInfo;
    }
}
