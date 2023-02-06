package ru.yandex.market.mbo.cms.api.servlets.api;

import java.util.Iterator;

import org.eclipse.jetty.server.Server;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cms.api.dto.filter.FilterValue;
import ru.yandex.market.mbo.cms.api.dto.filter.ResourceFilter;
import ru.yandex.market.mbo.cms.api.servlets.bean.RequestHelper;
import ru.yandex.market.mbo.cms.core.utils.http.response.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author gilmulla
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@SuppressWarnings("magicnumber")
public class ResourceFiltersServletTest {

    //    @Configuration
//    @Import({CmsEditorApiIntegrationTestConf.class})
    static class ContextConfiguration {
    }

    @Autowired
    private Server server;
    @Autowired
    private RequestHelper requestHelper;

    //@Before
    public void before() throws Exception {
        server.start();
    }

    //@After
    public void after() throws Exception {
        server.stop();
    }

    //@Test
    public void testFilter() {
        ApiResponse<ResourceFilter> response = requestHelper.request(
                "/api/getResourceFilterList", ResourceFilter.class);
        assertThat(response.getError()).isNull();
        assertThat(response.getPagination()).isNull();
        assertThat(response.getRows()).isNotNull();

        assertThat(response.getRows()).hasSize(3);

        Iterator<ResourceFilter> iterator = response.getRows().iterator();

        ResourceFilter authorFilter = iterator.next();
        assertThat(authorFilter.getName()).isEqualTo("author");
        assertThat(authorFilter.getTitle()).isEqualTo("Автор");
        assertThat(authorFilter.getValues()).extracting(FilterValue::getId).containsExactly(
                "1", "2", "3");
        assertThat(authorFilter.getValues()).extracting(FilterValue::getTitle).containsExactly(
                "user1", "user2", "user3");

        ResourceFilter statusFilter = iterator.next();
        assertThat(statusFilter.getName()).isEqualTo("status");
        assertThat(statusFilter.getTitle()).isEqualTo("Статус");
        assertThat(statusFilter.getValues()).extracting(FilterValue::getId).containsExactly(
                "NEW", "PUBLISHED", "PUBLISHED_AND_UPDATED", "PUBLISHED_WITHOUT_LINKS", "UNPUBLISHED");
        assertThat(statusFilter.getValues()).extracting(FilterValue::getTitle).containsExactly(
                "Новая", "Опубликованная", "Обновленная", "Опубл. без связей", "Распубликованная");

        ResourceFilter typeFilter = iterator.next();
        assertThat(typeFilter.getName()).isEqualTo("type");
        assertThat(typeFilter.getTitle()).isEqualTo("Тип");
        assertThat(typeFilter.getValues()).extracting(FilterValue::getId).containsExactly(
                "article", "franchise", "hub", "blog", "landing");
        assertThat(typeFilter.getValues()).extracting(FilterValue::getTitle).containsExactly(
                "Статья",
                "Страница франшизы",
                "Журнал / Хаб",
                "Журнал / Блог",
                "landing"/*нет шаблона для landing - показываем в конце*/);
    }
}
