package ru.yandex.market.partner.mvc.controller.feed;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.partner.feed.model.CreateFeedResponse;
import ru.yandex.market.partner.mvc.MvcTestSerializationConfig;

/**
 * @author fbokovikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MvcTestSerializationConfig.class)
public class FeedCreationSerializationTest {

    @Autowired
    private SerializationChecker checker;

    private CreateFeedResponse createFeedResponse = new CreateFeedResponse(100L);

    @Test
    public void testSerialization() {
        checker.testJsonSerialization(createFeedResponse, "{\"feedId\":100}");
    }

}
