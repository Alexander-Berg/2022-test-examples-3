package ru.yandex.crypta.graph2.matching.human.workflow;

import ru.yandex.crypta.graph.Identifier;
import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph2.model.matching.proto.VertexInComponent;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.random.Random2;

public class RandomIdGenerator {
    public Random2 random;

    public RandomIdGenerator(int seed) {
        Identifier.SetRandomSeed(seed);
        random = new Random2(seed);
    }

    public RandomIdGenerator() {
        this(123);
    }

    public Identifier randomIdentifier(String idType) {
        return Identifier.random(idType);
    }

    public Identifier randomIdentifier(EIdType idType) {
        return Identifier.random(idType);
    }

    public String randomStringValueId(String idType) {
        return Identifier.random(idType).getValue();
    }

    public String randomStringValueId(EIdType idType) {
        return Identifier.random(idType).getValue();
    }

    public String randomStringYuid() {
        return randomStringValueId(EIdType.YANDEXUID);
    }

    public String randomStringPhone() {
        return randomStringValueId(EIdType.PHONE);
    }

    public String randomStringGaid() {
        return randomStringValueId(EIdType.GAID);
    }

    public String randomStringIdfa() {
        return randomStringValueId(EIdType.IDFA);
    }

    public String randomStringEmail() {
        return randomStringValueId(EIdType.EMAIL);
    }

    public String randomStringLogin() {
        return randomStringValueId(EIdType.LOGIN);
    }

    public VertexInComponent randomVertexInComponent(String cryptaId, EIdType idType) {
        VertexInComponent vertex = VertexInComponent.newBuilder()
                .setId(randomStringValueId(idType))
                .setIdType(Soup.CONFIG.name(idType))
                .setCryptaId(cryptaId)
                .build();
        return vertex;
    }

    public Vertex randomVertex(EIdType idType) {
        return new Vertex(randomStringValueId(idType), idType);
    }

    public Vertex randomVertex() {
        try {
            return randomVertex(random.randomElement(EIdType.values()));
        } catch (RuntimeException e) {
            return randomVertex(EIdType.YANDEXUID);
        }
    }

    public String randomCryptaId() {
        return String.valueOf(random.nextNonNegativeInt());
    }
}
