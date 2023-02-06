package ru.yandex.market.logistics.management.facade;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.dto.TextObject;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.front.customerInfo.PartnerCustomerInfoUpdateDto;
import ru.yandex.market.logistics.management.exception.BadRequestException;

public class PartnerCustomerInfoFacadeTest extends AbstractTest {

    private PartnerCustomerInfoFacade partnerCustomerInfoFacade = new PartnerCustomerInfoFacade(null, null, null, null);

    @Test
    public void shouldCheckPhoneNumbersOnCreate() {
        softly.assertThatThrownBy(() -> {
            partnerCustomerInfoFacade.create(
                PartnerCustomerInfoUpdateDto.newBuilder()
                    .phones(new TextObject("+7 800 123-45-67\n123-45-68"))
                    .build()
            );
        }).isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldCheckPhoneNumbersOnUpdate() {
        softly.assertThatThrownBy(() -> {
            partnerCustomerInfoFacade.update(
                1L,
                PartnerCustomerInfoUpdateDto.newBuilder()
                    .phones(new TextObject("+7 800 123-45-67\n123-45-68"))
                    .build()
            );
        }).isInstanceOf(BadRequestException.class);
    }
}
