package ru.yandex.market.wms.packing.utils;

import java.util.List;

import lombok.Builder;
import lombok.Value;

import ru.yandex.market.wms.packing.dto.CloseParcelResponse.ScanDropInfo;

@Value
@Builder
public class Parcel  {
    String orderKey;
    String parcelId;
    int parcelNumber;
    boolean isLast;
    String carton;
    List<String> uits;
    Runnable validationAfterParcelClosed;
    boolean shouldCloseParcel;

    // поля про дроппинг
    boolean withDropping;
    ScanDropInfo scanDropInfo;
    String scannedDropId;

    public static class ParcelBuilder {
        private boolean isLast;
        private boolean withDropping;

        public ParcelBuilder isLast(boolean isLast) {
            this.isLast = isLast;
            return this;
        }

        public ParcelBuilder isLast() {
             return isLast(true);
        }

        public ParcelBuilder withDropping() {
            this.withDropping = true;
            return this;
        }
    }

}
