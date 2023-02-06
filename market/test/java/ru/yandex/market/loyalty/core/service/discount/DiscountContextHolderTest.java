package ru.yandex.market.loyalty.core.service.discount;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.core.service.discount.DiscountChangeSource.POST_DISCOUNT_REVERT;
import static ru.yandex.market.loyalty.core.service.discount.DiscountChangeSource.TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS;

public class DiscountContextHolderTest {

    @Before
    public void init() {
        DiscountContextHolder.clearContext();
    }

    @Test
    public void shouldAddSourceAndNotes() {
        DiscountContextHolder.setDiscountChangeSourceAndNotes(POST_DISCOUNT_REVERT, "Hello!");

        assertThat(DiscountContextHolder.getContext().getNotes(), is("Hello!"));
        assertThat(DiscountContextHolder.getContext().getDiscountChangeSource(), is(POST_DISCOUNT_REVERT));
    }

    @Test
    public void shouldSetLastSource() {
        DiscountContextHolder.setDiscountChangeSource(POST_DISCOUNT_REVERT);
        DiscountContextHolder.setDiscountChangeSource(TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS);

        assertThat(DiscountContextHolder.getContext().getDiscountChangeSource(),
                is(TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS));
        assertThat(DiscountContextHolder.getContext().getNotes(), emptyString());
    }

    @Test
    public void shouldClearNotesWhenSetSource() {
        DiscountContextHolder.setDiscountChangeSourceAndNotes(POST_DISCOUNT_REVERT, "notes");
        DiscountContextHolder.setDiscountChangeSource(TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS);

        assertThat(DiscountContextHolder.getContext().getDiscountChangeSource(),
                is(TMS_PROCESS_CHECKOUTER_EVENTS_BUCKETS));
        assertThat(DiscountContextHolder.getContext().getNotes(), emptyString());
    }

    @Test
    public void shouldAppendNotes() {
        DiscountContextHolder.setDiscountChangeSourceAndNotes(POST_DISCOUNT_REVERT, "Hello!");
        DiscountContextHolder.appendNotes("How are you?");

        assertThat(DiscountContextHolder.getContext().getNotes(), is("Hello!\nHow are you?"));
        assertThat(DiscountContextHolder.getContext().getDiscountChangeSource(), is(POST_DISCOUNT_REVERT));
    }
}
