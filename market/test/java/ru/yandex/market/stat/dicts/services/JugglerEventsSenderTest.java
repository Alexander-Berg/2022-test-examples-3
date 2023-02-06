package ru.yandex.market.stat.dicts.services;

import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.retry.support.RetryTemplate;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.TestLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

public class JugglerEventsSenderTest {

    private JugglerEventsSender jugglerEventsSender;
    private DictionaryLoader loader;
    private JSONObject jugglerEvent;

    @Mock
    private CloseableHttpClient jugglerHttpClient;
    private RetryTemplate jugglerRetryTemplate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.loader = new TestLoader(LoaderScale.DAYLY);

        jugglerRetryTemplate = new RetryTemplate();

        this.jugglerEventsSender = new JugglerEventsSender(
                jugglerHttpClient, jugglerRetryTemplate, "production"
        );

        jugglerEvent = jugglerEventsSender.constructEvent(
                Collections.singletonList(loader.getSystemSource()),
                "OK",
                "Everything ok",
                "marketstat_production_dicts/test/test_dictionary"
        );
    }

    @Test
    public void testConstructEvent() throws JSONException {
        assertThat(jugglerEvent.getString("source"), is("mstat_production_dictionaries"));
        JSONObject firstElement = jugglerEvent.getJSONArray("events").getJSONObject(0);

        assertThat(firstElement.getString("service"), is("marketstat_production_dicts/test/test_dictionary"));
        assertThat(firstElement.getString("host"), is("mstat_dictionaries"));
        assertThat(firstElement.getString("description"), is("Everything ok"));
        assertThat(firstElement.getString("status"), is("OK"));

        assertThat(firstElement.getJSONArray("tags").getString(0), is("test"));
    }

    @Test
    public void testLogicOfSendEvent() throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);

        when(entity.getContent()).thenReturn(new ByteArrayInputStream(constructResponseJSON().toString().getBytes()));
        when(response.getEntity()).thenReturn(entity);

        when(jugglerHttpClient.execute(any())).thenReturn(response);

        assertThat(jugglerEventsSender.sendOk(loader.getDictionary(), loader.getSystemSource()), is(true));
    }

    @SneakyThrows
    private JSONObject constructResponseJSON() {
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("success", true);

        JSONArray arrayOfCodes = new JSONArray();

        JSONObject code = new JSONObject();
        code.put("code", 200);

        arrayOfCodes.put(code);
        responseJSON.put("events", arrayOfCodes);

        return responseJSON;
    }
}
