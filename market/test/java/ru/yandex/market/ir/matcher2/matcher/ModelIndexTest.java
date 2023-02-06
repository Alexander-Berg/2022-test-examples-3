package ru.yandex.market.ir.matcher2.matcher;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Match;
import ru.yandex.market.ir.matcher2.matcher.be.OfferCopy;
import ru.yandex.market.ir.matcher2.matcher.category.CategoryLoadingException;
import ru.yandex.market.ir.matcher2.matcher.category.patterns.CategoryPattern;
import ru.yandex.market.ir.matcher2.matcher.utils.CategoryPatternUtils;
import ru.yandex.market.ir.matcher2.matcher.utils.FileUtil;
import ru.yandex.market.mbo.export.MboParameters;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class ModelIndexTest {
    private static final int HID = 1;
    private static final Consumer<MboParameters.Category.Builder> NOP = o -> {};

    private CategoryPattern categoryPattern;


    @Before
    public void setUp() throws IOException, CategoryLoadingException {
        String categoryFileName = FileUtil.getAbsolutePath("/proto_dump/parameters_1.json");
        String modelFileName = FileUtil.getAbsolutePath("/proto_dump/all_models_1.json");

        categoryPattern = CategoryPatternUtils.buildCategoryPattern(categoryFileName, modelFileName, NOP);
    }

    @Test
    public void testCategoryAttr() {
        Assert.assertEquals(HID, categoryPattern.getHid());
    }

    @Test
    public void testMatching() {
        Match match = categoryPattern.match(OfferCopy.newBuilder().setTitle("Маяк Турист Дерево-пластик 100").build());
        Assert.assertEquals(MatchType.VENDOR_MATCH, match.getMatchedType());
        Assert.assertEquals("Маяк", match.getHierarchy()[0].getName());

        match = categoryPattern.match(OfferCopy.newBuilder().setTitle("Маяк Турист Дерево-пластик 200").build());
        Assert.assertEquals(MatchType.MODEL_TASK_MATCH, match.getMatchedType());
        Assert.assertEquals("Маяк", match.getHierarchy()[0].getName());
        Assert.assertEquals("Турист Дерево-пластик 200", match.getHierarchy()[1].getName());

        match = categoryPattern.match(OfferCopy.newBuilder()
                .setTitle("Маяк second alias for Дерево-пластик 200").build()
        );
        Assert.assertEquals(MatchType.MODEL_TASK_MATCH, match.getMatchedType());
        Assert.assertEquals("Маяк", match.getHierarchy()[0].getName());
        Assert.assertEquals("Турист Дерево-пластик 200", match.getHierarchy()[1].getName());

        match = categoryPattern.match(OfferCopy.newBuilder().setTitle("Маяк Турист Дерево-пластик 300").build());
        Assert.assertEquals(MatchType.VENDOR_MATCH, match.getMatchedType());

        match = categoryPattern.buildMatchById(1805860022,
            MatchType.GOOD_ID_MATCH,
            "",
            Match.MatchMethod.MODEL_ID,
            Matcher.SourceIndex.GOOD_ID_VALUE
            );
        Assert.assertEquals(MatchType.GOOD_ID_MATCH, match.getMatchedType());
        Assert.assertEquals(Match.MatchMethod.MODEL_ID, match.getMatchMethod());
        Assert.assertEquals("Маяк", match.getHierarchy()[0].getName());
        Assert.assertTrue(match.getHierarchy()[1].getName().isEmpty());
        Assert.assertEquals(1805860022, match.getHierarchy()[1].getMatchedId());
    }


    @Test
    public void testVendorCodeMatching() {
        Match match = categoryPattern.match(OfferCopy.newBuilder().setTitle("Маяк").setVendorCode("0023455").build());
        Assert.assertEquals(MatchType.SUPER_PARAM_MATCH, match.getMatchedType());
        Assert.assertEquals("Маяк", match.getHierarchy()[0].getName());

        match = categoryPattern.match(OfferCopy.newBuilder().setTitle("Маяк 1123455").build());
        Assert.assertEquals(MatchType.VENDOR_MATCH, match.getMatchedType());
    }
}
