package factory;

import java.util.List;

import dto.requests.checkouter.RearrFactor;
import dto.requests.report.OfferItem;
import step.ReportSteps;

@SuppressWarnings("checkstyle:NoWhitespaceBefore")
public enum OfferItems {
    //честные стоки, заменено в тестах на нечестные FF_172_UNFAIR_STOCK
    FF {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "200634693.*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    //честные стоки, заменено в тестах на нечестные FF_171_UNFAIR_STOCK
    FF_171 {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "200634692.*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },

    FF_171_1P_FASHION_1 {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                FF_1P_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "200396943.00065.00026.100126177243",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },

    FF_171_1P_FASHION_2 {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                FF_1P_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "200396943.00065.00026.100126174719",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },

    FF_145 {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "200344277.*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    CROSSDOC {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "200423152.100126176155",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    DROPSHIP_SD_PICKUP {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                DROPSHIP_DS_PICKUP_SUPPLIER_ID,
                DROPSHIP_DS_PICKUP_FEED_ID,
                "*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    DROPSHIP_SD_COURIER {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                DROPSHIP_DS_COURIER_SUPPLIER_ID,
                DROPSHIP_DS_COURIER_FEED_ID,
                "*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    DROPSHIP_DR {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                DROPSHIP_DR_SUPPLIER_ID,
                DROPSHIP_DR_FEED_ID,
                "*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    DROPSHIP_DR_PVZ {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                DROPSHIP_DR_PVZ_SUPPLIER_ID,
                DROPSHIP_DR_PVZ_FEED_ID,
                "*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    DROPSHIP_SC {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_DROPSHIP_SC_SUPPLIER_ID,
                BLUE_DROPSHIP_SC_FEED_ID,
                "*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    DROPSHIP_EXPRESS {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                DROPSHIP_EXPRESS_SUPPLIER_ID,
                DROPSHIP_EXPRESS_FEED_ID,
                "*",
                count,
                RearrFactor.EXPRESS.getValue(),
                true);
        }
    },
    //Используются честные стоки, заменено в тестах на FF_300_UNFAIR_STOCK_EXPRESS
    DARKSTORE_EXPRESS {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "200955562.00065.00026.100126174453",
                count,
                RearrFactor.EXPRESS.getValue(),
                true);
        }
    },

    FF_172_UNFAIR_STOCK {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "201537597.*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    FF_172_B2B {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "201734177.*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    FF_171_UNFAIR_STOCK {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "201552170.*",
                count,
                RearrFactor.GLOBAL.getValue(),
                true);
        }
    },
    FF_300_UNFAIR_STOCK_EXPRESS {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                BLUE_VIRTUAL_SUPPLIER_ID,
                BLUE_VIRTUAL_FEED_ID,
                "201553298.*",
                count,
                RearrFactor.EXPRESS.getValue(),
                true);
        }
    },
    DBS_ITEM {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                DBS_SUPPLIER_ID,
                DBS_FEED_ID,
                "*",
                count,
                RearrFactor.GLOBAL.getValue(),
                isDifferentOffers);
        }
    },
    DBS_TO_MARKET_PICKUP_ITEM {
        @Override
        public List<OfferItem> getItems(int count, boolean isDifferentOffers) {
            return REPORT_STEPS.getValidOffer(
                DBS_TO_MARKET_PICKUP_SUPPLIER_ID,
                DBS_TO_MARKET_PICKUP_FEED_ID,
                "10622*",
                count,
                RearrFactor.GLOBAL.getValue(),
                isDifferentOffers,
                true
            );
        }
    },
    ;

    private static final long BLUE_VIRTUAL_SUPPLIER_ID = 431782;
    private static final long BLUE_VIRTUAL_FEED_ID = 475690;
    private static final long DROPSHIP_DS_COURIER_SUPPLIER_ID = 10726518;
    private static final long DROPSHIP_DS_COURIER_FEED_ID = 200746976;
    private static final long DROPSHIP_DS_PICKUP_SUPPLIER_ID = 10726603;
    private static final long DROPSHIP_DS_PICKUP_FEED_ID = 200747029;
    private static final long BLUE_DROPSHIP_SC_SUPPLIER_ID = 10361574;
    private static final long BLUE_DROPSHIP_SC_FEED_ID = 200515163;
    private static final long DROPSHIP_DR_SUPPLIER_ID = 10781189;
    private static final long DROPSHIP_DR_FEED_ID = 200836573;
    private static final long DROPSHIP_EXPRESS_SUPPLIER_ID = 10782016;
    private static final long DROPSHIP_EXPRESS_FEED_ID = 200838835;
    private static final long DBS_FEED_ID = 201022469;
    private static final long DBS_SUPPLIER_ID = 11125682;
    private static final long DROPSHIP_DR_PVZ_SUPPLIER_ID = 10793182;
    private static final long DROPSHIP_DR_PVZ_FEED_ID = 200860515;
    private static final long FF_1P_SUPPLIER_ID = 10264169;
    private static final long DBS_TO_MARKET_PICKUP_FEED_ID = 201204916;
    private static final long DBS_TO_MARKET_PICKUP_SUPPLIER_ID = 11306056;

    private static final ReportSteps REPORT_STEPS = new ReportSteps();

    public abstract List<OfferItem> getItems(int count, boolean isDifferentOffers);

    public OfferItem getItem() {
        List<OfferItem> items = getItems(1, true);
        assert !items.isEmpty() : "Список итемов пустой";
        return items.get(0);
    }

}
