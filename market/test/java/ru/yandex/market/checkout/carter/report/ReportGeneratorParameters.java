package ru.yandex.market.checkout.carter.report;

import java.util.Objects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ReportGeneratorParameters {

    private final Multimap<Long, ReportOffer> mskuToOfferMap = HashMultimap.create();
    private final Multimap<Long, ReportOffer> mskuToOfferResponseMap = HashMultimap.create();

    public Multimap<Long, ReportOffer> getMskuToOfferMap() {
        return mskuToOfferMap;
    }

    public Multimap<Long, ReportOffer> getMskuToOfferResponseMap() {
        return mskuToOfferResponseMap;
    }

    public ReportGeneratorParameters addReportOffer(Long msku, ReportOffer reportOffer) {
        mskuToOfferMap.put(msku, reportOffer);
        return this;
    }

    public ReportGeneratorParameters addReportOffer(Long msku, String wareMd5) {
        mskuToOfferMap.put(msku, new ReportOffer(wareMd5));
        return this;
    }

    public ReportGeneratorParameters addReportResponseOffer(Long msku, ReportOffer reportOffer) {
        mskuToOfferResponseMap.put(msku, reportOffer);
        return this;
    }

    public static class ReportOffer {

        private final String wareMd5;
        private Integer warehouseId = 1;
        private Integer price;
        private Integer count;
        private boolean priceDropEnabled;

        public ReportOffer(String wareMd5) {
            this.wareMd5 = wareMd5;
        }

        public ReportOffer(String wareMd5, Integer warehouseId) {
            this.wareMd5 = wareMd5;
            this.warehouseId = warehouseId;
        }

        public ReportOffer(String wareMd5, Integer warehouseId, boolean priceDropEnabled) {
            this.wareMd5 = wareMd5;
            this.warehouseId = warehouseId;
            this.priceDropEnabled = priceDropEnabled;
        }

        public ReportOffer(String wareMd5, Integer warehouseId, Integer price) {
            this.wareMd5 = wareMd5;
            this.warehouseId = warehouseId;
            this.price = price;
        }

        public ReportOffer(String wareMd5, Integer warehouseId, Integer price, Integer count) {
            this.wareMd5 = wareMd5;
            this.warehouseId = warehouseId;
            this.price = price;
            this.count = count;
        }

        public String getWareMd5() {
            return wareMd5;
        }

        public Integer getWarehouseId() {
            return warehouseId;
        }

        public Integer getPrice() {
            return price;
        }

        public Integer getCount() {
            return count;
        }

        public boolean isPriceDropEnabled() {
            return priceDropEnabled;
        }

        @Override
        public String toString() {
            return "ReportOffer{" +
                    "wareMd5='" + wareMd5 + '\'' +
                    ", warehouseId=" + warehouseId +
                    ", price=" + price +
                    ", count=" + count +
                    ", priceDropEnabled=" + priceDropEnabled +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ReportOffer that = (ReportOffer) o;
            return Objects.equals(wareMd5, that.wareMd5) &&
                    Objects.equals(warehouseId, that.warehouseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(wareMd5, warehouseId);
        }
    }
}
