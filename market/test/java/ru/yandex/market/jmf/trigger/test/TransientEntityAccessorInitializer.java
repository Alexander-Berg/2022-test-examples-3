package ru.yandex.market.jmf.trigger.test;

import ru.yandex.market.jmf.entity.AbstractAccessorInitializer;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.metadata.Fqn;

public class TransientEntityAccessorInitializer extends AbstractAccessorInitializer<TransientEntityImpl> {
    protected TransientEntityAccessorInitializer(AttributeTypeService attributeTypeService) {
        super(Fqn.of("transientEntity"), attributeTypeService);

        withGetter("attrInt", TransientEntityImpl::getAttrInt);
        withGetter("attrStr", TransientEntityImpl::getAttrStr);
    }
}
