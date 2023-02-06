package ru.yandex.autotests.market.stat.dictionaries_yt.tests;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictType;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.Dictionaries;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.DictionaryRecord;
import ru.yandex.autotests.market.stat.dictionaries_yt.steps.DictionariesDataSteps;
import ru.yandex.autotests.market.stat.dictionaries_yt.dao.YtIcebergDao;
import ru.yandex.autotests.market.stat.dictionaries_yt.steps.DictionariesYtTmsSteps;
import ru.yandex.autotests.market.stat.util.ParametersUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.matchers.collection.ContainsUniqueItems.containsUniqueItems;


/**
 * Created by kateleb on 02.06.17
 */
@RunWith(Parameterized.class)
@Feature("Dictionaries YT")
@Aqua.Test(title = "Check data was updated in dictionaries-yt")
public class DictionariesYtUpdatesTest {
    private static YtIcebergDao iceberg = YtIcebergDao.newInstance();
    private static DictionariesDataSteps dataSteps = new DictionariesDataSteps();
    private static DictionariesYtTmsSteps tms = new DictionariesYtTmsSteps();
    private static Map<DictType, List<DictionaryRecord>> ytDataForDicts = new HashMap<>();
    private static List<DictionaryRecord> ytData = new ArrayList<>();
    private DictType type;

    public DictionariesYtUpdatesTest(DictType type) {
        this.type = type;

    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return ParametersUtils.asParameters(Dictionaries.ytDicts());
    }

    @Before
    public void getDataFromYt() {
        ytData = new ArrayList<>();
        if (ytDataForDicts.get(type) == null) {
//            if (!iceberg.latestPathExists(type)) {
//                tms.runJobAndWaitItFinish(DictionariesJobs.loaderFor(type));
//                tms.runJobAndWaitItFinish(DictionariesJobs.PUBLISH_JOB);
//            }

            if (iceberg.latestPathExists(type)) {
                ytData = iceberg.readFromLatestPartition(type, 10, iceberg.getRowCount(type));
            }
            ytDataForDicts.put(type, ytData);
        } else {
            ytData = ytDataForDicts.get(type);
        }
    }

    @Test
    public void testDataExistInDictionary() {
        dataSteps.checkDataExistInDictionary(ytData, type);
    }

    @Test
    public void testDataHasDistinctId() {
        Assume.assumeFalse("No  data found in yt! Nothing to check.", ytData.isEmpty());
        List<String> ids = ytData.stream().map(DictionaryRecord::id).collect(toList());
        assertThat("Table contains duplicated records!", ids, containsUniqueItems());
    }

    @Test
    public void testFieldsAreOk() {
        Assume.assumeFalse("No  data found in yt! Nothing to check.", ytData.isEmpty());
        dataSteps.checkDictFields(ytData, type);
    }
}

