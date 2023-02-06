package ru.yandex.market.notification.simple.model.data;

import java.util.ArrayList;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ArrayListNotificationData}.
 *
 * @author Vladislav Bauer
 */
public class ArrayListNotificationDataTest {

    @Test
    public void testContract() {
        checkIt(new ArrayListNotificationData<>());
        checkIt(new ArrayListNotificationData<>(1));
        checkIt(new ArrayListNotificationData<>(new ArrayList<>()));
    }


    private void checkIt(final ArrayListNotificationData<Object> data) {
        assertThat(data.add(new Object()), equalTo(true));
        assertThat(data.addAll(asList(new Object(), new Object())), equalTo(true));
        assertThat(data, hasSize(3));

        data.clear();
        assertThat(data, empty());
    }

}
