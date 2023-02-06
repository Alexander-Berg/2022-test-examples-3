package ru.yandex.chemodan.app.notes.core.test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.dataapi.api.user.DataApiUserId;
import ru.yandex.chemodan.app.notes.core.MdsContextConfiguration;
import ru.yandex.chemodan.app.notes.core.NotesContentMdsManager;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;

/**
 * @author vpronto
 */
@ContextConfiguration(classes = {
        MdsContextConfiguration.class,
})
public class NotesContentMdsManagerTest extends NotesAbstractTest {

    private final static Logger logger = LoggerFactory.getLogger(NotesContentMdsManagerTest.class);

    @Autowired
    private NotesContentMdsManager mdsManager;

    private final String uuid = UUID.randomUUID().toString();
    private final String testData = "exampleString";

    @Test
//    not stable: depends on external service
    public void test() {

        try {
            MdsFileKey put = mdsManager.put(uuid, DataApiUserId.parse("123"), 1, testData.getBytes(StandardCharsets.UTF_8));

            OutputStream outputStream = new OutputStream() {
                private StringBuilder string = new StringBuilder();

                @Override
                public void write(int b) throws IOException {
                    this.string.append((char) b);
                }
                public String toString() {
                    return this.string.toString();
                }
            };
            mdsManager.get(put, outputStream);
            String result = outputStream.toString();
            logger.debug("result: {}", result);
            Assert.equals(testData, result);
            mdsManager.deleteData(put);
        } catch (Exception e) { // catch only Exception, Assert throws AssertionError
            logger.error("Ooops something goes wrong {}", e);
        }

    }

}
