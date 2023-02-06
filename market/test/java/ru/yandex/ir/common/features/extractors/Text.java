package ru.yandex.ir.common.features.extractors;

import ru.yandex.ir.common.be.CommonTextContent;

public class Text implements CommonTextContent {
    private final String title;
    private final String description;

    public Text(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
