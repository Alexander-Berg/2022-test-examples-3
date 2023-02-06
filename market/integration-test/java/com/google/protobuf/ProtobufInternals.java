package com.google.protobuf;

import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ProtobufInternals {

    private ProtobufInternals() {
        throw new UnsupportedOperationException();
    }

    // FIXME DELIVERY-28768 грязный хак для обхода package-private доступа к IntList
    @Nonnull
    public static Internal.IntList randomList(Random random, int sizeFrom, int sizeTo) {
        Internal.IntList result = ProtobufLists.newIntList();
        int size = sizeFrom + random.nextInt(sizeTo - sizeFrom);
        random.ints(size).forEach(result::addInt);
        return result;
    }

}
