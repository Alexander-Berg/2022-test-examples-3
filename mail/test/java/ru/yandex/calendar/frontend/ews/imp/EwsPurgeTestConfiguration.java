package ru.yandex.calendar.frontend.ews.imp;


import java.io.Closeable;
import java.util.stream.IntStream;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ItemIdType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.StreamEx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxy;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.misc.email.Email;

@Configuration
@Slf4j
public class EwsPurgeTestConfiguration {
    private static final int MAX_ATTEMPTS = 10;
    private static final int BATCH_LIMIT = 10;

    @Bean(destroyMethod = "close")
    public EwsPurger purger(EwsProxy ewsProxy) {
        return new EwsPurger(ewsProxy, TestManager.testExchangeUserEmail);
    }

    @AllArgsConstructor
    private class EwsPurger implements Closeable {
        private final EwsProxy ewsProxy;
        private final Email email;

        private int makeAttempt() {
            val events = ewsProxy.findInstanceEventsForPurging(email, BATCH_LIMIT);

            if (events.isEmpty()) {
                return 0;
            }

            val eventIds = StreamEx.of(events)
                    .map(CalendarItemType::getItemId)
                    .map(ItemIdType::getId)
                    .toListAndThen(Cf::list);

            ewsProxy.deleteEvents(eventIds, EwsProxyWrapper.DEFAULT_CREATE_OR_DELETE_OPERATION_TYPE);
            return events.size();
        }

        @Override
        public void close() {
            log.info("Starting purging of exchange events");

            val sum = IntStream.range(0, MAX_ATTEMPTS)
                    .map(i -> makeAttempt())
                    .takeWhile(eventCnt -> eventCnt > 0)
                    .sum();

            log.info("Purged {} events", sum);
        }
    }
}
