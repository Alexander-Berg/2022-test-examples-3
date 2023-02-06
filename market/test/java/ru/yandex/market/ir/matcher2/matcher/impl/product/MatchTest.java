package ru.yandex.market.ir.matcher2.matcher.impl.product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.market.ir.matcher2.matcher.MatchType;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Dimension;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Infix;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Level;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Match;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.InOfferEntry;

import static org.junit.Assert.assertEquals;


/**
 * @author a-shar.
 */
public class MatchTest {

    private static final int LOCAL_VENDOR_ID = 100500;
    private static final int MODEL_ID = 154;
    private static final int GLOBAL_VENDOR_ID = 5001000;
    private static final int MODIFICATION_ID = 154451;
    private static final List<MatchType> MATCH_TYPES = ImmutableList.of(MatchType.GOOD_ID_MATCH,
        MatchType.MODIFICATION_MATCH,
        MatchType.MODEL_OK_MATCH,
        MatchType.BLOCK_WORD_MATCH
    );

    @Test
    public void test_matchOrder() {
        List<Match> matches = buildMatches();
        Collections.sort(matches);

        for (int i = 0, match_typesSize = MATCH_TYPES.size(); i < match_typesSize; i++) {
            MatchType matchType = MATCH_TYPES.get(i);
            assertEquals(matchType, matches.get(i).getMatchedType());
        }
    }

    private List<Match> buildMatches() {
        List<Match> matches = new ArrayList<>();
        Match.MatchBuilder matchBuilder = Match.newBuilder();
        matchBuilder.setCategoryBlockWords(Infix.EMPTY_ARRAY);
        matchBuilder.setLocalVendorId(LOCAL_VENDOR_ID);
        matchBuilder.setGlobalVendorId(GLOBAL_VENDOR_ID);
        Level vendorLevel = new Level(LOCAL_VENDOR_ID, Infix.GOOD_ID_INFIX, Infix.EMPTY_ARRAY, "vendor");
        Infix goodIdInfix = new Infix(InOfferEntry.GOOD_ID_ENTRY, Collections.singletonList("GOOD_ID"), null);
        Level lastLevel = new Level(MODEL_ID, goodIdInfix, Infix.EMPTY_ARRAY, "");
        matchBuilder.setMatchedType(MatchType.GOOD_ID_MATCH);
        matchBuilder.setHierarchy(new Level[]{vendorLevel, lastLevel});
        matchBuilder.setMatchMethod(Match.MatchMethod.GOOD_ID);
        matches.add(matchBuilder.build());

        matchBuilder.clear();
        matchBuilder.setMatchedType(MatchType.BLOCK_WORD_MATCH);
        matchBuilder.setLocalVendorId(0);
        matchBuilder.setGlobalVendorId(0);
        matchBuilder.setHierarchy(Level.EMPTY_ARR);
        Infix[] infixes = {new Infix(InOfferEntry.DUMMY_ENTRY, Collections.singletonList("BLOCK"), null)};
        matchBuilder.setCategoryBlockWords(infixes);
        matchBuilder.setDimension(Dimension.EMPTY_DIMENSION);
        matchBuilder.setMatchMethod(Match.MatchMethod.ALIAS);
        matches.add(matchBuilder.build());

        matchBuilder.clear();
        matchBuilder.setMatchedType(MatchType.MODEL_OK_MATCH);
        matchBuilder.setLocalVendorId(LOCAL_VENDOR_ID);
        matchBuilder.setGlobalVendorId(GLOBAL_VENDOR_ID);
        Level modelLevel = new Level(MODEL_ID, Infix.DUMMY_INFIX, Infix.EMPTY_ARRAY, "");
        matchBuilder.setHierarchy(new Level[]{vendorLevel, modelLevel});
        matchBuilder.setDimension(Dimension.EMPTY_DIMENSION);
        matchBuilder.setMatchMethod(Match.MatchMethod.ALIAS);
        matches.add(matchBuilder.build());

        matchBuilder.clear();
        matchBuilder.setMatchedType(MatchType.MODIFICATION_MATCH);
        matchBuilder.setLocalVendorId(LOCAL_VENDOR_ID);
        matchBuilder.setGlobalVendorId(GLOBAL_VENDOR_ID);
        Level modificationLevel = new Level(MODIFICATION_ID, Infix.DUMMY_INFIX, Infix.EMPTY_ARRAY, "");
        matchBuilder.setHierarchy(new Level[]{vendorLevel, modelLevel, modificationLevel});
        matchBuilder.setDimension(Dimension.EMPTY_DIMENSION);
        matchBuilder.setMatchMethod(Match.MatchMethod.ALIAS);
        matches.add(matchBuilder.build());
        return matches;
    }
}
