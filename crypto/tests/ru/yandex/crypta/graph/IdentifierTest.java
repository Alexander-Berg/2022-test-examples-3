package ru.yandex.crypta.graph;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.crypta.lib.proto.identifiers.TEmail;
import ru.yandex.crypta.lib.proto.identifiers.TGenericID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IdentifierTest {
    @Test
    public void testGenerator() {
        Identifier.SetRandomSeed(123);
        Identifier id1 = Identifier.random("gaid");
        Identifier.SetRandomSeed(123);
        Identifier id2 = Identifier.random("gaid");
        System.out.println(id1.value);
        assertEquals(id1, id2);
    }

    @Test
    public void testFromProto() {
        TGenericID genericID = TGenericID.newBuilder()
                .setType(EIdType.EMAIL)
                .setEmail(TEmail.newBuilder().setLogin("asd").setDomain("ddd.x"))
                .build();
        Identifier identifier = Identifier.fromProto(genericID.toByteArray());

        assertEquals("asd@ddd.x", identifier.getValue());
        assertEquals(EIdType.EMAIL, identifier.getType());
    }

    @Test
    public void testToProto() throws InvalidProtocolBufferException {
        Identifier y = new Identifier("yandexuid", "5371137181547547840");
        byte[] bytes = y.toProto();
        TGenericID genericID = TGenericID.parseFrom(bytes);

        assertTrue(genericID.hasYandexuid());
        assertEquals(EIdType.YANDEXUID, genericID.getType());
        assertEquals(Long.parseUnsignedLong(y.getValue()), genericID.getYandexuid().getValue());
    }

    @Test
    public void testNext() throws InvalidProtocolBufferException {
        Identifier y = Identifier.random("yandexuid");
        System.out.println(y.value);
        byte[] bytes = y.toProto();
        TGenericID genericID = TGenericID.parseFrom(bytes);

        assertTrue(genericID.hasYandexuid());
        assertEquals(EIdType.YANDEXUID, genericID.getType());
        assertEquals(Long.parseUnsignedLong(y.getValue()), genericID.getYandexuid().getValue());

    }
}
