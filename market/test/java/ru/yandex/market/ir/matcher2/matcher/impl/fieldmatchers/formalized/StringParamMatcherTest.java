package ru.yandex.market.ir.matcher2.matcher.impl.fieldmatchers.formalized;

import java.util.Collections;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.matcher2.matcher.alternate.be.dao.FormalizedParamValue;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.fieldmatchers.formalized.StringParamMatcher;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.IMatchedIds;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.StringTokenizationFactory;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.tokenizers.Tokenization;
import ru.yandex.market.ir.matcher2.matcher.be.OfferCopy;
import ru.yandex.market.ir.matcher2.matcher.util.MapToSet;

import static org.junit.Assert.assertEquals;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class StringParamMatcherTest {
    private static final int PARAM_ID = 100500;
    private StringParamMatcher stringParamMatcher;
    private StringTokenizationFactory stringTokenizationFactory = new StringTokenizationFactory(
        false, false, false, false
    );


    @Before
    public void init() {
        MapToSet<FormalizedParamValue, Integer> paramValueHolders = MapToSet.newInstance();
        FormalizedParamValue formalizedParamValue = new FormalizedParamValue(1005001, 0.0,
            Collections.singleton("first"));
        paramValueHolders.put(formalizedParamValue, 1);
        formalizedParamValue = new FormalizedParamValue(1005002, 0.0, Collections.singleton("second"));
        paramValueHolders.put(formalizedParamValue, 1);
        stringParamMatcher = new StringParamMatcher(PARAM_ID, paramValueHolders);
    }

    @Test
    public void addMatchedValueEntries() {
        Map result = getMatchedValueEntryInt2ObjectMap("first");
        assertEquals(1, result.size());
        getMatchedValueEntryInt2ObjectMap("second");
        assertEquals(1, result.size());
    }

    private Int2IntMap getMatchedValueEntryInt2ObjectMap(String title) {
        OfferCopy offerCopy = OfferCopy.newBuilder().setTitle(title).build();
        Tokenization tokenization = stringTokenizationFactory.getTokenization(offerCopy);
        Int2IntMap result = new Int2IntOpenHashMap();
        tokenization.apply((baseInOfferEntry, tokens) ->
            stringParamMatcher.addMatchedValueEntries(result, tokens, IMatchedIds.CONTINUUM,
                (modId, parameterId) -> {
                    boolean x = modId == parameterId;
                }
            ));
        return result;
    }

}
