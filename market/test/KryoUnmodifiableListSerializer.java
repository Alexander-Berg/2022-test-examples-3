package ru.yandex.market.mbo.lightmapper.test;

import java.util.ArrayList;
import java.util.Collection;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;

/**
 * @author dmserebr
 * @date 04/10/2019
 */
public class KryoUnmodifiableListSerializer extends CollectionSerializer {
    @Override
    protected Collection createCopy(Kryo kryo, Collection original) {
        return new ArrayList();
    }
}
