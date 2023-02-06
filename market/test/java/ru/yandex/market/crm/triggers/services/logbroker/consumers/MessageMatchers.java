package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.assertj.core.api.Assertions;
import org.assertj.core.matcher.AssertionMatcher;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;

import static org.hamcrest.Matchers.containsInAnyOrder;

public class MessageMatchers {

    private static class MessageMatcher extends AssertionMatcher<UidBpmMessage> {

        private final UidBpmMessage expected;

        private MessageMatcher(UidBpmMessage expected) {
            this.expected = expected;
        }

        @Override
        public void assertion(UidBpmMessage actual) throws AssertionError {
            Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    private static class MessageListMatcher extends AssertionMatcher<List<UidBpmMessage>> {

        private final List<UidBpmMessage> messages;

        private MessageListMatcher(List<UidBpmMessage> messages) {
            this.messages = messages;
        }

        @Override
        public void assertion(List<UidBpmMessage> actual) throws AssertionError {
            Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(messages);
        }
    }


    public static MessageListMatcher messagesMatcher(UidBpmMessage... messages) {
        return new MessageListMatcher(List.of(messages));
    }
}
