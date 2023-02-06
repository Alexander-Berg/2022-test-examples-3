package ru.yandex.travel.commons.logging.ydb;

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ru.yandex.travel.commons.logging.CommonMdcParams;

@Ignore
public class YdbLogLocalTest {
    @Test
    public void ydbLogCopy_filtrationAndConversion() {
        // the test is expected to be ran manually and solely so it shouldn't affect any other tests configuration
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "log4j2-yt-backup-test.xml");
        Logger log = LoggerFactory.getLogger(YdbLogLocalTest.class);

        // this message should appear only as a common plain text log record
        log.info("Message 1");

        // these messages will be written as json records as well
        MDC.put(CommonMdcParams.MDC_ENTITY_ID, "id");
        log.info("Message 2");
        log.info("Message 3");
    }
}
