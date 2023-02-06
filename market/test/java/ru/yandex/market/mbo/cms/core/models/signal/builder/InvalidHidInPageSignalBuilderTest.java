package ru.yandex.market.mbo.cms.core.models.signal.builder;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.signal.model.Signal;

public class InvalidHidInPageSignalBuilderTest extends PageSignalBuilderAbstractTest {

    private static final String TYPE = "type";
    private static final long ID = 1;
    private static final long REVISION_ID = 2;
    private static final String NAME = "name";
    private static final long HID = 3;

    protected Map<String, Object> getExpectedData() {
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("pageType", TYPE);
        expectedData.put("id", ID);
        expectedData.put("revId", REVISION_ID);
        expectedData.put("name", NAME);
        expectedData.put("hid", HID);
        return expectedData;
    }

    protected Signal getSignalForTest() {
        Page page = new Page();
        page.setDocumentDescription(new DocumentDescription(null, TYPE), 0, 0);
        page.setId(ID);
        page.setRevisionId(REVISION_ID);
        page.setName(NAME);
        return new InvalidHidInPageSignalBuilder().withPage(page).withHid(HID).build();
    }
}
