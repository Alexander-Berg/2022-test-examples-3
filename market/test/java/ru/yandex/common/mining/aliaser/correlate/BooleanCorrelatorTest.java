package ru.yandex.common.mining.aliaser.correlate;

import static org.junit.Assert.*;
import org.junit.Test;
import ru.yandex.common.mining.property.PropertyInfo;
import ru.yandex.common.mining.propkinds.Converter;
import ru.yandex.common.mining.propkinds.PropertyKind;
import static ru.yandex.common.util.collections.CollectionFactory.newList;
import ru.yandex.common.util.collections.nullable.NotNullPair;

import java.util.List;

/**
 * Created on 17:44:41 27.06.2008
 *
 * @author jkff
 */
public class BooleanCorrelatorTest {
    @Test
    public void testValueSimilarity() {
        BooleanCorrelator bc = new BooleanCorrelator();
        PropertyInfo pi = new PropertyInfo("prop", PropertyKind.bool());
        assertTrue(0.6 < bc.valueSimilarity(vals(
                ar(0,1,0,1,0,1,1,1,0),
                ar(0,1,0,1,1,1,0,1,0)),
                pi));

        // Two randoms
        assertTrue(0.2 > bc.valueSimilarity(vals(
                ar(0,1,0,1,0,1,1,1,0),
                ar(1,0,1,1,1,0,0,1,1)),
                pi));
    }

    @Test
    public void testMakeConverter() {
        BooleanCorrelator bc = new BooleanCorrelator();
        PropertyInfo pi = new PropertyInfo("prop", PropertyKind.bool());
        Converter c = bc.tryMakeConverter(vals(
                ar(0, 1, 0, 1, 0, 1, 1, 1, 0),
                ar(0, 1, 0, 1, 1, 1, 0, 1, 0)),
                pi, pi);
        assertNotNull(c);
        assertEquals("TRUE", c.convert("true"));

        c = bc.tryMakeConverter(vals(
                ar(0, 1, 0, 1, 0, 1, 1, 1, 0),
                ar(1, 0, 1, 0, 0, 0, 1, 0, 1)),
                pi, pi);
        assertNotNull(c);
//        assertEquals("false", c.convert("true"));
    }

    private static List<NotNullPair<String,String>> vals(Boolean[] xs, Boolean[] ys) {
        List<NotNullPair<String,String>> res = newList();
        for(int i = 0; i < xs.length; ++i) {
            if(xs[i] != null && ys[i] != null)
                res.add(new NotNullPair<String, String>(
                    String.valueOf(xs[i]),
                    String.valueOf(ys[i])));
        }
        return res;
    }
    private static Boolean[] ar(int... ns) {
        Boolean[] res = new Boolean[ns.length];
        for(int i = 0; i < ns.length; ++i)
            res[i] = ns[i]==0 ? false : ns[i]==1 ? true : null;
        return res;
    }
}
