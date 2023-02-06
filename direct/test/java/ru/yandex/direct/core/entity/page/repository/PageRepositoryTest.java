package ru.yandex.direct.core.entity.page.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.pages.model.Page;
import ru.yandex.direct.core.entity.pages.model.PageOption;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PageRepositoryTest {

    @Autowired
    private DslContextProvider dslContextProvider;

    private PageRepository pageRepository;

    private List<Page> testPages;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        pageRepository = new PageRepository(dslContextProvider);

        testPages = new ArrayList<>();
        testPages.add(new Page()
                .withGroupNick("GroupNick 1")
                .withDomain("Domain 1")
                .withName("Name 1")
                .withDescription(null)
                .withId(1L)
                .withOrigPageId(1L)
                .withOptions(Collections.emptySet())
                .withTargetType((short) 0)
                .withSorting((short) 1));
        testPages.add(new Page()
                .withGroupNick("GroupNick 2")
                .withDomain("Domain 2")
                .withName("Name 2")
                .withDescription("Description 2")
                .withId(2L)
                .withOrigPageId(22L)
                .withOptions(Set.of(PageOption.INTERNAL_AD))
                .withTargetType((short) 1)
                .withSorting((short) 2));
        testPages.add(new Page()
                .withGroupNick("GroupNick 3")
                .withDomain("Domain 3")
                .withName("Name 3")
                .withDescription("Description 3")
                .withId(3L)
                .withOrigPageId(3L)
                .withOptions(Collections.emptySet())
                .withTargetType((short) 2)
                .withSorting((short) 3));
        testPages.add(new Page()
                .withGroupNick("GroupNick 4")
                .withDomain("Domain 4")
                .withName("Name 4")
                .withDescription("Description 4")
                .withId(4L)
                .withOrigPageId(44L)
                .withOptions(Set.of(PageOption.INTERNAL_AD))
                .withTargetType((short) 3)
                .withSorting((short) 4));
        pageRepository.updatePages(testPages);
    }

    /**
     * Тест: если взять из таблицы PAGES запросом объекты по id -> мы получим все объекты с id в запрошенном
     * диапазоне
     */
    @Test
    public void getPagesBetweenIds() {
        Set<Long> pagesId = Set.of(1L, 2L, 3L);
        testPages = testPages.stream()
                .filter(p -> pagesId.contains(p.getId()))
                .collect(Collectors.toList());
        Set<Page> pages = pageRepository.getPagesByIds(pagesId);
        assertThat(pages).containsAll(testPages);
    }

    /**
     * Тест: если отправить в таблицу PAGES существующие в ней объекты с измененными значениями ->
     * они все сохранятся в БД с новыми значениями
     */
    @Test
    public void updatePages() {
        List<Page> pages = new ArrayList<>();
        pages.add(new Page()
                .withGroupNick("GroupNick 1")
                .withDomain("changed Domain 1")
                .withName("Name 1")
                .withDescription("changed Description 1")
                .withId(1L)
                .withOrigPageId(11L)
                .withOptions(Collections.emptySet())
                .withTargetType((short) 5)
                .withSorting((short) 1));
        pages.add(new Page()
                .withGroupNick("changed GroupNick 2")
                .withDomain("Domain 2")
                .withName("Name 2")
                .withDescription(null)
                .withId(2L)
                .withOrigPageId(2L)
                .withOptions(Collections.emptySet())
                .withTargetType((short) 6)
                .withSorting((short) 2));
        pages.add(new Page()
                .withGroupNick("GroupNick 3")
                .withDomain("Domain 3")
                .withName("changed Name 3")
                .withDescription("Description 3")
                .withId(3L)
                .withOrigPageId(33L)
                .withOptions(Set.of(PageOption.INTERNAL_AD))
                .withTargetType((short) 7)
                .withSorting((short) 3));
        List<Long> pagesId = pages.stream()
                .map(Page::getId)
                .collect(Collectors.toList());

        pageRepository.updatePages(pages);
        Set<Page> pagesAfter = pageRepository.getPagesByIds(pagesId);
        assertThat(pages).containsAll(pagesAfter);
    }
}
