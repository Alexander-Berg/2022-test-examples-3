package ru.yandex.direct.api.v5.entity.adextensions.converter;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import com.yandex.direct.api.v5.adextensions.AdExtensionFieldEnum;
import com.yandex.direct.api.v5.adextensions.AdExtensionsSelectionCriteria;
import com.yandex.direct.api.v5.adextensions.CalloutFieldEnum;
import com.yandex.direct.api.v5.adextensions.GetRequest;
import com.yandex.direct.api.v5.general.ExtensionStatusSelectionEnum;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.adextensions.container.GetFieldName;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.DateTimeUtils.MOSCOW_TIMEZONE;

public class GetRequestConverterExtractFieldNamesTest {

    private GetRequestConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new GetRequestConverter(MOSCOW_TIMEZONE);
    }

    @Test
    public void extractFieldNames_Empty_ToEmpty() throws Exception {
        GetRequest req = createGetRequestWithFields(emptyList(), emptyList());
        req.getSelectionCriteria().setStatuses(singletonList(ExtensionStatusSelectionEnum.MODERATION));
        Set<GetFieldName> fieldNames = converter.extractFieldNames(req);
        assertThat(fieldNames).isEmpty();
    }

    @Test
    public void extractFieldNames_AllValues_ToAllValues() throws Exception {
        GetRequest req = createGetRequestWithFields(
                EnumSet.allOf(AdExtensionFieldEnum.class),
                EnumSet.allOf(CalloutFieldEnum.class));
        req.getSelectionCriteria().setStatuses(singletonList(ExtensionStatusSelectionEnum.MODERATION));
        Set<GetFieldName> fieldNames = converter.extractFieldNames(req);
        assertThat(fieldNames).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(GetFieldName.class));
    }

    @Test
    public void extractFieldNames_CalloutText_ShouldAddCalloutField() throws Exception {
        GetRequest req = createGetRequestWithFields(
                emptyList(),
                singleton(CalloutFieldEnum.CALLOUT_TEXT));
        req.getSelectionCriteria().setStatuses(singletonList(ExtensionStatusSelectionEnum.MODERATION));
        Set<GetFieldName> fieldNames = converter.extractFieldNames(req);
        assertThat(fieldNames).containsExactlyInAnyOrder(GetFieldName.CALLOUT, GetFieldName.CALLOUT_TEXT);
    }

    private GetRequest createGetRequestWithFields(Collection<AdExtensionFieldEnum> fields,
            Collection<CalloutFieldEnum> calloutFields)
    {
        return new GetRequest()
                .withSelectionCriteria(new AdExtensionsSelectionCriteria())
                .withFieldNames(fields)
                .withCalloutFieldNames(calloutFields);
    }
}
