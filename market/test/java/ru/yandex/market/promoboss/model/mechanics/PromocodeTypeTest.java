package ru.yandex.market.promoboss.model.mechanics;

import ru.yandex.market.promoboss.model.AbstractModelEnumTest;
import ru.yandex.mj.generated.server.model.Promocode;

class PromocodeTypeTest extends AbstractModelEnumTest<Promocode.CodeTypeEnum, PromocodeType> {
    PromocodeTypeTest() {
        super(Promocode.CodeTypeEnum.class, PromocodeType.class);
    }
}
