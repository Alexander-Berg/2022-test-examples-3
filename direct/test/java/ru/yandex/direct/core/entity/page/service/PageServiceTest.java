package ru.yandex.direct.core.entity.page.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.page.repository.PageRepository;
import ru.yandex.direct.core.entity.pages.model.Page;
import ru.yandex.direct.core.entity.pages.model.PageOption;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class PageServiceTest {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PageService pageService;

    private List<Page> internalAdPages;

    @Before
    public void before() {
        List<Page> testPages = List.of(
                new Page()
                        .withGroupNick("GroupNick 1")
                        .withDomain("Domain 1")
                        .withName("InternalAd page 1")
                        .withDescription(null)
                        .withId(1L)
                        .withOrigPageId(1L)
                        .withTargetType((short) 0)
                        .withSorting((short) 1)
                        .withOptions(Set.of(PageOption.INTERNAL_AD)),
                new Page()
                        .withGroupNick("GroupNick 2")
                        .withDomain("Domain 2")
                        .withName("InternalAd page 2")
                        .withDescription("Description 2")
                        .withId(2L)
                        .withOrigPageId(22L)
                        .withTargetType((short) 1)
                        .withSorting((short) 2)
                        .withOptions(Set.of(PageOption.INTERNAL_AD)),
                new Page()
                        .withGroupNick("GroupNick 3")
                        .withDomain("Domain 3")
                        .withName("Not internalAd page 3")
                        .withDescription("Description 3")
                        .withId(3L)
                        .withOrigPageId(2L)
                        .withTargetType((short) 1)
                        .withSorting((short) 2)
                        .withOptions(Collections.emptySet())
        );
        internalAdPages = testPages.subList(0, 2);

        pageRepository.updatePages(testPages);
    }


    @Test
    public void getAllInternalAdPages() {
        List<Page> pages = pageService.getAllInternalAdPages();

        assertThat(pages).containsAll(internalAdPages);
    }

}
