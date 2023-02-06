package ru.yandex.direct.core.entity.domain.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.domain.model.MarketRating;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MarketRatingRepositoryTest {


    @Autowired
    private MarketRatingRepository marketRatingRepository;

    private static final LocalDateTime DATE = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);

    @Test
    public void testAddAll() {
        var rating1 = new MarketRating().withDomainId(1L).withRating(1L).withLastChange(DATE);
        var rating2 = new MarketRating().withDomainId(2L).withRating(2L).withLastChange(LocalDateTime.now());
        marketRatingRepository.addAll(List.of(rating1, rating2));

        var updatedRating2 = new MarketRating().withDomainId(2L).withRating(3L).withLastChange(DATE);
        var rating3 = new MarketRating().withDomainId(3L).withRating(3L).withLastChange(DATE);
        marketRatingRepository.addAll(List.of(updatedRating2, rating3));

        Map<Long, MarketRating> map = marketRatingRepository.getAll();
        assertThat(map)
                .containsEntry(1L, rating1)
                .containsEntry(2L, updatedRating2)
                .containsEntry(3L, rating3);
    }

    @Test
    public void testGetActual() throws Exception {

        final List<MarketRating> ratings = new ArrayList<>();
        ratings.add(new MarketRating().withDomainId(1L).withRating(-1L).withLastChange(DATE));
        ratings.add(new MarketRating().withDomainId(2L).withRating(2L).withLastChange(DATE));

        marketRatingRepository.addAll(ratings);

        Map<Long, MarketRating> map = marketRatingRepository.getActual();
        assertNull(map.get(1L));
        assertEquals(map.get(2L), new MarketRating().withDomainId(2L).withRating(2L).withLastChange(DATE));
    }
}
