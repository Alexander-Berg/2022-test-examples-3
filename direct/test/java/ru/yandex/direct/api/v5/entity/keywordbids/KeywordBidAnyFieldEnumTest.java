package ru.yandex.direct.api.v5.entity.keywordbids;

import java.util.List;

import com.yandex.direct.api.v5.keywordbids.KeywordBidFieldEnum;
import com.yandex.direct.api.v5.keywordbids.KeywordBidNetworkFieldEnum;
import com.yandex.direct.api.v5.keywordbids.KeywordBidSearchFieldEnum;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class KeywordBidAnyFieldEnumTest {

    static List<KeywordBidFieldEnum> parametersForFromKeywordBidFieldEnum() {
        return StreamEx.of(KeywordBidFieldEnum.values())
                .toList();
    }

    @Test
    @Parameters
    public void fromKeywordBidFieldEnum(KeywordBidFieldEnum keywordBidFieldEnum) {
        assertThat(KeywordBidAnyFieldEnum.fromKeywordBidFieldEnum(keywordBidFieldEnum))
                .isNotNull();
    }

    static List<KeywordBidSearchFieldEnum> parametersForFromKeywordBidSearchFieldEnum() {
        return StreamEx.of(KeywordBidSearchFieldEnum.values())
                .toList();
    }

    @Test
    @Parameters
    public void fromKeywordBidSearchFieldEnum(KeywordBidSearchFieldEnum bidSearchFieldEnum) {
        assertThat(KeywordBidAnyFieldEnum.fromKeywordBidSearchFieldEnum(bidSearchFieldEnum))
                .isNotNull();
    }

    static List<KeywordBidNetworkFieldEnum> parametersForFromKeywordBidNetworkFieldEnum() {
        return StreamEx.of(KeywordBidNetworkFieldEnum.values())
                .toList();
    }

    @Test
    @Parameters
    public void fromKeywordBidNetworkFieldEnum(KeywordBidNetworkFieldEnum bidNetworkFieldEnum) {
        assertThat(KeywordBidAnyFieldEnum.fromKeywordBidNetworkFieldEnum(bidNetworkFieldEnum))
                .isNotNull();
    }
}
