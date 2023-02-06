package ru.yandex.market.ir.autogeneration_api.http.service;

import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author danfertev
 * @since 12.07.2019
 */
public class PictureStatus {
    private final ModelStorage.Picture picture;
    private final ModelStorage.OperationStatus status;

    public PictureStatus(ModelStorage.Picture picture, ModelStorage.OperationStatus status) {
        this.picture = picture;
        this.status = status;
    }

    public ModelStorage.Picture getPicture() {
        return picture;
    }

    public ModelStorage.OperationStatus getStatus() {
        return status;
    }
}
