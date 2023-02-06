package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.List;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.ArrayOfLong;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertLibraryMinusKeywordsToExternal;

public class GetResponseConverterConvertLibraryMinusKeywordsToExternalTest {

    @Test
    public void convertLibraryMinusKeywordsToExternal_NotEmptyList() {
        List<Long> expected = asList(1L, 4L, 23L);
        List<Long> libraryMinusKeywords = convertLibraryMinusKeywordsToExternal(expected).getValue().getItems();
        assertThat(libraryMinusKeywords).containsExactly(expected.toArray(new Long[0]));
    }

    @Test
    public void convertLibraryMinusKeywordsToExternal_EmptyListOfStringsGiven() {
        JAXBElement<ArrayOfLong> result = convertLibraryMinusKeywordsToExternal(emptyList());
        assertThat(result.isNil()).isTrue();
    }

    @Test
    public void convertLibraryMinusKeywordsToExternal_NullGiven() {
        JAXBElement<ArrayOfLong> result = convertLibraryMinusKeywordsToExternal(null);
        assertThat(result.isNil()).isTrue();
    }
}
