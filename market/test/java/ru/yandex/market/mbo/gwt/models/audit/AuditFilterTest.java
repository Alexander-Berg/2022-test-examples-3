package ru.yandex.market.mbo.gwt.models.audit;

import org.junit.Test;
import ru.yandex.market.mbo.core.audit.AuditFilterConverter;

/**
 * @author yuramalinov
 * @created 23.12.2019
 */
public class AuditFilterTest {
    @Test
    public void testRequestTypeConversion() {
        // We really need only one-way conversion
        for (AuditFilter.RequestType requestType : AuditFilter.RequestType.values()) {
            // Just don't fail.
            AuditFilterConverter.convert(requestType);
        }
    }

}
