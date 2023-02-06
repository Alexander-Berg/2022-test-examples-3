package ru.yandex.market.aa.easy.kmax;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Disabled;

/**
 * @author antipov93.
 */
@Disabled
class KMaxCandidateTest extends KMaxTest implements KMax {

    @Override
    protected KMax solution() {
        return this;
    }

    @Override
    public Collection<Integer> kMax(List<Integer> elements, int k) {
        return null;
    }
}