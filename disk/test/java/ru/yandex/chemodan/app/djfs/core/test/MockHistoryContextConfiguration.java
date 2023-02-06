package ru.yandex.chemodan.app.djfs.core.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.history.EventHistoryLogger;

/**
 * @author eoshch
 */
@Configuration
public class MockHistoryContextConfiguration {
    @Bean
    @Primary
    public MockEventHistoryLogger mockEventHistoryLogger(DjfsResourceDao djfsResourceDao) {
        return new MockEventHistoryLogger(djfsResourceDao);
    }

    public static class MockEventHistoryLogger extends EventHistoryLogger {
        public ListF<MapF<String, String>> messageData = Cf.arrayList();

        public MockEventHistoryLogger(DjfsResourceDao djfsResourceDao) {
            super(djfsResourceDao);
        }

        @Override
        protected void log(MapF<String, String> data) {
            super.log(data);
            messageData.add(data);
        }
    }
}
