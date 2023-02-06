package ru.yandex.market.mbo.gwt.client.pages.audit;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.utils.url.GwtUrlConverter;
import ru.yandex.market.mbo.gwt.client.utils.url.ParamsEncoderDecoderMock;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;

import java.util.Date;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class AuditUrlConverterTest {
    private static final long SEED = 12345;

    private GwtUrlConverter<AuditFilter> converter;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        converter = AuditUrlConverter.createConverter(new ParamsEncoderDecoderMock());
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(SEED)
            .overrideDefaultInitialization(true)
            // We aren't going to support lists in GWT, so just skip this.
            // It should work with single-valued getter/setter.
            .collectionSizeRange(0, 1)
            .build();
    }

    @Test
    public void testAllAuditParamsAreInSync() {
        for (int i = 0; i < 100; i++) {
            AuditFilter auditFilter = random.nextObject(AuditFilter.class,
                "startTimestamp", "finishTimestamp", "requestType", "nextPageKey");
            auditFilter.setStartDate(truncateDate(auditFilter.getStartDate()));
            auditFilter.setFinishDate(truncateDate(auditFilter.getFinishDate()));

            String params = converter.convert(auditFilter);
            AuditFilter converterFilter = converter.convert(params);

            Assertions.assertThat(converterFilter)
                .isEqualToComparingFieldByFieldRecursively(auditFilter);
        }
    }

    @Test
    public void testReactAudit() {
        Assertions.assertThat(AuditUrlConverter.toReactUrl(new AuditUrlConverter.AuditParams()))
            .isEqualTo("/ui/audit?hasFilters=1");

        Assertions.assertThat(AuditUrlConverter.toReactUrl(new AuditUrlConverter.AuditParams()
            .setEntityType(AuditAction.EntityType.MODEL_PARAM)
            .setEntityId(123L)
            .setCategoryId(231L)
            .setParameterId(1111L)))
            .isEqualTo(
                "/ui/audit?hasFilters=1&entityTypeList=MODEL_PARAM&entityId=123&parameterId=1111&categoryId=231");

        Assertions.assertThat(AuditUrlConverter.toReactUrl(new AuditUrlConverter.AuditParams()
            .setEntityType(AuditAction.EntityType.MODEL_PARAM)
            .setCategoryId(231L)
            .setParameterId(1111L)))
            .isEqualTo("/ui/audit?hasFilters=1&entityTypeList=MODEL_PARAM&parameterId=1111&categoryId=231");
    }

    private Date truncateDate(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtils.truncate(date, java.util.Calendar.DAY_OF_MONTH);
    }
}
