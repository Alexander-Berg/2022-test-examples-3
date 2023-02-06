package ru.yandex.autotests.market.stat.dictionaries_yt.beans;

import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.Dictionaries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kateleb on 03.06.17.
 */
public class DictionariesJobs {

    public static DictionariesJob PUBLISH_JOB = new DictionariesJob("publishJob");
    public static DictionariesJob UPLOAD_CURRENCY_RATES_YT_JOB = new DictionariesJob("upload_currency_rates_job");
    public static DictionariesJob UPLOAD_SHOP_CRM_YT_JOB = new DictionariesJob("upload_shop_crm_job");
    public static DictionariesJob UPLOAD_SHOP_RATINGS_YT_JOB = new DictionariesJob("upload_shop_ratings_job");

    public static Map<DictType, DictionariesJob> loaders = new HashMap<>();

    public static List<DictionariesJob> ytJobs() {
        List<DictionariesJob> list = new ArrayList<>(Arrays.asList(PUBLISH_JOB, UPLOAD_CURRENCY_RATES_YT_JOB,
            UPLOAD_SHOP_CRM_YT_JOB, UPLOAD_SHOP_RATINGS_YT_JOB));
        Dictionaries.ytDicts().forEach(dict -> {
            if (!loaders.keySet().contains(dict)) {
                loaders.put(dict, new DictionariesJob(dict));
            }
        });
        list.addAll(loaders.values());
        return list;
    }
}
