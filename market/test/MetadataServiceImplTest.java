package ru.yandex.market.jmf.metadata.test;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.impl.MetadataServiceImpl;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.metainfo.MetaInfoService;

import static org.mockito.ArgumentMatchers.any;

public class MetadataServiceImplTest {

    MetadataService metadataService;

    @BeforeEach
    public void setUp() {
        var metainfoService = Mockito.mock(MetaInfoService.class);

        metadataService = new MetadataServiceImpl(metainfoService, null, null, List.of(), null, null,
                null, null);
    }

    // Тест для такой структуры
    //             mcA1 - (mcB1)
    //           /
    // root - lca
    //           \
    //             (mcA2)
    // ближайший общий предок в для mcA2 и mcB1 в таком случае - lca
    @Test
    public void getLowestCommonAncestorOrdinaryBinTree() {
        var root = createMetaclassMock("root", null);
        var expectedLca = createMetaclassMock("lca", root);
        var mcA1 = createMetaclassMock("mcA1", expectedLca);
        var mcA2 = createMetaclassMock("mcA2", expectedLca);
        var mcB1 = createMetaclassMock("mcB1", mcA1);

        var lca = metadataService.getLowestCommonAncestor(mcB1, mcA2, root.getFqn());

        Assertions.assertEquals(expectedLca.getTitle(), lca.getTitle());
    }

    @Test
    public void getLowestCommonAncestorOnlyRoot() {
        var root = createMetaclassMock("root", null);

        var lca = metadataService.getLowestCommonAncestor(root, root, root.getFqn());

        Assertions.assertEquals(root.getTitle(), lca.getTitle());
    }

    @Test
    public void getLowestCommonAncestorChain() {
        var root = createMetaclassMock("root", null);
        var mcA1 = createMetaclassMock("mcA1", root);
        var mcB1 = createMetaclassMock("mcB1", mcA1);

        var lca = metadataService.getLowestCommonAncestor(mcA1, mcB1, root.getFqn());

        Assertions.assertEquals(mcA1.getTitle(), lca.getTitle());
    }

    @Test
    public void getLowestCommonAncestorEquals() {
        var root = createMetaclassMock("root", null);
        var mcA1 = createMetaclassMock("mcA1", root);

        var lca = metadataService.getLowestCommonAncestor(mcA1, mcA1, root.getFqn());

        Assertions.assertEquals(mcA1.getTitle(), lca.getTitle());
    }

    private Metaclass createMetaclassMock(String name, Metaclass parent) {
        var metaclass = Mockito.mock(Metaclass.class);
        Mockito.when(metaclass.getFqn()).thenReturn(Fqn.of(name));
        Mockito.when(metaclass.getParent()).thenReturn(parent);
        Mockito.when(metaclass.hasParent()).thenReturn(null != parent);
        Mockito.when(metaclass.getTitle()).thenReturn(name);

        // не выбрасываем ошибку с отсутсвием родителя
        Mockito.when(metaclass.equalsOrDescendantOf(any())).thenReturn(true);

        return metaclass;
    }
}
