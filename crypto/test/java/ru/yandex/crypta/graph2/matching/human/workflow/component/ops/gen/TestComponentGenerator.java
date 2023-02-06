package ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen;

import ru.yandex.crypta.graph2.matching.human.workflow.RandomIdGenerator;

public abstract class TestComponentGenerator {
    protected RandomIdGenerator generator;

    TestComponentGenerator(Integer seed) {
        generator = new RandomIdGenerator(seed);
    }

    TestComponentGenerator() {
        this(4);
    }

    abstract TestComponent generateComponent();

}
