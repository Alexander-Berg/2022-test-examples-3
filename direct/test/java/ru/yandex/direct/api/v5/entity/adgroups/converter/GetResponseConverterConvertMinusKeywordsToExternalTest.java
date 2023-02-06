package ru.yandex.direct.api.v5.entity.adgroups.converter;

import java.util.List;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.general.ArrayOfString;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertMinusKeywordsToExternal;

public class GetResponseConverterConvertMinusKeywordsToExternalTest {

    @Test
    public void convertMinusKeywordsToExternal_NotEmptyListOfStringsGiven() {
        List<String> expectedMinusKeywords = asList("очень", "остроумная", "фраза");
        List<String> minusKeywords = convertMinusKeywordsToExternal(expectedMinusKeywords).getValue().getItems();
        assertThat(minusKeywords).containsExactly(expectedMinusKeywords.toArray(new String[0]));
    }

    @Test
    public void convertMinusKeywordsToExternal_EmptyListOfStringsGiven() {
        JAXBElement<ArrayOfString> result = convertMinusKeywordsToExternal(emptyList());
        assertThat(result.isNil()).isTrue();
    }

    @Test
    public void convertMinusKeywordsToExternal_NullGiven() {
        JAXBElement<ArrayOfString> result = convertMinusKeywordsToExternal(null);
        assertThat(result.isNil()).isTrue();
    }
}
