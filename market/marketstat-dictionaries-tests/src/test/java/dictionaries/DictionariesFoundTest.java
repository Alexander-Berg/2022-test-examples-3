package dictionaries;

import org.hamcrest.Matchers;
import org.junit.Test;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.Dictionaries;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.records.DictionaryRecord;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Created by kateleb on 02.10.17.
 */
public class DictionariesFoundTest {
    @Test
    public void myTest() throws IOException {
        List<Class<? extends DictionaryRecord>> actual = Dictionaries.allDictionariesClasses();
        Attacher.attach("Classes", actual);
        assertThat(actual, not(Matchers.empty()));
    }

}
