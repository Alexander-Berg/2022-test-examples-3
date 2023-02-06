package dictionaries;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictType;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.Dictionaries;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.DictionaryRecord;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 03.07.17.
 */
@RunWith(Parameterized.class)
public class DictionaryRecordTest {
    private DictionaryRecord record;
    private DictType dictType;


    public DictionaryRecordTest(Class<DictionaryRecord> dictClass, String className) throws IllegalAccessException, InstantiationException {
        this.dictType = new DictType(dictClass);
        this.record = dictClass.newInstance();
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() throws IOException {
        return Dictionaries.allDictionariesClasses()
            .stream().map(it -> new Object[]{it, it.getSimpleName()})
            .collect(Collectors.toList());
    }

    @Test
    public void testValidId() {
        record.id();
    }

    @Test
    public void testQueryCondition() {
        record.getQueryCondition();
    }

    @Test
    public void testTableName() {
        assertThat(dictType.getTableName(), not(isEmptyString()));
    }
}
