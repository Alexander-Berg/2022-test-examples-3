package ru.yandex.common.mining.bd;

import javax.annotation.Nonnull;
import org.w3c.dom.Element;
import ru.yandex.common.mining.bd.domex.DomExtractor;

import java.util.*;

/**
 * Created on 14:33:27 05.01.2008
*
* @author jkff
*/
public class UnionExtractor extends DomExtractor {
    private final List<DomExtractor> list;

    public UnionExtractor(DomExtractor... parts) {
        list = Arrays.asList(parts);
    }
    public UnionExtractor(List<DomExtractor> parts) {
        list = new ArrayList<DomExtractor>(parts);
    }

    public List<Element> extractElements(@Nonnull Element block) {
        Set<Element> res = new LinkedHashSet<Element>();
        for (DomExtractor x : list) {
            res.addAll(x.extractElements(block));
        }
        return new ArrayList<Element>(res);
    }
}
