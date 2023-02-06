package ru.yandex.market.api.util.jackson;

import ru.yandex.market.api.controller.jackson.ObjectMapperFactory;

/**
 * @author dimkarp93
 */
class Json extends Format {
    private static final String FIELD_TEMPLATE = "\"%s\":\"%s\"";

    public Json(ObjectMapperFactory mapperFactory) {
        super(mapperFactory, ObjectMapperFactory::getJsonObjectMapper);
    }

    @Override
    protected String template() {
        return FIELD_TEMPLATE;
    }
}
