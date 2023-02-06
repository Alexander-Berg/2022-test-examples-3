package ru.yandex.market.stat.dicts.loaders;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.records.DictionaryRecord;
import ru.yandex.market.stat.dicts.records.ParametersDictionaryRecord;
import ru.yandex.market.stat.dicts.records.ParametersMboDictionaryRecord;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.dicts.utils.ParserTestUtil.readAsText;

/**
 * @author kateleb
 * @since 08.05.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class ParametesLoaderTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    private static final String TEST_MBO_URL = "//tmp/kateleb/base_mbo_export_url";
    @Mock
    private YtDictionaryStorage dictionaryStorage;
    private ParametersLoader paramsLoader;
    private List<ParametersDictionaryRecord> records;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {

        List<String> databinary = readAsText("/parsers/mbo-parameters.txt.gz");
        assertThat(databinary.size(), is(1)); //1 категория с кучей параметров
        List<DictionaryRecord> sourceData = Collections.singletonList(
            ParametersMboDictionaryRecord.builder()
                .hid(90404L).name("bicycles")
                .data(databinary.get(0))
                .build()
        );

        when(dictionaryStorage.iterator(eq(DEFAULT_CLUSTER), eq(TEST_MBO_URL + "/parameters"), any())).thenReturn(DictionaryLoadIterator.from(sourceData));
        when(dictionaryStorage.save(eq(DEFAULT_CLUSTER), any(Dictionary.class), any(LocalDateTime.class), any())).then(invocation -> {
            DictionaryLoadIterator<ParametersDictionaryRecord> iterator =
                (DictionaryLoadIterator<ParametersDictionaryRecord>) invocation.getArguments()[3];
            records = Lists.newArrayList(iterator);
            return (long) records.size();
        });

        paramsLoader = new ParametersLoader(dictionaryStorage, TEST_MBO_URL);
    }

    @Test
    public void loaderShouldLoadData() throws Exception {
        long recordCount = paramsLoader.load(DEFAULT_CLUSTER, LocalDate.now().atStartOfDay());
        verify(dictionaryStorage).iterator(eq(DEFAULT_CLUSTER), eq(TEST_MBO_URL + "/parameters"), any());
        assertThat(recordCount, greaterThan(1L));

        List<ParametersDictionaryRecord> color =
            records.stream().filter(r -> r.getCategory_id() == 90404L && r.getParam_name().equals("Цвет"))
                .collect(toList());
        assertThat("Сolor param is not found", color.size(), is(1));
        assertThat(color.get(0).getUse_for_gurulight(), is(true));
        assertThat(color.get(0).getParam_options().get(13891871L), is("оранжевый"));
        assertThat(color.get(0).getValue_type(), is("ENUM"));

        List<ParametersDictionaryRecord> width =
            records.stream().filter(r -> r.getCategory_id() == 90404L && r.getParam_name().equals("Ширина упаковки"))
                .collect(toList());
        assertThat("Width param is not found", width.size(), is(1));
        assertThat(width.get(0).getDescription(), is("Ширина заводской упаковки в сантиметрах"));
        assertThat(width.get(0).getValue_type(), is("NUMERIC"));
    }

    @Test
    public void shouldNeedLoad() {
        LocalDateTime lastLoadTime = LocalDate.parse("2019-03-23").atTime(12, 10);
        LocalDateTime wantLoadNextDay = LocalDate.parse("2019-03-25").atTime(0, 0);
        //new day has come
        assertTrue(paramsLoader.needLoad(DEFAULT_CLUSTER, lastLoadTime, wantLoadNextDay));
    }
}
