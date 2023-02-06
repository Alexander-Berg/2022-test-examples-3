package ru.yandex.direct.model.generator.uf;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.model.generator.uf.UnionFindTestData.ITEMS;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class UnionFindConnectedTest {

    private static final UnionFind<Integer> UF = UnionFindFactory.createUnionFind(ITEMS);

    @Parameter
    public Integer left;

    @Parameter(1)
    public Integer right;

    @Parameter(2)
    public boolean expected;

    @BeforeClass
    public static void init() {
        UnionFindTestData.connections().forKeyValue(UF::connect);
    }

    @Parameters
    public static Iterable<Object[]> params() {
        return UnionFindTestData.connected();
    }

    @Test
    public void quickUnion_Connected() {
        assertThat(UF.connected(left, right)).isEqualTo(expected);
    }

}
