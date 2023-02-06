package ru.yandex.market.mbo.cms.core.models.signal.builder;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.mbo.cms.signal.model.Signal;

public class PageDeletedSignalBuilderTest extends PageSignalBuilderAbstractTest {

    private static final String TYPE = "type";
    private static final long ID = 1;
    private static final long USER_ID = 3;

    protected Map<String, Object> getExpectedData() {
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("pageType", TYPE);
        expectedData.put("id", ID);
        expectedData.put("userId", USER_ID);
        return expectedData;
    }

    protected Signal getSignalForTest() {
        return new PageDeletedSignalBuilder().withPageId(ID).withPageType(TYPE).withUserId(USER_ID).build();
    }
}
