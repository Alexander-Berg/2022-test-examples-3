package ru.yandex.market.robot.db;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.common.util.http.HttpGetLocation;
import ru.yandex.common.util.http.Page;
import ru.yandex.common.util.http.Response;
import ru.yandex.common.util.http.js.HttpJsLocation;

import javax.inject.Inject;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

@Ignore("For manual use. Uses YT")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:YtPageStorageTest.xml"})
public class YtPageStorageTest {

    @Inject
    private YtPageStorage pageStorage;

    private HttpJsLocation jsLocation = new HttpJsLocation(new HttpGetLocation("http://ya.ru/smth.js"));
    private HttpGetLocation location = new HttpGetLocation("http://ya.ru");
    private HttpGetLocation unknownLocation = new HttpGetLocation("http://smth.ru");
    private Page jsPage = new Page(jsLocation, "test_source2", "test_lang2", "test_mime2");
    private Page page = new Page(location, "test_source1", "test_lang1", "test_mime1");
    private Response jsResponse = new Response(jsLocation, jsPage);
    private Response response = new Response(location, page);

    @Test
    public void testCacheJsLocation() {
        pageStorage.put(jsResponse);

        Page pageCached = pageStorage.get(jsLocation);

        assertThat(pageCached, pageEqualTo(jsPage));
    }

    @Test
    public void testCacheNonJsLocation() {
        pageStorage.put(response);

        Page pageCached = pageStorage.get(location);

        assertThat(pageCached, pageEqualTo(page));
        assertThat(pageStorage.get(new HttpJsLocation(location)), is(nullValue()));
    }

    @Test
    public void testCacheManyLocations() {
        pageStorage.put(asList(jsResponse, response));

        assertThat(pageStorage.get(asList(location)), allOf(hasEntry(equalTo(location), pageEqualTo(page))));
        assertThat(pageStorage.get(asList(location)), allOf(hasEntry(equalTo(jsLocation), pageEqualTo(jsPage))));
    }

    @Test
    public void testGetManyLocations_and_new() {
        pageStorage.put(asList(jsResponse, response));

        assertThat(pageStorage.get(asList(unknownLocation)),
            allOf(hasEntry(equalTo(unknownLocation), is(nullValue()))));
        assertThat(pageStorage.get(asList(jsLocation)), allOf(hasEntry(equalTo(jsLocation), pageEqualTo(jsPage))));
    }

    @Test
    public void testGetManyLocations_and_new_with_exception() {
        pageStorage.put(asList(jsResponse, response));

        pageStorage.setYt(null);

        assertThat(pageStorage.get(asList(unknownLocation)),
            allOf(hasEntry(equalTo(unknownLocation), is(nullValue()))));
        assertThat(pageStorage.get(asList(jsLocation)),allOf(hasEntry(equalTo(jsLocation), is(nullValue()))));
    }

    private static Matcher<Page> pageEqualTo(Page expectedPage) {
        return new BaseMatcher<Page>() {
            Throwable throwable;

            @Override
            public boolean matches(Object o) {
                try {
                    Page actualPage = (Page) o;
                    assertThat(actualPage.getDocument().isEqualNode(expectedPage.getDocument()), is(true));
                    assertThat(actualPage.getLanguage(), is(expectedPage.getLanguage()));
                    assertThat(actualPage.getLocation(), is(expectedPage.getLocation()));
                    assertThat(actualPage.getMime(), is(expectedPage.getMime()));
                    assertThat(actualPage.getSource(), is(expectedPage.getSource()));

                    return true;
                } catch (RuntimeException | AssertionError e) {
                    this.throwable = e;

                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                if (throwable != null) {
                    description.appendText(throwable.getMessage());
                }
            }
        };
    }

}