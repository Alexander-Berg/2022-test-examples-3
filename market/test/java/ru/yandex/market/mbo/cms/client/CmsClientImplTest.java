package ru.yandex.market.mbo.cms.client;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.mbo.cms.client.builders.DeletePageRequest;
import ru.yandex.market.mbo.cms.client.builders.PublishPageRequest;
import ru.yandex.market.mbo.cms.client.builders.ResourceListRequest;
import ru.yandex.market.mbo.cms.client.builders.UpdateResourceRequest;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author eremeevvo
 * @since 20.12.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CmsClientImplTest {

    private static final String HOST = "http://test.cms.ru";

    private static final ResponseEntity<String> OK =
            new ResponseEntity<>("{rows:[]}", HttpStatus.OK);

    private RestTemplate restTemplate;

    private CmsClient cmsClient;

    @Before
    public void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        cmsClient = new CmsClientImpl(HOST, restTemplate);
    }

    @Test
    public void getResourceList() {
    }

    @Test
    public void deletePageTestRequest() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.eq(HttpMethod.DELETE),
                Mockito.eq(null),
                Mockito.eq(String.class))).thenReturn(OK);

        cmsClient.deletePage(
                new DeletePageRequest()
                        .setId(1L)
                        .setUserId(2L));

        cmsClient.deletePage(
                new DeletePageRequest()
                        .setId(1L));

        cmsClient.deletePage(new DeletePageRequest());

        Mockito.verify(restTemplate, Mockito.times(3)).exchange(
                captor.capture(),
                Mockito.eq(HttpMethod.DELETE),
                Mockito.eq(null),
                Mockito.eq(String.class));

        assertThat(captor.getAllValues()).containsExactlyInAnyOrder(
                HOST + CmsClientImpl.DELETE_PAGE_HANDLE + "?id=1&userId=2",
                HOST + CmsClientImpl.DELETE_PAGE_HANDLE + "?id=1",
                HOST + CmsClientImpl.DELETE_PAGE_HANDLE
        );
    }

    @Test
    public void getResourceListTestRequest() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        Mockito.when(restTemplate.getForEntity(
                Mockito.anyString(),
                Mockito.eq(String.class))).thenReturn(OK);

        cmsClient.getResourceList(
                new ResourceListRequest()
                        .setType("type")
                        .setAuthor("author")
                        .setLimit(1)
                        .setOffset(2)
                        .setUserId(3L)
                        .setSort("sort")
                        .setStatus("status")
                        .setSearch("search")
        );

        cmsClient.getResourceList(
                new ResourceListRequest()
                        .setType("type")
                        .setAuthor("author")
        );

        cmsClient.getResourceList(
                new ResourceListRequest()
        );

        Mockito.verify(restTemplate, Mockito.times(3)).getForEntity(
                captor.capture(),
                Mockito.eq(String.class));

        assertThat(captor.getAllValues()).containsExactlyInAnyOrder(
                HOST + CmsClientImpl.GET_RESOURCE_LIST_HANDLE
                        + "?limit=1&offset=2&userId=3&type=type&author=author" +
                        "&search=search&sort=sort&status=status",
                HOST + CmsClientImpl.GET_RESOURCE_LIST_HANDLE
                        + "?type=type&author=author",
                HOST + CmsClientImpl.GET_RESOURCE_LIST_HANDLE
        );
    }

    @Test
    public void publishPageTestRequest() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        Mockito.when(restTemplate.getForEntity(
                Mockito.anyString(),
                Mockito.eq(String.class))).thenReturn(OK);

        cmsClient.publishPage(
                new PublishPageRequest()
                        .setUserId(1L)
                        .setRevisionId(2L)
                        .setPublish(true)
        );

        cmsClient.publishPage(
                new PublishPageRequest()
        );

        Mockito.verify(restTemplate, Mockito.times(2)).getForEntity(
                captor.capture(),
                Mockito.eq(String.class));

        assertThat(captor.getAllValues()).containsExactlyInAnyOrder(
                HOST + CmsClientImpl.PUBLISH_PAGE_HANDLE + "?publish=true&rev-id=2&user-id=1",
                HOST + CmsClientImpl.PUBLISH_PAGE_HANDLE
        );
    }

    @Test
    public void updateResourceTestRequest() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        Mockito.when(restTemplate.exchange(
                Mockito.anyString(),
                Mockito.eq(HttpMethod.PUT),
                Mockito.any(HttpEntity.class),
                Mockito.eq(String.class))).thenReturn(OK);

        cmsClient.updateResource(
                new UpdateResourceRequest()
                        .setType("type")
                        .setBody("body")
                        .setConsumer("consumer")
                        .setNamespace("namespace")
                        .setUserId(1L)
                        .setId(2L));

        cmsClient.updateResource(
                new UpdateResourceRequest()
                        .setId(1L)
                        .setBody("body"));

        cmsClient.updateResource(
                new UpdateResourceRequest()
                        .setBody("body"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        Mockito.verify(restTemplate, Mockito.times(3)).exchange(
                captor.capture(),
                Mockito.eq(HttpMethod.PUT),
                Mockito.eq(entity),
                Mockito.eq(String.class));

        assertThat(captor.getAllValues()).containsExactlyInAnyOrder(
                HOST + CmsClientImpl.UPDATE_RESOURCE_HANDLE
                        + "?id=2&userId=1&type=type&consumer=consumer&namespace=namespace",
                HOST + CmsClientImpl.UPDATE_RESOURCE_HANDLE + "?id=1",
                HOST + CmsClientImpl.UPDATE_RESOURCE_HANDLE
        );
    }
}
