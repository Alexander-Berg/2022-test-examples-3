package ru.yandex.direct.jobs.bannersystem.dataimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.page.repository.PageRepository;
import ru.yandex.direct.core.entity.pages.model.Page;
import ru.yandex.direct.core.entity.pages.model.PageOption;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.bannersystem.dataimport.ImportPagesJob.convertDomain;
import static ru.yandex.direct.jobs.bannersystem.dataimport.ImportPagesJob.createPage;

@JobsTest
@ExtendWith(SpringExtension.class)
class ImportPagesJobTest {

    @Autowired
    private YtCluster ytCluster;

    @Mock
    private YtProvider ytProvider;
    @Mock
    private YtOperator ytOperator;
    @Mock
    private PageRepository pageRepository;

    private ImportPagesJob job;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        job = new ImportPagesJob(ytProvider, ytCluster, pageRepository);
        when(ytProvider.getOperator(any())).thenReturn(ytOperator);
        when(ytOperator.exists(any())).thenReturn(true);
    }

    /**
     * Тест: если в метод saveChanges передать пустой список -> в базу PAGES в PpcDict ничего не передастся
     */
    @Test
    void saveChangesWithEmptyList() {
        job.saveChanges(Collections.emptyList());
        verify(pageRepository, never()).updatePages(anyList());
    }

    /**
     * Тест: если в метод createPage передать заданные значения -> метод вернет объект Page с правильными значениями
     */
    @Test
    void checkConvertor_nameWithPrefixAndSuffix() {
        Page page = createPage(1L, 0L, "http://DOMAIN.ru/class", "descr", 0L, true, false, false);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getId())
                .withDomain("domain.ru")
                .withDescription("descr")
                .withSorting((short) 0)
                .withTargetType((short) 0)
                .withName("Яндекс")
                .withOptions(Collections.emptySet())
                .withGroupNick("yandex.0");
        assertThat(page).isEqualTo(pageExpected);
    }

    @Test
    void checkConvertor_nameWithPrefix() {
        Page page = createPage(2L, 2L, "HTTP://DOMAIN.RU", "descr", 0L, true, false, false);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getId())
                .withDomain("domain.ru")
                .withDescription("descr")
                .withSorting((short) 0)
                .withTargetType((short) 0)
                .withName("Яндекс")
                .withOptions(Collections.emptySet())
                .withGroupNick("yandex.0");
        assertThat(page).isEqualTo(pageExpected);
    }

    @Test
    void checkConvertor_nameWithSuffixWithoutDescr() {
        Page page = createPage(3L, 0L, "domain.ru/CLASS", null, 10L, true, false, false);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getId())
                .withDomain("domain.ru")
                .withDescription(null)
                .withSorting((short) 0)
                .withTargetType((short) 10)
                .withName("Яндекс")
                .withOptions(Collections.emptySet())
                .withGroupNick("yandex.0");
        assertThat(page).isEqualTo(pageExpected);
    }

    @Test
    void checkConvertor_notYaGroup() {
        Page page = createPage(4L, 0L, "domain.ru", "TestDescr", 3L, false, false, false);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getId())
                .withDomain("domain.ru")
                .withDescription("TestDescr")
                .withSorting((short) 9)
                .withTargetType((short) 3)
                .withName("domain.ru")
                .withOptions(Collections.emptySet())
                .withGroupNick("domain.ru.3");
        assertThat(page).isEqualTo(pageExpected);
    }

    @Test
    void checkConvertor_notYaGroupWithFullUrl() {
        Page page = createPage(5L, 0L, "HTTP://domain.ru/class3/class2/class1",
                "descr", 0L, false, false, false);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getId())
                .withDomain("domain.ru")
                .withDescription("descr")
                .withSorting((short) 9)
                .withTargetType((short) 0)
                .withName("domain.ru")
                .withOptions(Collections.emptySet())
                .withGroupNick("domain.ru.0");
        assertThat(page).isEqualTo(pageExpected);
    }

    @Test
    void checkConvertor_origPageIdNotEqualPageId() {
        Page page = createPage(6L, 66L, "HTTP://domain.ru/class3/class2/class1",
                "descr", 0L, false, false, false);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getOrigPageId())
                .withDomain("domain.ru")
                .withDescription("descr")
                .withSorting((short) 9)
                .withTargetType((short) 0)
                .withName("domain.ru")
                .withOptions(Collections.emptySet())
                .withGroupNick("domain.ru.0");
        assertThat(page).isEqualTo(pageExpected);
    }

    @Test
    void checkConvertor_yaPageIsTrue() {
        Page page = createPage(7L, 7L, "HTTP://domain.ru/class3/class2/class1",
                "descr", 0L, false, true, false);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getOrigPageId())
                .withDomain("domain.ru")
                .withDescription("descr")
                .withSorting((short) 9)
                .withTargetType((short) 0)
                .withName("domain.ru")
                .withOptions(Set.of(PageOption.INTERNAL_AD))
                .withGroupNick("domain.ru.0");
        assertThat(page).isEqualTo(pageExpected);
    }

    @Test
    void checkConvertor_distribAdvIsTrue() {
        Page page = createPage(8L, 8L, "HTTP://domain.ru/class3/class2/class1",
                "descr", 0L, false, false, true);
        Page pageExpected = new Page()
                .withId(page.getId())
                .withOrigPageId(page.getOrigPageId())
                .withDomain("domain.ru")
                .withDescription("descr")
                .withSorting((short) 9)
                .withTargetType((short) 0)
                .withName("domain.ru")
                .withOptions(Set.of(PageOption.INTERNAL_AD))
                .withGroupNick("domain.ru.0");
        assertThat(page).isEqualTo(pageExpected);
    }

    /**
     * Тест: если при вызове метода saveChanges отправить список из трех Page объектов:
     * первый существует в таблице Page в PpcDict с такими же значениями
     * второй существует в таблице с другими значениями
     * третьего нет в таблице -> в таблицу PAGES будут переданы только два объекта: второй и третий
     */
    @Test
    void checkSaveChanges() {
        Set<Page> pagesFromPpcDict = new HashSet<>();
        pagesFromPpcDict.add(new Page()
                .withId(1L)
                .withOrigPageId(1L)
                .withDomain("domain.ru")
                .withDescription("description")
                .withSorting((short) 0)
                .withTargetType((short) 10)
                .withName("Яндекс")
                .withGroupNick("yandex.0")
                .withOptions(Collections.emptySet()));
        pagesFromPpcDict.add(new Page()
                .withId(2L)
                .withOrigPageId(222L)
                .withDomain("site.ru")
                .withDescription(null)
                .withSorting((short) 0)
                .withTargetType((short) 10)
                .withName("Яндекс")
                .withGroupNick("yandex.0")
                .withOptions(Set.of(PageOption.INTERNAL_AD)));
        when(pageRepository.getPagesByIds(anyCollection())).thenReturn(pagesFromPpcDict);

        List<Page> pagesIn = new ArrayList<>();
        pagesIn.add(new Page()
                .withId(1L)
                .withOrigPageId(1L)
                .withDomain("domain.ru")
                .withDescription("description")
                .withSorting((short) 0)
                .withTargetType((short) 10)
                .withName("Яндекс")
                .withGroupNick("yandex.0")
                .withOptions(Collections.emptySet()));
        pagesIn.add(new Page()
                .withId(2L)
                .withOrigPageId(222L)
                .withDomain("domain.ru")
                .withDescription("description")
                .withSorting((short) 0)
                .withTargetType((short) 10)
                .withName("Яндекс")
                .withGroupNick("yandex.0")
                .withOptions(Set.of(PageOption.INTERNAL_AD)));
        pagesIn.add(new Page()
                .withId(3L)
                .withOrigPageId(3L)
                .withDomain("domain.ru")
                .withDescription(null)
                .withSorting((short) 0)
                .withTargetType((short) 10)
                .withName("Яндекс")
                .withGroupNick("yandex.0")
                .withOptions(Collections.emptySet()));
        int countChanged = job.saveChanges(pagesIn);

        List<Page> pagesCheck = new ArrayList<>();
        pagesCheck.add(pagesIn.get(2));
        pagesCheck.add(pagesIn.get(1));

        assertThat(countChanged).isEqualTo(2);
        verify(pageRepository).updatePages(contains(pagesCheck));
    }

    @Test
    void convertDomain_withHttp() {
        String expected = "domain.ru";
        String result = convertDomain("http://domAIN.Ru");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withHttpAndSlash() {
        String expected = "domain.ru";
        String result = convertDomain("htTP://Domain.ru/ok");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withHttps() {
        String expected = "domain.ru";
        String result = convertDomain("httpS://DOmain.ru");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withHttpsAndSlash() {
        String expected = "domain.ru";
        String result = convertDomain("httPS://DOmain.ru/ok");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withSpaces() {
        String expected = "domain.ru";
        String result = convertDomain("  domaIN.Ru ");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withHttpsAndSlashesAndParam() {
        String expected = "play.google.com";
        String result = convertDomain("https://play.google.com/store/apps/details?id=com.momento.cam");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withDescription() {
        String expected = "bm.igrulka.net";
        String result = convertDomain("bm.igrulka.net - Full Pack");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withSlashAndDescription() {
        String expected = "m.ya.ru";
        String result = convertDomain("m.ya.ru/ybrowser - Мобильный Яндекс.Браузер для iOS");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_withHttpAndSlashAndDescription() {
        String expected = "avito-browser.ru";
        String result = convertDomain("http://avito-browser.ru/ok - Лайт пак + Я.Браузер с avito.ru'");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_whenEmpty() {
        String expected = "";
        String result = convertDomain("");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertDomain_whenCantGetUrl() {
        String expected = "фштуоаущл-/укмаук";
        String result = convertDomain("фшТУоаУЩЛ-/УКМаук");
        assertThat(result).isEqualTo(expected);
    }

    private Collection<Page> contains(List<Page> pages) {
        return argThat(args -> CollectionUtils.containsAll(args, pages));
    }
}
