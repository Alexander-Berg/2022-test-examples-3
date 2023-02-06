package ru.yandex.market.crm.operatorwindow.utils.mail;

import ru.yandex.market.crm.util.CrmObjects;

/**
 * Позволяет получать тело письма вида:
 * Имя клиента:
 * Василий Пупкин
 * <p>
 * Проблема клиента:
 * нет_проблем
 */
public class DynamicLogisticMailBodyBuilder {
    private final String keySeparator;
    private final String linesSeparator;
    private final StringBuilder body = new StringBuilder();

    public DynamicLogisticMailBodyBuilder() {
        this(": \n", "\n\n");
    }

    public DynamicLogisticMailBodyBuilder(String keySeparator, String linesSeparator) {
        this.keySeparator = keySeparator;
        this.linesSeparator = linesSeparator;
    }

    /**
     * Добавляет строку вида
     * key: \n
     * {value / defaultValue} \n\n
     *
     * @param key          Ключ атрибута
     * @param defaultValue значение атрибута, выбирается, если value не задано
     * @param value        значение атрибута, выбирается по умолчанию
     */
    public DynamicLogisticMailBodyBuilder addAttribute(String key, String value, String defaultValue) {
        if (defaultValue == null && value == null) {
            return this;
        }
        var attributeValue = CrmObjects.firstNonNull(value, defaultValue);

        body.append(key);
        body.append(keySeparator);
        body.append(attributeValue);
        body.append(linesSeparator);
        return this;
    }

    public DynamicLogisticMailBodyBuilder addAttribute(String key, Long value, Long defaultValue) {
        return addAttribute(key,
                value == null ? null : value.toString(),
                defaultValue == null ? null : defaultValue.toString());
    }

    public DynamicLogisticMailBodyBuilder addAttribute(String key, String value) {
        return addAttribute(key, value, null);
    }

    public DynamicLogisticMailBodyBuilder addAttribute(String key, Long value) {
        return addAttribute(key, value, null);
    }

    public String build() {
        if (body.length() > 0) {
            return body.substring(0, body.length() - linesSeparator.length());
        }

        return body.toString();
    }
}
