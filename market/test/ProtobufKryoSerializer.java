package ru.yandex.market.mbo.lightmapper.test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;

import ru.yandex.market.mbo.protoutils.ProtoReflectionUtil;

public class ProtobufKryoSerializer extends Serializer<AbstractMessage> {
    private static final DefaultArraySerializers.ByteArraySerializer BYTE_ARRAY_SERIALIZER =
        new DefaultArraySerializers.ByteArraySerializer();

    @Override
    public void write(Kryo kryo, Output output, AbstractMessage object) {
        BYTE_ARRAY_SERIALIZER.write(kryo, output, object == null ? null : object.toByteArray());
    }

    @Override
    public AbstractMessage read(Kryo kryo, Input input, Class<? extends AbstractMessage> type) {
        byte[] bytes = BYTE_ARRAY_SERIALIZER.read(kryo, input, byte[].class);
        try {
            return (AbstractMessage) ProtoReflectionUtil.getDefaultInstance(type)
                .getParserForType()
                .parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AbstractMessage copy(Kryo kryo, AbstractMessage original) {
        return (AbstractMessage) original.toBuilder().build();
    }
}
