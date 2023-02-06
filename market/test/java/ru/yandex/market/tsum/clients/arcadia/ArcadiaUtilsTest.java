package ru.yandex.market.tsum.clients.arcadia;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 30.01.18
 */
public class ArcadiaUtilsTest {
    @Test
    public void extractsReviewsFromString() {
        List<Integer> reviews = ArcadiaUtils.extractReview(
            "REVIEW:1,REVIEW:1 test REVIEW:  100, review:200 test Review: 300"
        );

        assertThat(reviews, equalTo(Arrays.asList(1, 100, 200, 300)));
    }
}
