package io.github.benas.randombeans.randomizers.collection;

import io.github.benas.randombeans.api.Randomizer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.github.benas.randombeans.randomizers.number.ByteRandomizer.aNewByteRandomizer;
import static java.lang.Math.abs;

/**
 * @author s-ermakov
 */
public class MapRandomizerExt<K, V> implements Randomizer<Map<K, V>> {

    private final Function<V, K> keyGenerator;
    private final Randomizer<V> valueRandomizer;
    private final int nbElements;

    public MapRandomizerExt(Randomizer<K> keyRandomizer, Randomizer<V> valueRandomizer) {
        this(keyRandomizer, valueRandomizer, getRandomSize());
    }

    public MapRandomizerExt(Randomizer<K> keyRandomizer, Randomizer<V> valueRandomizer, int nbElements) {
        this(v -> keyRandomizer.getRandomValue(), valueRandomizer, nbElements);
    }

    public MapRandomizerExt(Function<V, K> keyGenerator, Randomizer<V> valueRandomizer) {
        this(keyGenerator, valueRandomizer, getRandomSize());
    }

    public MapRandomizerExt(Function<V, K> keyGenerator, Randomizer<V> valueRandomizer, int nbElements) {
        checkArguments(nbElements);
        this.keyGenerator = keyGenerator;
        this.valueRandomizer = valueRandomizer;
        this.nbElements = nbElements;
    }

    @Override
    public Map<K, V> getRandomValue() {
        Map<K, V> result = new HashMap<>(nbElements);
        for (int i = 0; i < nbElements; i++) {
            V randomValue = valueRandomizer.getRandomValue();
            result.put(keyGenerator.apply(randomValue), randomValue);
        }
        return result;
    }

    public static <K, V> MapRandomizerExt<K, V> aNewMapRandomizer(Function<V, K> keyGenerator,
                                                                  Randomizer<V> valueRandomizer, int nbElements) {
        return new MapRandomizerExt<>(keyGenerator, valueRandomizer, nbElements);
    }

    private void checkArguments(final int nbEntries) {
        if (nbEntries < 0) {
            throw new IllegalArgumentException("The number of entries to generate must be >= 0");
        }
    }

    private static int getRandomSize() {
        return abs(aNewByteRandomizer().getRandomValue()) + 1;
    }
}
