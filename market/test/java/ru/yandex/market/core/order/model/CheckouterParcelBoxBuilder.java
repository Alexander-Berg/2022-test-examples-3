package ru.yandex.market.core.order.model;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;

/**
 * Вспомогательный класс для удобного конструирования чекаутерного {@link ParcelBox} в тестах.
 *
 * @author ivmelnik
 * @since 15.08.18
 */
public class CheckouterParcelBoxBuilder {

    @Nonnull
    private Long id;
    private WeightAndSize weightAndSize;

    public CheckouterParcelBoxBuilder(@Nonnull Long id, WeightAndSize weightAndSize) {
        this.id = id;
        this.weightAndSize = weightAndSize;
    }

    public ParcelBox build() {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(id);
        parcelBox.setWeight(weightAndSize.getWeight());
        parcelBox.setHeight(weightAndSize.getHeight());
        parcelBox.setWidth(weightAndSize.getWidth());
        parcelBox.setDepth(weightAndSize.getDepth());
        return parcelBox;
    }
}
