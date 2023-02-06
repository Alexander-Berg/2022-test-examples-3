package ru.yandex.market.ff.model.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.dto.CarDTO;
import ru.yandex.market.ff.client.dto.ClientResourceIdDTO;
import ru.yandex.market.ff.client.dto.CourierDTO;
import ru.yandex.market.ff.client.dto.LegalEntityDTO;
import ru.yandex.market.ff.client.dto.LocationDTO;
import ru.yandex.market.ff.client.dto.PersonDTO;
import ru.yandex.market.ff.client.dto.PhoneDTO;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ParametersAreNonnullByDefault
class RequestCourierConverterTest {

    private RequestCourierConverter converter = new RequestCourierConverter();

    @Test
    void entityFromDto() {
        assertNull(converter.entitiesFromDto(null));
        CourierDTO dto = new CourierDTO();
        dto.setPartnerId(new ClientResourceIdDTO());
        assertNotNull(converter.entitiesFromDto(dto));
    }

    @Test
    void legalEntityFromDto() {
        assertNull(converter.legalEntityFromDto(null));
        assertNotNull(converter.legalEntityFromDto(new LegalEntityDTO()));
    }

    @Test
    void addressEntityFromDto() {
        assertNull(converter.addressEntityFromDto(null));
        assertNotNull(converter.addressEntityFromDto(new LocationDTO()));
    }

    @Test
    void carEntityFromDto() {
        assertNull(converter.carEntityFromDto(null));
        assertNotNull(converter.carEntityFromDto(new CarDTO()));
    }

    @Test
    void phoneEntityFromDto() {
        assertNull(converter.phoneEntityFromDto(null));
        assertNotNull(converter.phoneEntityFromDto(new PhoneDTO()));
    }

    @Test
    void personEntitiesFromDtos() {
        assertNull(converter.personEntitiesFromDtos(null));
        assertNotNull(converter.personEntitiesFromDtos(List.of()));
        assertNotNull(converter.personEntitiesFromDtos(List.of(new PersonDTO())));
    }

    @Test
    void partnerIdEntityFromDto() {
        assertNotNull(converter.partnerIdEntityFromDto(new ClientResourceIdDTO()));
    }
}
