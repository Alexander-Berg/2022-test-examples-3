package ru.yandex.direct.web.entity.adgroup.controller.contentpromotionvideo;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;

import static java.util.Collections.singletonList;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForContentPromotionVideo;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class ContentPromotionVideoAdGroupControllerCopyTest extends ContentPromotionVideoAdGroupControllerTestBase {

    @Test
    public void saveContentPromotionVideoAdGroup_CopyAdGroupWithKeywordsManualStrategy_PricesCopied() {
        AdGroupInfo adGroupForCopy = steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo,
                ContentPromotionAdgroupType.VIDEO);
        BigDecimal price = BigDecimal.valueOf(100);
        KeywordInfo keyword = steps.keywordSteps()
                .createKeyword(adGroupForCopy, keywordForContentPromotionVideo()
                        .withPrice(price)
                        .withPriceContext(null));

        WebContentPromotionAdGroup complexContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupForCopy.getAdGroupId(), campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(keyword.getId())))
                        .withGeneralPrice(200.0);

        WebResponse webResponse = controller
                .saveContentPromotionVideoAdGroup(singletonList(complexContentPromotionVideoAdGroup),
                        campaignInfo.getCampaignId(), false, true, true, null);
        checkResponse(webResponse);

        List<AdGroup> adGroups = findAdGroups();
        assertThat("количество групп не соответствует ожидаемому", adGroups, hasSize(2));
        Long copiedAdGroupId = adGroups.get(0).getId().equals(adGroupForCopy.getAdGroupId())
                ? adGroups.get(1).getId()
                : adGroups.get(0).getId();

        Keyword expectedKeyword = keyword.getKeyword()
                .withPriceContext(BigDecimal.valueOf(200.0));
        checkKeywords(expectedKeyword, copiedAdGroupId);
    }

    private void checkKeywords(Keyword expected, Long adGroupId) {
        List<Keyword> keywords = findKeywordsInAdGroup(adGroupId);

        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyFields(newPath("priceContext"))
                .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer());
        assertThat("количество фраз не соответствует ожидаемому", keywords, hasSize(1));
        assertThat("цена во фразах скопирована верно", keywords.get(0),
                beanDiffer(expected).useCompareStrategy(strategy));
    }
}
