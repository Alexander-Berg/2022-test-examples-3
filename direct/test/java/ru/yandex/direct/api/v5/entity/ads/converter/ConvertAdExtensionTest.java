package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.Collection;

import com.yandex.direct.api.v5.adextensiontypes.AdExtensionTypeEnum;
import com.yandex.direct.api.v5.ads.AdExtensionAdGetItem;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static ru.yandex.direct.api.v5.entity.ads.converter.AdExtensionsConverter.convertAdExtensions;

public class ConvertAdExtensionTest {

    @Test
    public void convertAdExtension_NotEmptyList() {
        Collection<AdExtensionAdGetItem> items = convertAdExtensions(asList(1L, 2L, 3L));
        assertThat(items)
                .extracting("AdExtensionId", "Type")
                .contains(
                        tuple(1L, AdExtensionTypeEnum.CALLOUT),
                        tuple(2L, AdExtensionTypeEnum.CALLOUT),
                        tuple(3L, AdExtensionTypeEnum.CALLOUT));
    }

    @Test
    public void convertAdExtension_EmptyList() {
        assertThat(convertAdExtensions(emptyList())).isEmpty();
    }
}
