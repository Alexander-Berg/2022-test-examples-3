package ru.yandex.market.mbo.cms.api.servlets.api;

import org.eclipse.jetty.server.Server;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.cms.api.servlets.bean.RequestHelper;
import ru.yandex.market.mbo.cms.core.dto.Resource;
import ru.yandex.market.mbo.cms.core.utils.http.ErrorCode;
import ru.yandex.market.mbo.cms.core.utils.http.response.ApiResponse;
import ru.yandex.market.mbo.cms.core.utils.http.response.Error;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author gilmulla
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@SuppressWarnings("magicnumber")
public class ResourceServletTest {

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
    public void testMalformedSortMissingColon() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?sort=type", Resource.class);
        assertError(response);

        Error error = response.getError();

        assertThat(error.getCode()).isEqualTo(ErrorCode.UNEXPECTED_PARAMETER_VALUE.name());
        assertThat(error.getMessage())
                .isEqualTo("Failed to parse request parameter: sort. Missing colon after token: type");
    }

    //@Test
    public void testMalformedSortUnexpectedSortField() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?sort=typo:asc", Resource.class);
        assertError(response);

        Error error = response.getError();

        assertThat(error.getCode()).isEqualTo(ErrorCode.UNEXPECTED_PARAMETER_VALUE.name());
        assertThat(error.getMessage())
                .isEqualTo("Failed to parse request parameter: sort. Unexpected sort field: typo");
    }

    //@Test
    public void testMalformedSortUnexpectedSortOrder() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?sort=type:ass", Resource.class);
        assertError(response);

        Error error = response.getError();

        assertThat(error.getCode()).isEqualTo(ErrorCode.UNEXPECTED_PARAMETER_VALUE.name());
        assertThat(error.getMessage())
                .isEqualTo("Failed to parse request parameter: sort. Unexpected sort order: ass for field: type");
    }

    //@Test
    public void testMalformedOffset() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?offset=abc", Resource.class);
        assertError(response);

        Error error = response.getError();

        assertThat(error.getCode()).isEqualTo(ErrorCode.UNEXPECTED_PARAMETER_VALUE.name());
        assertThat(error.getMessage())
                .isEqualTo("Parameter offset: expected Integer, provided: \"abc\"");
    }

    //@Test
    public void testMalformedLimit() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?limit=1.2", Resource.class);
        assertError(response);

        Error error = response.getError();

        assertThat(error.getCode()).isEqualTo(ErrorCode.UNEXPECTED_PARAMETER_VALUE.name());
        assertThat(error.getMessage())
                .isEqualTo("Parameter limit: expected Integer, provided: \"1.2\"");
    }

    //@Test
    public void testNoArg() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows()).hasSize(10);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testResourceFields() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?offset=0&limit=1", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows()).hasSize(1);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
        assertThat(response.getPagination().getOffset()).isEqualTo(0);
        assertThat(response.getPagination().getLimit()).isEqualTo(1);

        Resource resource = response.getRows().iterator().next();
        assertThat(resource.getId()).isEqualTo(1L);
        assertThat(resource.getTitle()).isEqualTo("Title 1");
        assertThat(resource.getAuthor().getId()).isEqualTo(1);
        assertThat(resource.getAuthor().getName()).isEqualTo("user1");
        assertThat(resource.getType()).isEqualTo("article");
        assertThat(resource.getStatuses()).containsExactlyInAnyOrder("NEW");
        assertThat(resource.getUpdatedAt()).isEqualTo("2018-07-03T19:10:20");
    }

    //@Test
    public void testMissingAuthor() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?offset=6&limit=1", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows()).hasSize(1);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
        assertThat(response.getPagination().getOffset()).isEqualTo(6);
        assertThat(response.getPagination().getLimit()).isEqualTo(1);

        Resource resource = response.getRows().iterator().next();
        assertThat(resource.getId()).isEqualTo(7L);
        assertThat(resource.getTitle()).isEqualTo("Title 7");
        assertThat(resource.getAuthor().getId()).isEqualTo(4);
        assertThat(resource.getAuthor().getName()).isEqualTo("");
        assertThat(resource.getType()).isEqualTo("landing");
        assertThat(resource.getStatuses()).containsExactlyInAnyOrder("PUBLISHED_WITHOUT_LINKS");
        assertThat(resource.getUpdatedAt()).isEqualTo("2018-07-03T19:10:20");
    }

    //@Test
    public void testGetByNewStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?status=NEW", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(3)
                .extracting(Resource::getId).containsExactly(1L, 2L, 10L);
        assertThat(response.getPagination().getCount()).isEqualTo(3);
    }

    //@Test
    public void testGetByUnpublishedStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?status=UNPUBLISHED", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(3L, 4L);
        assertThat(response.getPagination().getCount()).isEqualTo(2);
    }

    //@Test
    public void testGetByPublishedStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?status=PUBLISHED", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(5)
                .extracting(Resource::getId).containsExactly(5L, 6L, 7L, 8L, 9L);
        assertThat(response.getPagination().getCount()).isEqualTo(5);
    }

    //@Test
    public void testGetByPublishedWithoutLinksStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?status=PUBLISHED_WITHOUT_LINKS", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(6L, 7L);
        assertThat(response.getPagination().getCount()).isEqualTo(2);
    }

    //@Test
    public void testGetByPublishedAndUpdatedStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?status=PUBLISHED_AND_UPDATED", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(8L, 9L);
        assertThat(response.getPagination().getCount()).isEqualTo(2);
    }

    //@Test
    public void testGetByNewAndUnpublishedStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?status=NEW&status=UNPUBLISHED", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(5)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L, 4L, 10L);
        assertThat(response.getPagination().getCount()).isEqualTo(5);
    }

    //@Test
    public void testGetByNewAndUnpublishedAndPublishedWithoutLinksStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?status=NEW&status=UNPUBLISHED&status=PUBLISHED_WITHOUT_LINKS", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(7)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L, 4L, 6L, 7L, 10L);
        assertThat(response.getPagination().getCount()).isEqualTo(7);
    }

    //@Test
    public void testGetByPublishedWithoutLinksAndPublishedAndUpdatedStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?status=PUBLISHED_AND_UPDATED&status=PUBLISHED_WITHOUT_LINKS", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(4)
                .extracting(Resource::getId).containsExactly(6L, 7L, 8L, 9L);
        assertThat(response.getPagination().getCount()).isEqualTo(4);
    }

    //@Test
    public void testGetArticleType() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?type=article", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(3)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L);
        assertThat(response.getPagination().getCount()).isEqualTo(3);
    }

    //@Test
    public void testGetArticleAndLandingType() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?type=article&type=landing", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(4)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L, 7L);
        assertThat(response.getPagination().getCount()).isEqualTo(4);
    }

    //@Test
    public void testGetByAuthor1() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?author=1", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(3)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L);
        assertThat(response.getPagination().getCount()).isEqualTo(3);
    }

    //@Test
    public void testGetByAuthor1AndAuthor3() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?author=1&author=3", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(4)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L, 6L);
        assertThat(response.getPagination().getCount()).isEqualTo(4);
    }

    /**
     * Этот тест важен - он тестирует случай неправильного приоритета (скобки вокруг условий статуса).
     */
    //@Test
    public void testGetByAuthorAndNewAndUnpublishedStatus() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?author=1&status=NEW&status=UNPUBLISHED", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(3)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L);
        assertThat(response.getPagination().getCount()).isEqualTo(3);
    }

    //@Test
    public void testSortByTitleAsc() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=title:asc", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(10)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByTitleDesc() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=title:desc", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(10)
                .extracting(Resource::getId).containsExactly(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByTitleDescWithOffsetAndLimit() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=title:desc&offset=3&limit=2", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(7L, 6L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByTypeAsc() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=type:asc", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(10)
                .extracting(Resource::getId).containsExactly(1L, 2L, 3L, 4L, 5L, 8L, 9L, 10L, 6L, 7L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByTypeDesc() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=type:desc", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(10)
                .extracting(Resource::getId).containsExactly(7L, 6L, 8L, 9L, 10L, 4L, 5L, 1L, 2L, 3L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByTypeDescWithOffsetAndLimit() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=type:desc&offset=3&limit=2", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(9L, 10L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByUpdatedAsc() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=updated:asc", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(10)
                .extracting(Resource::getId).containsExactly(2L, 1L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByUpdatedDesc() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=updated:desc", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(10)
                .extracting(Resource::getId).containsExactly(1L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 2L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testSortByUpdatedDescWithOffsetAndLimit() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?sort=updated:desc&offset=3&limit=2", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(5L, 6L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
    }

    //@Test
    public void testOffsetLimit() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList?offset=3&limit=2", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(4L, 5L);
        assertThat(response.getPagination().getCount()).isEqualTo(10);
        assertThat(response.getPagination().getOffset()).isEqualTo(3);
        assertThat(response.getPagination().getLimit()).isEqualTo(2);
    }

    //@Test
    public void testOffsetLimitCount() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?author=1&offset=1&limit=1", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(1)
                .extracting(Resource::getId).containsExactly(2L);
        assertThat(response.getPagination().getCount()).isEqualTo(3);
    }

    //@Test
    public void testOffsetLimitCount2() {
        ApiResponse<Resource> response = requestHelper.request(
                "/api/getResourceList" +
                        "?author=1&author=3&offset=1&limit=2", Resource.class);
        assertSuccess(response);

        assertThat(response.getRows())
                .hasSize(2)
                .extracting(Resource::getId).containsExactly(2L, 3L);
        assertThat(response.getPagination().getCount()).isEqualTo(4);
    }

    private void assertSuccess(ApiResponse response) {
        assertThat(response.getError()).isNull();
        assertThat(response.getRows()).isNotNull();
        assertThat(response.getPagination()).isNotNull();
    }

    private void assertError(ApiResponse response) {
        assertThat(response.getError()).isNotNull();
        assertThat(response.getRows()).isNull();
        assertThat(response.getPagination()).isNull();
    }
}
