package ru.yandex.market.wms.servicebus.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.UnauthorizedException;
import com.yandex.disk.rest.json.ApiError;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.dto.startrek.AttachItem;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class YandexDiskClientImplTest {

    @Mock
    private RestClient diskRestClient;
    @InjectMocks
    private YandexDiskClientImpl yandexDiskClient;

    @Test
    void createDirectory() throws ServerIOException, IOException {
        //given
        when(diskRestClient.makeFolder(any())).thenReturn(new Link());

        //when
        Link directory = yandexDiskClient.createDirectory("path/path1/path2");

        //then
        assertThat("Directory created", directory, notNullValue());
        verify(diskRestClient, times(1)).makeFolder(eq("path"));
        verify(diskRestClient, times(1)).makeFolder(eq("path/path1"));
        verify(diskRestClient, times(1)).makeFolder(eq("path/path1/path2"));
    }

    @Test
    void createSubDirectory() throws ServerIOException, IOException {
        //given
        Resource mockResource = mock(Resource.class);
        when(mockResource.getType()).thenReturn("dir");
        when(diskRestClient.makeFolder(any())).thenReturn(new Link());
        when(diskRestClient.getResources(any())).thenReturn(mockResource).thenReturn(null).thenReturn(null);

        //when
        Link directory = yandexDiskClient.createDirectory("path/path1/path2");

        //then
        assertThat("Directory created", directory, notNullValue());
        verify(diskRestClient, times(0)).makeFolder(eq("path"));
        verify(diskRestClient, times(1)).makeFolder(eq("path/path1"));
        verify(diskRestClient, times(1)).makeFolder(eq("path/path1/path2"));
    }

    @Test
    void uploadFile() throws ServerException, IOException {
        //given
        AttachItem attachItem = new AttachItem();
        attachItem.setFilename("file1.txt");
        attachItem.setContent(new byte[] {0x01, 0x02, 0x03, 0x04});
        attachItem.setContentType("ContentType");
        when(diskRestClient.getUploadLink("path/file1.txt", true)).thenReturn(Link.DONE);

        //when
        yandexDiskClient.uploadFile(attachItem, "path");

        //then
        verify(diskRestClient, times(1)).uploadFile(eq(Link.DONE), eq(true), any(), any());
    }

    @Test
    void shareResourceByPath() throws ServerIOException, IOException {
        //when
        yandexDiskClient.shareResourceByPath("path");

        //then
        verify(diskRestClient, times(1)).publish("path");
    }

    @Test
    void getResource() throws ServerIOException, IOException {
        //when
        yandexDiskClient.getResource("path");

        //then
        verify(diskRestClient, times(1)).getResources(any());
    }

    @Test
    void getResource_throwsException() throws ServerIOException, IOException {
        //given
        when(diskRestClient.getResources(any())).thenThrow(new UnauthorizedException(401, new ApiError()));

        //when
        Resource resource = yandexDiskClient.getResource("path");

        //then
        assertThat("Returns null", resource, nullValue());
        verify(diskRestClient, times(1)).getResources(any());
    }

    @Test
    void delete() throws ServerIOException, IOException {
        //when
        yandexDiskClient.delete("path");

        //then
        verify(diskRestClient, times(1)).delete(eq("path"), eq(true));
    }

    @Test
    void getDirectories() {
        //given
        ResourceList resourceList = mock(ResourceList.class);
        Resource mockResource = mock(Resource.class);
        when(mockResource.getType()).thenReturn("dir");
        when(mockResource.getResourceList()).thenReturn(resourceList);
        when(yandexDiskClient.getResource(any())).thenReturn(mockResource);
        ArrayList<Resource> resources = newArrayList(mockResource, mockResource, mockResource);
        resourceList.getItems().add(mockResource);
        resourceList.getItems().add(mockResource);
        resourceList.getItems().add(mockResource);
        when(resourceList.getItems()).thenReturn(resources);

        //when
        List<Resource> resourceListResult = yandexDiskClient.getDirectories("path");

        //then
        assertThat("Returns list", resourceListResult.size(), equalTo(3));
    }
}
