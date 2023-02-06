package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.VideoFilesServiceConfig;
import ru.yandex.canvas.model.video.files.VideoSource;
import ru.yandex.canvas.service.video.files.StockMoviesService;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Import(VideoFilesServiceConfig.class)
public class FindVideoByCategoriesTest {
    @Autowired
    StockMoviesService stockMoviesService;

    @Test
    public void findTest() {
        List<VideoSource> expected = Streams.stream(stockMoviesService.iterator())
                .filter(e -> e.getCategoryId().equals("200063733"))
                .collect(Collectors.toList());

        assertThat("We have some data", expected, Matchers.hasSize(4));

        StockMoviesService.SearchResult<VideoSource>
                found = stockMoviesService.searchVideo(Arrays.asList("200063733"), null, null, null, 0, 1000, null);

        assertThat("All categories found", expected,
                Matchers.containsInAnyOrder(expected.toArray(new VideoSource[expected.size()])));
    }

}
