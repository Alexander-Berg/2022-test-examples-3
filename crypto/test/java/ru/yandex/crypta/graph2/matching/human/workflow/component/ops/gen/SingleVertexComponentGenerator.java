package ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

public class SingleVertexComponentGenerator extends TestComponentGenerator {
    @Override
    public TestComponent generateComponent() {
        return new TestComponent(
                Cf.list(generator.randomVertex(EIdType.YANDEXUID)),
                Cf.list()
        );
    }
}
