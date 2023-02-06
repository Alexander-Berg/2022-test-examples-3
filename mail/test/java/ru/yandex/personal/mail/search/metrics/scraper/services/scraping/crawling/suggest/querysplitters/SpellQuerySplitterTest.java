package ru.yandex.personal.mail.search.metrics.scraper.services.scraping.crawling.suggest.querysplitters;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.personal.mail.search.metrics.scraper.metrics.basket.BasketQuery;
import ru.yandex.personal.mail.search.metrics.scraper.model.query.SearchQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellQuerySplitterTest {

    @Test
    void splitQueryDiff() {
        SpellQuerySplitter sqs = new SpellQuerySplitter();
        SearchQuery query = new BasketQuery("foo бар");
        SplitQuery sq = sqs.splitQuery(query);

        List<String> expected = query.getText().chars()
                .mapToObj(charValue -> String.valueOf((char) charValue))
                .collect(Collectors.toList());

        List<String> actual = StreamSupport.stream(sq.spliterator(), false)
                .map(QueryPart::getDiff)
                .collect(Collectors.toList());

        assertTrue(Iterables.elementsEqual(expected, actual));
    }

    @Test
    void splitQueryPart() {
        SpellQuerySplitter sqs = new SpellQuerySplitter();
        SearchQuery query = new BasketQuery("foo бар");
        SplitQuery sq = sqs.splitQuery(query);

        LinkedList<String> queryLetters = query.getText().chars()
                .mapToObj(charValue -> String.valueOf((char) charValue))
                .collect(Collectors.toCollection(LinkedList::new));

        StringBuilder builder = new StringBuilder();

        for (QueryPart qp : sq) {
            builder.append(queryLetters.removeFirst());
            assertEquals(builder.toString(), qp.getPart());
        }
    }
}
