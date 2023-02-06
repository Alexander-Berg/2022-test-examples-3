package ru.yandex.market.mboc.common.masterdata.services.iris.proto;

import java.util.Random;

import com.google.protobuf.ByteString;
import io.github.benas.randombeans.api.Randomizer;

public class ByteStringRandomizer implements Randomizer<ByteString> {

    private static final int MAX_BYTE_STRING_LENGTH = 128;

    private final Random random;

    public ByteStringRandomizer(Random random) {
        this.random = random;
    }

    @Override
    public ByteString getRandomValue() {
        final int n = random.nextInt(MAX_BYTE_STRING_LENGTH);
        byte[] bytes = new byte[n];
        random.nextBytes(bytes);
        return ByteString.copyFrom(bytes);
    }

}
