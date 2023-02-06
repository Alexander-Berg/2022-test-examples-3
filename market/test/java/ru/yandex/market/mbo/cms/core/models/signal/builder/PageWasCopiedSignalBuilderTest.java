package ru.yandex.market.mbo.cms.core.models.signal.builder;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.signal.model.Signal;
import ru.yandex.market.mbo.cms.signal.model.SignalConstants;

public class PageWasCopiedSignalBuilderTest extends PageSignalBuilderAbstractTest {

    private static final String TYPE = "type";
    private static final long ID = 1;
    private static final long REVISION_ID = 2;
    private static final String NAME = "name";

    private static final String TYPE2 = "type2";
    private static final long ID2 = 21;
    private static final long REVISION_ID2 = 22;
    private static final String NAME2 = "name";

    protected Map<String, Object> getExpectedData() {
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put(SignalConstants.PAGE_TYPE_FIELD_NAME, TYPE);
        expectedData.put(SignalConstants.ID_FIELD_NAME, ID);
        expectedData.put(SignalConstants.REV_ID_FIELD_NAME, REVISION_ID);
        expectedData.put(SignalConstants.NAME_FIELD_NAME, NAME);
        expectedData.put(SignalConstants.COPIED_ID_FIELD_NAME, ID2);
        expectedData.put(SignalConstants.COPIED_REV_ID_FIELD_NAME, REVISION_ID2);
        expectedData.put(SignalConstants.COPIED_NAME_FIELD_NAME, NAME2);
        return expectedData;
    }

    protected Signal getSignalForTest() {
        Page originPage = new Page();
        originPage.setDocumentDescription(new DocumentDescription(null, TYPE), 0, 0);
        originPage.setId(ID);
        originPage.setRevisionId(REVISION_ID);
        originPage.setName(NAME);

        Page copiedPage = new Page();
        copiedPage.setDocumentDescription(new DocumentDescription(null, TYPE2), 0, 0);
        copiedPage.setId(ID2);
        copiedPage.setRevisionId(REVISION_ID2);
        copiedPage.setName(NAME2);

        return new PageWasCopiedSignalBuilder()
            .withOriginalPage(originPage)
            .withCopiedPage(copiedPage)
            .build();
    }
}
