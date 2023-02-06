package ru.yandex.market.ir.matcher2.matcher;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.FormalizedParameterCategoryMatcher;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Level;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Match;
import ru.yandex.market.ir.matcher2.matcher.be.OfferCopy;
import ru.yandex.market.ir.matcher2.matcher.be.formalized.ParamBag;
import ru.yandex.market.ir.matcher2.matcher.category.CategoryLoadingException;
import ru.yandex.market.ir.matcher2.matcher.category.patterns.CategoryPattern;
import ru.yandex.market.ir.matcher2.matcher.utils.CategoryPatternUtils;
import ru.yandex.market.ir.matcher2.matcher.utils.FileUtil;
import ru.yandex.market.mbo.export.MboParameters;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class FormalizedMatcherLogicTest {
    private static final int HID = 91532;
    private static final int FISHED_ID = 995536;
    private static final Consumer<MboParameters.Category.Builder> NOP = o -> {};

    private CategoryPattern categoryPattern;
    private CategoryPattern categoryPatternNoSplitter;


    @Before
    public void setUp() throws IOException, CategoryLoadingException {
        String categoryFileName = FileUtil.getAbsolutePath("/proto_dump/parameters_91532.json");
        String modelFileName = FileUtil.getAbsolutePath("/proto_dump/all_models_91532.json");

        categoryPattern = CategoryPatternUtils.buildCategoryPattern(categoryFileName, modelFileName, NOP);
        categoryPatternNoSplitter = CategoryPatternUtils.buildCategoryPattern(categoryFileName, modelFileName,
            builder -> builder.getParameterBuilderList().stream()
                .filter(p -> p.getXslName().equalsIgnoreCase("vendor"))
                .forEach(o -> o.setParamMatchingType(MboParameters.ParamMatchingType.NONE)));
    }

    @Test
    public void testCategoryAttr() {
        Assert.assertEquals(HID, categoryPattern.getHid());
    }

    @Test
    public void testAliasMatching() {
        OfferCopy.Builder offerBuilder = OfferCopy.newBuilder()
            .setTitle("fisher")
            .setUseFormalizedMatcher(true);
        Match match = categoryPattern.match(offerBuilder.build());
        Assert.assertEquals(MatchType.VENDOR_MATCH, match.getMatchedType());
        Assert.assertEquals("Fischer", match.getHierarchy()[0].getName());

        match = categoryPattern.match(OfferCopy.newBuilder().setTitle("fisher N38116 XS").build());
        Assert.assertEquals(MatchType.MODEL_TASK_MATCH, match.getMatchedType());
        Assert.assertEquals("Fischer", match.getHierarchy()[0].getName());
        Assert.assertEquals("Elegance My Style NIS 164", match.getHierarchy()[1].getName());
    }

    @Test
    public void testMandatoryParams() {
        ParamBag paramBag = createFisherVendorParamBag();

        paramBag.addOptionValue(13085029, 13085032);
        paramBag.addNumberValue(6268940, 0, 164);

        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(HID)
            .setTitle("LS Crown")
            .setParamBag(paramBag)
            .setUseFormalizedMatcher(true)
            .build();
        List<Match> matches = categoryPattern.multiMatch(offer);

        Assert.assertEquals(1, matches.size());
        Match firstMatch = matches.get(0);
        Assert.assertEquals(MatchType.FORMALIZED_PARAM_MATCH, firstMatch.getMatchedType());
        Assert.assertEquals(Matcher.MatchTarget.UNPUBLISHED_MODEL, firstMatch.getMatchTarget());
        Assert.assertEquals(2, firstMatch.getHierarchy().length);
        Assert.assertEquals("Fischer", firstMatch.getHierarchy()[0].getName());
        Assert.assertEquals("Elegance My Style NIS 164", firstMatch.getHierarchy()[1].getName());
        Level level = firstMatch.getHierarchy()[1];
        Assert.assertEquals(3, level.getParamEntries().length);
        Assert.assertEquals(1742701266, level.getMatchedId());

        ParamBag secondParamBag = createFisherVendorParamBag();

        secondParamBag.addOptionValue(13085029, 13085032);
        secondParamBag.addNumberValue(6268940, 0, 174);

        OfferCopy secondOffer = OfferCopy.newBuilder()
            .setCategoryId(HID)
            .setTitle("LS Crown")
            .setParamBag(secondParamBag)
            .setUseFormalizedMatcher(true)
            .build();
        List<Match> secondMatches = categoryPattern.multiMatch(secondOffer);

        Assert.assertEquals(1, secondMatches.size());
        Match secondMatch = secondMatches.get(0);
        Assert.assertEquals(MatchType.FORMALIZED_PARAM_MATCH, secondMatch.getMatchedType());
        Assert.assertEquals(Matcher.MatchTarget.UNPUBLISHED_MODEL, secondMatch.getMatchTarget());
        Assert.assertEquals(2, secondMatch.getHierarchy().length);
        Assert.assertEquals("Fischer", secondMatch.getHierarchy()[0].getName());
        Assert.assertEquals("Apollo NIS 174", secondMatch.getHierarchy()[1].getName());
        Level secondLevel = secondMatch.getHierarchy()[1];
        Assert.assertEquals(1742716155, secondLevel.getMatchedId());
    }

    @Test
    public void testConflictParamMatches() {
        ParamBag paramBag = createFisherVendorParamBag();

        paramBag.addOptionValue(13085029, 13085032);
        paramBag.addNumberValue(6268940, 0, 205);

        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(HID)
            .setTitle("fisher RIDGE WAX")
            .setParamBag(paramBag)
            .setUseFormalizedMatcher(true)
            .build();
        List<Match> matches = categoryPattern.multiMatch(offer);

        Assert.assertEquals(3, matches.size());

        Match vendorMatch = matches.get(0);
        Assert.assertEquals(MatchType.VENDOR_MATCH, vendorMatch.getMatchedType());
        Assert.assertTrue(vendorMatch.isDefinite());
        Assert.assertEquals(14661952, getMatchedId(vendorMatch));

        Match firstModelMatch = matches.get(1);
        Assert.assertEquals(MatchType.FORMALIZED_PARAM_MATCH, firstModelMatch.getMatchedType());
        Assert.assertFalse(firstModelMatch.isDefinite());
        Assert.assertEquals(1746660001, getMatchedId(firstModelMatch));

        Match secondModelMatch = matches.get(1);
        Assert.assertEquals(MatchType.FORMALIZED_PARAM_MATCH, secondModelMatch.getMatchedType());
        Assert.assertFalse(secondModelMatch.isDefinite());
        Assert.assertEquals(1746660001, getMatchedId(secondModelMatch));
    }

    @Test
    public void testEmptyMandatory() {
        ParamBag paramBag = createFisherVendorParamBag();

        paramBag.addOptionValue(13085029, 32);
        paramBag.addNumberValue(6268940, 0, 205000);

        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(HID)
            .setTitle("fisher Fake clone")
            .setParamBag(paramBag)
            .setUseFormalizedMatcher(true)
            .build();
        List<Match> matches = categoryPattern.multiMatch(offer);

        Assert.assertEquals(2, matches.size());
        Match modelMatch = matches.get(0);
        Assert.assertEquals(MatchType.MODEL_TASK_MATCH, modelMatch.getMatchedType());
        Assert.assertEquals(1746660001, getMatchedId(modelMatch));


        Match vendorMatch = matches.get(1);
        Assert.assertEquals(MatchType.VENDOR_MATCH, vendorMatch.getMatchedType());
        Assert.assertEquals(14661952, getMatchedId(vendorMatch));
    }

    @Test
    public void testAliasMatchingNoSplitParam() {
        OfferCopy.Builder offerBuilder = OfferCopy.newBuilder()
            .setTitle("fisher")
            .setUseFormalizedMatcher(true);
        Match match = categoryPatternNoSplitter.match(offerBuilder.build());
        Assert.assertEquals(MatchType.VENDOR_MATCH, match.getMatchedType());
        Assert.assertEquals("Fischer", match.getHierarchy()[0].getName());

        match = categoryPatternNoSplitter.match(
            OfferCopy.newBuilder()
                .setTitle("N38116 XS")
                .setUseFormalizedMatcher(true)
                .build());
        Assert.assertEquals(MatchType.MODEL_TASK_MATCH, match.getMatchedType());
        Assert.assertEquals("Fischer", match.getHierarchy()[0].getName());
        Assert.assertEquals(Match.MatchMethod.FORMALIZED, match.getMatchMethod());
        Assert.assertEquals("Elegance My Style NIS 164", match.getHierarchy()[1].getName());
    }

    @Test
    public void testMandatoryParamsNoSplitParam() {
        ParamBag paramBag = new ParamBag(2);

        paramBag.addOptionValue(13085029, 13085032);
        paramBag.addNumberValue(6268940, 0, 164);

        OfferCopy offer = OfferCopy.newBuilder()
            .setCategoryId(HID)
            .setTitle("LS Crown")
            .setParamBag(paramBag)
            .setUseFormalizedMatcher(true)
            .build();
        List<Match> matches = categoryPatternNoSplitter.multiMatch(offer);

        Assert.assertEquals(1, matches.size());
        Match firstMatch = matches.get(0);
        Assert.assertEquals(MatchType.FORMALIZED_PARAM_MATCH, firstMatch.getMatchedType());
        Assert.assertEquals(Matcher.MatchTarget.UNPUBLISHED_MODEL, firstMatch.getMatchTarget());
        Assert.assertEquals(2, firstMatch.getHierarchy().length);
        Assert.assertEquals("Fischer", firstMatch.getHierarchy()[0].getName());
        Assert.assertEquals("Elegance My Style NIS 164", firstMatch.getHierarchy()[1].getName());
        Level level = firstMatch.getHierarchy()[1];
        Assert.assertEquals(2, level.getParamEntries().length);
        Assert.assertEquals(1742701266, level.getMatchedId());

        ParamBag secondParamBag = createFisherVendorParamBag();

        secondParamBag.addOptionValue(13085029, 13085032);
        secondParamBag.addNumberValue(6268940, 0, 174);

        OfferCopy secondOffer = OfferCopy.newBuilder()
            .setCategoryId(HID)
            .setTitle("LS Crown")
            .setParamBag(secondParamBag)
            .setUseFormalizedMatcher(true)
            .build();
        List<Match> secondMatches = categoryPatternNoSplitter.multiMatch(secondOffer);

        Assert.assertEquals(1, secondMatches.size());
        Match secondMatch = secondMatches.get(0);
        Assert.assertEquals(MatchType.FORMALIZED_PARAM_MATCH, secondMatch.getMatchedType());
        Assert.assertEquals(Matcher.MatchTarget.UNPUBLISHED_MODEL, secondMatch.getMatchTarget());
        Assert.assertEquals(2, secondMatch.getHierarchy().length);
        Assert.assertEquals("Fischer", secondMatch.getHierarchy()[0].getName());
        Assert.assertEquals("Apollo NIS 174", secondMatch.getHierarchy()[1].getName());
        Level secondLevel = secondMatch.getHierarchy()[1];
        Assert.assertEquals(1742716155, secondLevel.getMatchedId());
    }

    private static ParamBag createFisherVendorParamBag() {
        ParamBag paramBag = new ParamBag(1);
        paramBag.addOptionValue(FormalizedParameterCategoryMatcher.VENDOR_PARAMETER_ID, FISHED_ID);
        return paramBag;
    }

    private static int getMatchedId(Match match) {
        return match.getHierarchy()[match.getHierarchy().length - 1].getMatchedId();
    }
}
