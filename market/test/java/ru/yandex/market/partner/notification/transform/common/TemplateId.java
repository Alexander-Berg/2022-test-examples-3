package ru.yandex.market.partner.notification.transform.common;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;

import static ru.yandex.market.partner.notification.transform.common.TemplateConstants.DELIVERY_GENERAL_HTML_TEMPLATE;
import static ru.yandex.market.partner.notification.transform.common.TemplateConstants.DELIVERY_TEMPLATES;
import static ru.yandex.market.partner.notification.transform.common.TemplateConstants.PICKUP_HTML_TEMPLATE;
import static ru.yandex.market.partner.notification.transform.common.TemplateConstants.PICKUP_TEMPLATES;
import static ru.yandex.market.partner.notification.transform.common.TemplateConstants.SELF_SUFFICIENT_TEMPLATES;

/**
 * Идентификатор темплейта для работы с тестами на темплейты.
 */
public final class TemplateId {

    /**
     * Айди типа нотификации.
     */
    @Nonnull
    private final long id;
    /**
     * Имя файла хранящего текст нотификации
     */
    @Nonnull
    private final String fileName;

    private TemplateId(long templateId, String fileName) {
        this.id = templateId;
        this.fileName = fileName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return true - если темплейт предназначен для уведомлений Яндекс.Доставки.
     */
    public boolean isDeliveryTemplate() {
        return DELIVERY_TEMPLATES.contains(id);
    }

    /**
     * @return true - если темплейт не нуждается в обертках при рендеринге.
     */
    public boolean isSelfSufficiantTemplate() {
        return SELF_SUFFICIENT_TEMPLATES.contains(id);
    }


    /**
     * @return true - если темплейт предназначен для уведомлений пвз.
     */
    public boolean isPickupTemplate() {
        return PICKUP_TEMPLATES.contains(id);
    }

    /**
     * @return true - если темплейт является оберкой.
     */
    public boolean isCommonTemplate() {
        return TemplateConstants.GENERAL_HTML_TEMPLATE_ID == id ||
                TemplateConstants.COMMON_TEMPLATE_ID_TO_BE_DELETED == id ||
                TemplateConstants.COMMON_TEMPLATE_ID == id ||
                PICKUP_HTML_TEMPLATE == id ||
                DELIVERY_GENERAL_HTML_TEMPLATE == id;
    }

    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public int hashCode() {
        return Objects.hash(id, fileName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TemplateId that = (TemplateId) o;
        return id == that.id &&
                fileName.equals(that.fileName);
    }

    @Override
    public String toString() {
        return fileName;
    }

    public static final class Builder {

        private Builder() {
        }

        public TemplateId buildForName(@Nonnull String fileName) {
            String[] splitString = fileName.split("_");

            if (ArrayUtils.isEmpty(splitString)) {
                throw new IllegalArgumentException("Failed to construct templateId& Invalid fileName format");
            }

            long templateId = Long.parseLong(splitString[0]);

            return new TemplateId(templateId, fileName);
        }

        public TemplateId buildForId(long id) {
            return new TemplateId(id, Long.toString(id));
        }
    }
}
