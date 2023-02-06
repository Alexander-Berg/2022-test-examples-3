package ru.yandex.autotests.direct.cmd.data.excel;

/*
* todo javadoc
*/
public enum ExcelColumnsEnum {
    ADDITION_BANNER("Доп. объявление группы"),
    GROUP_ID("ID группы"),
    BANNER_ID("ID объявления"),
    PHRASE_ID("ID фразы"),
    IS_MOBILE("Мобильное объявление"),
    BANNER_TYPE("Тип объявления"),
    HREF("Ссылка"),
    CALLOUTS("Уточнения"),
    APP_HREF("Ссылка на приложение в магазине"),
    TRACKING_HREF("Трекинговая ссылка"),
    DISPLAY_HREF("Отображаемая ссылка"),
    TEXT("Текст"),
    IMAGE("Изображение"),
    PARAM1("Параметр 1"),
    PARAM2("Параметр 2"),
    PHARASE_WITH_MINUS_WORDS("Фраза (с минус-словами)");

    private String caption;

    ExcelColumnsEnum(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }
}
