package ru.yandex.market.core.protocol.model;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class ActionContextBuilder {
    public static ActionContext system(ActionType type) {
        return new SystemActionContext(type, type.getName());
    }
}
