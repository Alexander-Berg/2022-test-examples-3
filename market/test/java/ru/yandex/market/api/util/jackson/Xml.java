package ru.yandex.market.api.util.jackson;

import ru.yandex.market.api.controller.jackson.ObjectMapperFactory;

/**
 * @author dimkarp93
 */
class Xml extends Format {
    private static final String FIELD_TEMPLATE = "<%1$s>%2$s</%1$s>";

    public Xml(ObjectMapperFactory mapperFactory) {
        super(mapperFactory, ObjectMapperFactory::getXmlObjectMapper);
    }

    @Override
    protected String template() {
        return FIELD_TEMPLATE;
    }
}
