package ru.yandex.market.stat.dicts.loaders;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import ru.yandex.market.stat.dicts.loaders.tvm.TvmTicketSupplier;
import ru.yandex.market.stat.dicts.services.DictionaryStorage;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.market.stat.dicts.loaders.PartnersLoader.joinQuoted;

/**
 * @author Ekaterina Lebedeva <kateleb@yandex-team.ru>
 */
@RunWith(DataProviderRunner.class)
public class PartnersLoaderTest {

    @Mock
    private DictionaryStorage dictionaryStorage;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private TvmTicketSupplier tvmTicketSupplier;

    private PartnersLoader partnersLoader;
    private BufferedReader reportJsonReader;

    @Before
    public void setUp() {
        initMocks(this);
        when(tvmTicketSupplier.get(anyInt())).thenReturn("testTvmTicket");
        partnersLoader = new PartnersLoader(httpClient, dictionaryStorage, tvmTicketSupplier);
    }

    @Test
    public void testParseSets() {
        reportJsonReader = new BufferedReader(new InputStreamReader(PartnersLoaderTest.class.getResourceAsStream("/parsers/partners_sets.json")));
        Map<Long, Date> records = partnersLoader.parseSetData(reportJsonReader);
        assertThat(records.size(), equalTo(6));
        assertThat(records.get(353759L), equalTo(Date.valueOf("2018-04-10")));
        assertThat(records.get(353756L), equalTo(Date.valueOf("2018-04-06")));
        assertThat(records.get(353745L), equalTo(Date.valueOf("2018-04-03")));
    }

    @Test
    public void testParsePacks() {
        reportJsonReader = new BufferedReader(new InputStreamReader(PartnersLoaderTest.class.getResourceAsStream("/parsers/partners_packs.json")));
        List<PartnersLoader.PackData> stringMapMap = partnersLoader.parsePackData(reportJsonReader);
        assertThat(stringMapMap.size(), equalTo(11));

        PartnersLoader.PackData p1 = stringMapMap.stream().filter(f -> f.getPackId().equals(7219L)).findFirst().orElse(null);
        PartnersLoader.PackData p2 = stringMapMap.stream().filter(f -> f.getPackId().equals(7192L)).findFirst().orElse(null);

        assertThat(p1.getDomain(), equalTo("http://zaporx.ru/"));
        assertThat(p2.getDomain(), equalTo("s2s.kakieshtory.ru"));
        assertThat(p1.getPackComment(), equalTo(""));
        assertThat(p2.getPackComment(), equalTo("mmm"));
        assertThat(p1.getManagerLogin(), equalTo("system-robot"));
        assertThat(p2.getManagerLogin(), equalTo("ads"));
    }

    @Test
    public void testParsePercents() {
        reportJsonReader = new BufferedReader(new InputStreamReader(PartnersLoaderTest.class.getResourceAsStream("/parsers/partners_percent.json")));
        Map<Long, Double> records = partnersLoader.parseClidsWithPercent(reportJsonReader);
        assertThat(records.size(), equalTo(2));
        assertThat(records.get(2210240L), equalTo(0.3));
        assertThat(records.get(2349536L), equalTo(0.55));
    }

    @Test
    public void testParsePlaces() {
        reportJsonReader = new BufferedReader(new InputStreamReader(PartnersLoaderTest.class.getResourceAsStream("/parsers/places_data.json")));
        List<PartnersLoader.PlacesData> records = partnersLoader.parsePlacesData(reportJsonReader);

        PartnersLoader.PlacesData p1 = records.stream().filter(f -> f.getName().equals("adsa")).findFirst().orElse(null);
        PartnersLoader.PlacesData p2 = records.stream().filter(f -> f.getName().equals("asdasd")).findFirst().orElse(null);

        assertThat(p1.getStatus(), equalTo(4));
        assertThat(p2.getStatus(), equalTo(2));
        assertThat(p1.getTypePlace(), equalTo("asd"));
        assertThat(p2.getTypePlace(), equalTo("asdew"));
        assertThat(p1.getUniqueVisitors(), equalTo("500 000 и более"));
        assertThat(p2.getUniqueVisitors(), equalTo("asdas"));
        assertThat(p1.getUrl(), equalTo("ewrtewt.ru"));
        assertThat(p2.getUrl(), equalTo("asdasd.ru"));
        assertThat(p1.getPartnerSegment(), equalTo(null));
        assertThat(p2.getPartnerSegment(), equalTo("closer"));
    }

    @Test
    public void testJoinQuoted() {
        assertThat(joinQuoted(Arrays.asList("a", "bb", "ccc")),
                equalTo("\"a\",\"bb\",\"ccc\""));
        assertThat(joinQuoted(Arrays.asList("", "b")),
                equalTo("\"\",\"b\""));
    }
}
