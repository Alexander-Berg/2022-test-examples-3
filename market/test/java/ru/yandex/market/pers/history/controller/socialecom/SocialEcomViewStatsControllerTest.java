package ru.yandex.market.pers.history.controller.socialecom;

import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.history.MockedDbTest;
import ru.yandex.market.pers.history.mvc.ViewStatisticsMvcMocks;
import ru.yandex.market.pers.history.socialecom.dto.ViewStatDto;
import ru.yandex.market.pers.history.socialecom.model.UserType;
import ru.yandex.market.pers.history.socialecom.model.ViewStatistics;
import ru.yandex.market.pers.history.socialecom.service.ViewStatsService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SocialEcomViewStatsControllerTest extends MockedDbTest {

    @Autowired
    private ViewStatsService service;

    @Autowired
    private ViewStatisticsMvcMocks mvcMocks;

    @Test
    public void testAuthorGetViews() {
        //create some views for authors
        mockAuthorViewsData();

        ViewStatDto dto = mvcMocks.getAuthorViews("123", UserType.BRAND.name());
        assertNotNull(dto);
        assertEquals(10L, (long) dto.getCount());

        //add new views
        mockAuthorViewsData();

        dto = mvcMocks.getAuthorViews("123", UserType.BRAND.name());
        assertNotNull(dto);
        assertEquals(20L, (long) dto.getCount());
    }

    @Test
    public void testPostGetViews() {
        //create some post views
        mockPostViewsData();

        ViewStatDto dto = mvcMocks.getPostViewSingle("123");
        assertNotNull(dto);
        assertEquals(10L, (long) dto.getCount());

        //add new views to existing
        mockPostViewsData();
        dto = mvcMocks.getPostViewSingle("123");
        assertNotNull(dto);
        assertEquals(20L, (long) dto.getCount());
    }

    @Test
    public void testPostGetViewsBulk() {
        mockPostViewsData();

        List<ViewStatDto> dtos = mvcMocks.getPostViewBulk("123", "124");

        assertTrue(dtos.stream().anyMatch(e -> Objects.equals(e.getCount(), 10L)));
        assertTrue(dtos.stream().anyMatch(e -> Objects.equals(e.getCount(), 11L)));

        mockPostViewsData();
        dtos = mvcMocks.getPostViewBulk("123", "124");
        assertTrue(dtos.stream().anyMatch(e -> Objects.equals(e.getCount(), 20L)));
        assertTrue(dtos.stream().anyMatch(e -> Objects.equals(e.getCount(), 22L)));
    }

    @Test
    public void testEmptyResponse() {
        List<ViewStatDto> postsBulk = mvcMocks.getPostViewBulk("123", "124");
        assertTrue(postsBulk.isEmpty());

        ViewStatDto authorStat = mvcMocks.getAuthorViews("123", UserType.BRAND.name());
        assertEquals("123", authorStat.getEntityId());
        assertEquals(0L, (long) authorStat.getCount());

        ViewStatDto postStat = mvcMocks.getPostViewSingle("999");
        assertEquals("999", postStat.getEntityId());
        assertEquals(0L, (long) postStat.getCount());
    }

    private void mockPostViewsData() {
        List<ViewStatistics> stats = List.of(
            new ViewStatistics("123", 10L),
            new ViewStatistics("124", 11L)
        );

        service.updatePostStats(stats);
    }

    private void mockAuthorViewsData() {
        List<ViewStatistics> stats = List.of(
            new ViewStatistics("123", UserType.BRAND, 10L),
            new ViewStatistics("124", UserType.BUSINESS,11L)
        );
        service.updateAuthorStats(stats);
    }
}
