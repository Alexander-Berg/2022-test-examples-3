package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.List;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.ArrayOfLong;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertRestrictedRegionIdsToExternal;

public class GetResponseConverterConvertRestrictedRegionIdsToExternalTest {

    @Test
    public void convertRestrictedRegionIdsToExternal_NotEmptyListOfLingGiven() {
        List<Long> expectedRestrictedRegionIds = asList(187L, 983L);
        List<Long> restrictedRegionIds =
                convertRestrictedRegionIdsToExternal(expectedRestrictedRegionIds).getValue().getItems();
        assertThat(restrictedRegionIds).containsExactly(expectedRestrictedRegionIds.toArray(new Long[0]));
    }

    @Test
    public void convertRestrictedRegionIdsToExternal_EmptyListOfLingGiven() {
        JAXBElement<ArrayOfLong> result = convertRestrictedRegionIdsToExternal(emptyList());
        assertThat(result.isNil()).isTrue();
    }

    @Test
    public void convertRestrictedRegionIdsToExternal_NullGiven() {
        JAXBElement<ArrayOfLong> result = convertRestrictedRegionIdsToExternal(null);
        assertThat(result.isNil()).isTrue();
    }
}
