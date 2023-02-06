package ru.yandex.autotests.direct.cmd.data.banners;

/**
 * Параметры для поиска баннеров
 */
public enum SearchWhat {

    NUM("num", "номеру объявления"),
    PHRASE("phrase", "фразе"),
    DOMAIN("domain", "домену"),
    IMAGE_ID("image_id", "номеру изображения"),
    GROUP("group", "номеру группы"),
    NUM_MEDIA("num_media", "номеру объявления"),
    DOMAIN_MEDIA("domain_media", "домену");

    private String name;
    private String note;


    SearchWhat(String name, String note) {
        this.name = name;
        this.note = note;
    }

    public String getName() {
        return name;
    }
    public String getNote() {
        return note;
    }

}
