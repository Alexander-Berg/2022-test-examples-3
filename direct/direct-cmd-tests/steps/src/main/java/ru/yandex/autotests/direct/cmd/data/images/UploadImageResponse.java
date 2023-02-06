package ru.yandex.autotests.direct.cmd.data.images;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ImageAd;

public class UploadImageResponse extends ImageAd {

    public static final String ERROR_IMG_SIZE_INVALID = "Неверный размер изображения";
    public static final String ERROR_IMG_SIZE_FOR_MCBANNER_INVALID = "Размер изображения должен быть 240х400px";
    public static final String ERROR_IMG_FORMAT_INVALID = "Неверный формат изображения";
    public static final String ERROR_IMG_SIZE_MUST_BE_THE_SAME = "Можно заменить только изображение такого же размера";
    public static final String ERROR_IMG_INVALID_TITLE = "Не указано значение в поле \"Заголовок 1\"";
    public static final String ERROR_IMG_INVALID_TEXT = "Не указано значение в поле \"Текст объявления\"";
    public static final String ERROR_IMG_INVALID_AD_TYPE = "Ошибка: не указан тип баннера";
    public static final String ERROR_IMG_OR_PERMISSION_DENIED = "Изображение не существует или у вас нет к нему доступа";
    public static final String ERROR_IMG_SIZE_TOO_BIG = "Размер файла больше допустимого (120 КБ)";


    private String error;

    @SerializedName("group_id")
    private String groupId;
    @SerializedName("result")
    private Integer result;

    public String getGroupId() {
        return groupId;
    }

    public UploadImageResponse withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public Integer getResult() {
        return result;
    }

    public UploadImageResponse withResult(Integer result) {
        this.result = result;
        return this;
    }
    public String getError() {
        return error;
    }

    public UploadImageResponse withError(String error) {
        this.error = error;
        return this;
    }
}
