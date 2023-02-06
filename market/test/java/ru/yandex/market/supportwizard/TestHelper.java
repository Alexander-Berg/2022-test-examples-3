package ru.yandex.market.supportwizard;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.market.supportwizard.base.PartnerType;
import ru.yandex.market.supportwizard.storage.WeightStatisticsEntity;
import ru.yandex.startrek.client.model.CollectionUpdate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;

class TestHelper {
    static String getComment(IssueUpdate issueUpdate) {
        return issueUpdate.getComment().get().getComment().get();
    }

    static <T> T getValue(IssueUpdate issueUpdate, String field) {
        return ((ScalarUpdate<T>) issueUpdate.getValues().getO("estimatedImportance").get()).getSet().get();
    }

    static <T> ListF<T> getCollection(IssueUpdate issueUpdate, String field) {
        return ((CollectionUpdate<T>) issueUpdate.getValues().getO(field).get()).getSet();
    }

    public static WeightStatisticsEntity shopStatisticsEntity() {
        return new WeightStatisticsEntity() {
            @Override
            public PartnerType getType() {
                return PartnerType.SHOP;
            }

            @Override
            public long getPerc099Score() {
                return 99;
            }

            @Override
            public long getPerc09Score() {
                return 9;
            }

            @Override
            public long getPerc08Score() {
                return 8;
            }

            @Override
            public long getPerc07Score() {
                return 7;
            }

            @Override
            public long getPerc06Score() {
                return 6;
            }

            @Override
            public long getPerc05Score() {
                return 5;
            }

            @Override
            public long getPerc03Score() {
                return 3;
            }

            @Override
            public long getPerc01Score() {
                return 1;
            }

            @Override
            public long getAvgScore() {
                return 4;
            }
        };
    }

    public static WeightStatisticsEntity supplierStatisticsEntity() {
        return new WeightStatisticsEntity() {
            @Override
            public PartnerType getType() {
                return PartnerType.SUPPLIER;
            }

            @Override
            public long getPerc099Score() {
                return 990;
            }

            @Override
            public long getPerc09Score() {
                return 90;
            }

            @Override
            public long getPerc08Score() {
                return 80;
            }

            @Override
            public long getPerc07Score() {
                return 70;
            }

            @Override
            public long getPerc06Score() {
                return 60;
            }

            @Override
            public long getPerc05Score() {
                return 50;
            }

            @Override
            public long getPerc03Score() {
                return 30;
            }

            @Override
            public long getPerc01Score() {
                return 10;
            }

            @Override
            public long getAvgScore() {
                return 40;
            }
        };
    }
}
