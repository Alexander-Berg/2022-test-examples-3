package ru.yandex.market.psku.postprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.LongStream;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class MemorizingLongGenerator {
    private List<Long> generated = new ArrayList<>();
    private PrimitiveIterator.OfLong generator;

    public MemorizingLongGenerator() {
        generator = LongStream.iterate(1L, i -> i + 1).iterator();
    }

    public long next() {
        long next = generator.next();
        generated.add(next);
        return next;
    }

    public List<Long> getGenerated() {
        return generated;
    }

    public void clearGenerated() {
        generated.clear();
    }
}
