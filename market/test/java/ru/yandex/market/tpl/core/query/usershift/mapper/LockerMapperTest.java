package ru.yandex.market.tpl.core.query.usershift.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import ru.yandex.market.tpl.api.model.order.locker.LockerDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;

import static org.assertj.core.api.Assertions.assertThat;

class LockerMapperTest {

    private static final String DESCRIPTION = "Сначала налева, потом направо";
    private static final String PHONE_NUMBER = "+79999999999";
    private static final String IMAGE_URL = "https://avatars.mds.yandex.net/get-market-shop-logo/1528691/package_1" +
            "/orig";

    @Test
    void map() {
        LockerMapper mapper = Mappers.getMapper(LockerMapper.class);

        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setCode("test");
        pickupPoint.setImageUrl(IMAGE_URL);
        pickupPoint.setDescription(DESCRIPTION);
        pickupPoint.setPhoneNumber(PHONE_NUMBER);
        pickupPoint.setType(PickupPointType.LOCKER);
        pickupPoint.setPartnerSubType(PartnerSubType.LOCKER);
        pickupPoint.setCode("4");
        LockerDto lockerDto = mapper.map(pickupPoint);

        assertThat(lockerDto).isEqualTo(
                new LockerDto(
                        IMAGE_URL,
                        null,
                        DESCRIPTION,
                        PHONE_NUMBER,
                        PickupPointType.LOCKER,
                        PartnerSubType.LOCKER,
                        "4"
                )
        );
    }
}
