package ru.yandex.direct.model.generator.example;

public enum Attribute {
    CAN_EDIT_CAMPAIGN_CONTENT_LANGUAGE_BLOCK("Разрешить менять язык"),
    OPERATOR_HAS_GRID_FEATURE("Использовать новый интерфейс");
    private final String description;

    Attribute(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
