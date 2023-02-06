package ru.yandex.market.core.transform.common;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;

import ru.yandex.market.core.language.model.Language;

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
     * Язык, на котором написана нотификация.
     */
    @Nonnull
    private final Language language;
    /**
     * Имя файла хранящего текст нотификации
     */
    @Nonnull
    private final String fileName;

    private TemplateId(long templateId, Language language, String fileName) {
        this.id = templateId;
        this.language = language;
        this.fileName = fileName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return true - если темплейт предназначен для уведомлений Яндекс.Доставки.
     */
    public boolean isDeliveryTemplate() {
        return TemplateConstants.DELIVERY_TEMPLATES.contains(id);
    }

    /**
     * @return true - если темплейт не нуждается в обертках при рендеринге.
     */
    public boolean isSelfSufficiantTemplate() {
        return TemplateConstants.SELF_SUFFICIENT_TEMPLATES.contains(id);
    }


    /**
     * @return true - если темплейт предназначен для уведомлений пвз.
     */
    public boolean isPickupTemplate() {
        return TemplateConstants.PICKUP_TEMPLATES.contains(id);
    }

    /**
     * @return true - если темплейт является оберкой.
     */
    public boolean isCommonTemplate() {
        return TemplateConstants.GENERAL_HTML_TEMPLATE_ID == id ||
                TemplateConstants.COMMON_TEMPLATE_ID == id ||
                TemplateConstants.PICKUP_HTML_TEMPLATE == id ||
                TemplateConstants.DELIVERY_GENERAL_HTML_TEMPLATE == id;
    }

    public long getId() {
        return id;
    }

    public Language getLanguage() {
        return language;
    }

    public String getFileName() {
        return fileName;
    }

    public int hashCode() {
        return Objects.hash(id, language, fileName);
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
                language == that.language &&
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
            Language language = Language.RUSSIAN;

            if (splitString.length > 1) {
                language = Language.findByLanguageTag(splitString[1]);
            }

            return new TemplateId(templateId, language, fileName);
        }

        public TemplateId buildForId(long id) {
            return new TemplateId(id, Language.RUSSIAN, Long.toString(id));
        }
    }
}
