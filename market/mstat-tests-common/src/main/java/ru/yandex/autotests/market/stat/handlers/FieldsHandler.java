package ru.yandex.autotests.market.stat.handlers;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;

/**
 * Created by jkt on 29.10.14.
 */
public enum FieldsHandler {

    EVENT_TIME_HANDLER(Handlers.EVENTTIME_HANDLER, new RawFilesTimeHandler()),

    QUOTED_VALUE_HANDLER(Handlers.QUOTED_VALUE_HANDLER, new QuotedHandler()),

    QUOTED_MAP_HANDLER(Handlers.QUOTED_MAP_HANDLER, new QuotedMapHandler()),

    QUOTED_LIST_HANDLER_YT(Handlers.QUOTED_LIST_HANDLER_YT, new YtQuotedListHandler()),

    UNQUOTED_LIST_HANDLER_YT(Handlers.UNQUOTED_LIST_HANDLER_YT, new YtUnquotedListHandler());

    private String handlerName;

    private TypeHandler handler;


    FieldsHandler(String handlerName, TypeHandler handler) {
        this.handlerName = handlerName;
        this.handler = handler;
    }

    public static FieldsHandler getByName(String handlerName) {
        for (FieldsHandler handler : values()) {
            if (handler.getHandlerName().equals(handlerName)) {
                return handler;
            }
        }
        throw new IllegalArgumentException("Can not find handler with name " + handlerName);
    }

    public Object parse(String text) {
        try {
            return handler.parse(text);
        } catch (TypeConversionException e) {
            throw new IllegalStateException("Can not handle string " + text, e);
        }
    }

    public String getHandlerName() {
        return handlerName;
    }
}
