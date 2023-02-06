package ru.yandex.direct.model.generator.uf;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.model.generator.uf.UnionFindTestData.ITEMS;

@ParametersAreNonnullByDefault
public class UnionFindCountTest {

    private static final UnionFind<Integer> UF = UnionFindFactory.createUnionFind(ITEMS);

    @BeforeClass
    public static void init() {
        UnionFindTestData.connections().forKeyValue(UF::connect);
    }

    @Test
    public void quickUnion_Count() {
        int distinctFindResults = (int) ITEMS.stream().map(UF::find).distinct().count();

        assertThat(distinctFindResults).isEqualTo(UF.count());
    }

}
