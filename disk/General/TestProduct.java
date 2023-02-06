package ru.yandex.chemodan.eventlog.events;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.eventlog.events.billing.Product;
import ru.yandex.misc.lang.StringUtils;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public enum TestProduct {
    PAID_1TB_1Y_2015("1tb_1y_2015", "months:12", false,Cf.Tuple2List.fromPairs(
                "product_name_ru", "1 ТБ",
                "product_name_ua", "1 ТБ",
                "product_name_en", "1 TB",
                "product_name_tr", "1 TB"
    )),
    PAID_10GB_1M_2014("10gb_1m_2014", "months:1", false,Cf.Tuple2List.fromPairs(
                "product_name_ru", "10 ГБ",
                "product_name_ua", "10 ГБ",
                "product_name_en", "10 GB",
                "product_name_tr", "10 GB"
    )),
    FREE_TURKEY_PANORAMA("turkey_panorama", "months:12", true,Cf.Tuple2List.fromPairs(
                "product_name_ru", "Друзьям Яндекса",
                "product_name_ua", "Друзям Яндекса",
                "product_name_en", "For friends of Yandex",
                "product_name_tr", "Yandex dostları için"
    )),
    FREE_PASSPORT_SPLIT("passport_split", true, Cf.Tuple2List.fromPairs(
                "product_name_ru", "Друзьям Яндекса",
                "product_name_ua", "Друзям Яндекса",
                "product_name_en", "For friends of Yandex",
                "product_name_tr", "Yandex dostları için"
    )),
    FREE_YANDEX_EGE("yandex_ege", "months: 12", true, Cf.Tuple2List.fromPairs(
            "product_name_ru", "Подготовка к ЕГЭ",
            "product_name_ua", "Підготовка до ЕГЭ",
            "product_name_en", "Preparation for university entrance exams",
            "product_name_tr", "Üniversite sınavına hazırlık"
    )),
    ;

    public final Product product;

    private final String logLine;

    TestProduct(String id, boolean free, Tuple2List<String, String> names) {
        this(id, Option.empty(), free, names);
    }

    TestProduct(String id, String period, boolean free, Tuple2List<String, String> names) {
        this(id, Option.of(period), free, names);
    }

    TestProduct(String id, Option<String> period, boolean free, Tuple2List<String, String> names) {
        this.product = new Product(id, names.toMap(), period, free);
        this.logLine = buildLogLine(product, names);
    }

    private String buildLogLine(Product product, Tuple2List<String, String> names) {
        return join(
                product.toKeyValue()
                        .plus(names)
        );
    }

    private String join(Tuple2List<String, String> keyValueMap) {
        return StringUtils.join(keyValueMap.map((key, value) -> key + "=" + value), '\t');
    }

    @Override
    public String toString() {
        return logLine;
    }
}
