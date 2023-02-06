package ru.yandex.market.dao;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Отчитывается о схожести, может помочь при отладке.
 */
class Similarity {
    final int pbSize;
    final int xmlSize;
    final double similarity;

    static <E> Similarity valueOfComparison(Set<E> pb, Set<E> xml) {
        int intersectionSize = Sets.intersection(pb, xml).size();
        int total = pb.size() + xml.size() - intersectionSize;
        double similarity = 1.0d * intersectionSize / total;
        return new Similarity(pb.size(), xml.size(), similarity);
    }

    private Similarity(int firstSize, int secondSize, double similarity) {
        this.pbSize = firstSize;
        this.xmlSize = secondSize;
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return String.format("Similarity{firstSize=%d, secondSize=%d, similarity=%.4f}", pbSize, xmlSize, similarity);
    }
}
