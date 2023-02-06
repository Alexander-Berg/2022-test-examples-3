package ru.yandex.market.books.diff.dao;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.FullNameRandomizer;
import io.github.benas.randombeans.randomizers.SentenceRandomizer;
import io.github.benas.randombeans.randomizers.text.StringRandomizer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongSets;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alexander Kramarev (pochemuto@gmail.com)
 * @date 02.08.2018
 */
public class BookCardRandomizer implements Randomizer<BookCard> {
    private static final int SEED = 539098739;

    private final EnhancedRandom enhancedRandom;
    private final Randomizer<String> fullNameRandomizer;
    private final Randomizer<String> titleRandomizer;
    private final Randomizer<String> stringRandomizer;

    private BookCardRandomizer(long seed) {
        this.enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(seed + SEED)
            .build();
        this.fullNameRandomizer = FullNameRandomizer.aNewFullNameRandomizer(seed);
        this.titleRandomizer = SentenceRandomizer.aNewSentenceRandomizer(seed);
        this.stringRandomizer = StringRandomizer.aNewStringRandomizer(seed);
    }

    public static BookCardRandomizer aNewRandomizer(long seed) {
        return new BookCardRandomizer(seed);
    }

    @Override
    public BookCard getRandomValue() {
        return new BookCard(
            enhancedRandom.nextInt(),
            LongSets.singleton(enhancedRandom.nextLong()),
            LongSets.singleton(enhancedRandom.nextLong()),
            titleRandomizer.getRandomValue(),
            fullNameRandomizer.getRandomValue(),
            titleRandomizer.getRandomValue(),
            titleRandomizer.getRandomValue(),
            IntArrayList.wrap(enhancedRandom.ints().limit(2).toArray()),
            fullNameRandomizer.getRandomValue(),
            IntArrayList.wrap(enhancedRandom.ints().limit(2).toArray()),
            -1,
            enhancedRandom.nextInt(6),
            Stream.generate(stringRandomizer::getRandomValue).limit(5).collect(Collectors.toSet())
        );
    }
}
