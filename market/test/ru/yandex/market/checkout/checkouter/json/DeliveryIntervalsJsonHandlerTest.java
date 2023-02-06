package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * @author mmetlov
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {"classpath:WEB-INF/checkouter-serialization.xml"})
public class DeliveryIntervalsJsonHandlerTest {

    public static final String OLD_JSON_STR =
            "[" +
                    "{" +
                    "\"date\":\"01-01-2018\"," +
                    "\"intervals\":[" +
                    "{\"fromTime\":\"11:00\",\"toTime\":\"12:00\",\"isDefault\":true}," +
                    "{\"fromTime\":\"13:00\",\"toTime\":\"14:00\"}" +
                    "]},{" +
                    "\"date\":\"02-01-2018\"," +
                    "\"intervals\":[" +
                    "{\"fromTime\":\"01:00\",\"toTime\":\"02:00\"}," +
                    "{\"fromTime\":\"03:00\",\"toTime\":\"04:00\"}" +
                    "]}" +
                    "]";
    public static final String NEW_JSON_STR =
            "[" +
                    "{" +
                    "\"intervals\":[" +
                    "       {\"fromTime\":\"11:00\",\"toTime\":\"12:00\",\"isDefault\":true}," +
                    "       {\"fromTime\":\"13:00\",\"toTime\":\"14:00\"}" +
                    "]}" +
                    "]";
    @Autowired
    HttpMessageConverter converter;

    @Test
    public void shouldDeserializeOldDeliveryIntervals() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(OLD_JSON_STR.getBytes());
        RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection =
                (RawDeliveryIntervalsCollection) converter.read(RawDeliveryIntervalsCollection.class, inputMessage);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 0, 1);
        Date date1 = DateUtil.truncDay(calendar.getTime());
        calendar.set(2018, 0, 2);
        Date date2 = DateUtil.truncDay(calendar.getTime());
        assertThat(rawDeliveryIntervalsCollection.getDates(), containsInAnyOrder(date1, date2));
        assertThat(rawDeliveryIntervalsCollection.getIntervalsByDate(date1), containsInAnyOrder(
                allOf(
                        hasProperty("fromTime", is(LocalTime.of(11, 0))),
                        hasProperty("toTime", is(LocalTime.of(12, 0))),
                        hasProperty("default", is(true))
                ),
                allOf(
                        hasProperty("fromTime", is(LocalTime.of(13, 0))),
                        hasProperty("toTime", is(LocalTime.of(14, 0))),
                        hasProperty("default", is(false))
                )
        ));
        assertThat(rawDeliveryIntervalsCollection.getIntervalsByDate(date2), containsInAnyOrder(
                allOf(
                        hasProperty("fromTime", is(LocalTime.of(1, 0))),
                        hasProperty("toTime", is(LocalTime.of(2, 0))),
                        hasProperty("default", is(false))
                ),
                allOf(
                        hasProperty("fromTime", is(LocalTime.of(3, 0))),
                        hasProperty("toTime", is(LocalTime.of(4, 0))),
                        hasProperty("default", is(false))
                )
        ));
    }

    @Test
    public void shouldDeserializeNewDeliveryIntervals() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(NEW_JSON_STR.getBytes());
        RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection =
                (RawDeliveryIntervalsCollection) converter.read(RawDeliveryIntervalsCollection.class, inputMessage);
        assertThat(rawDeliveryIntervalsCollection.getDates(), contains(nullValue()));
        assertThat(rawDeliveryIntervalsCollection.getIntervalsByDate(null), containsInAnyOrder(
                allOf(
                        hasProperty("fromTime", is(LocalTime.of(11, 0))),
                        hasProperty("toTime", is(LocalTime.of(12, 0))),
                        hasProperty("default", is(true))
                ),
                allOf(
                        hasProperty("fromTime", is(LocalTime.of(13, 0))),
                        hasProperty("toTime", is(LocalTime.of(14, 0))),
                        hasProperty("default", is(false))
                )
        ));
    }

    @Test
    public void shouldSerializeOldDeliveryIntervals() throws IOException, JSONException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 0, 1);
        Date date1 = DateUtil.truncDay(calendar.getTime());
        calendar.set(2018, 0, 2);
        Date date2 = DateUtil.truncDay(calendar.getTime());
        RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection = new RawDeliveryIntervalsCollection();
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(date1, LocalTime.of(11, 0), LocalTime.of(12, 0),
                true));
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(date1, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(date2, LocalTime.of(1, 0), LocalTime.of(2, 0)));
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(date2, LocalTime.of(3, 0), LocalTime.of(4, 0)));
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(rawDeliveryIntervalsCollection, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(
                OLD_JSON_STR,
                outputMessage.getBodyAsString(),
                false
        );
    }

    @Test
    public void shouldSerializeNewDeliveryIntervals() throws IOException, JSONException {
        RawDeliveryIntervalsCollection rawDeliveryIntervalsCollection = new RawDeliveryIntervalsCollection();
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(null, LocalTime.of(11, 0), LocalTime.of(12, 0),
                true));
        rawDeliveryIntervalsCollection.add(new RawDeliveryInterval(null, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(rawDeliveryIntervalsCollection, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(
                NEW_JSON_STR,
                outputMessage.getBodyAsString(),
                false
        );
    }
}
