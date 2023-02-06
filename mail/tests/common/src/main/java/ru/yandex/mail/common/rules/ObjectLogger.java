package ru.yandex.mail.common.rules;

import org.apache.log4j.Logger;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.Arrays;
import java.util.List;

import static org.apache.log4j.Logger.getLogger;


public class ObjectLogger extends TestWatcher {
    private List<Object> objects;
    private Logger logger = getLogger(ObjectLogger.class);

    public static ObjectLogger objectLogger(Object... objects) {
        return new ObjectLogger(objects);
    }

    private ObjectLogger(Object... objects) {
        this.objects = Arrays.asList(objects);
    }

    @Override
    protected void starting(Description description) {
        for (Object object : this.objects) {
            logger.info(object.toString());
        }
    }
}
