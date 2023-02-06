package ru.yandex.market.wms.servicebus.service.impl;

import java.util.List;

import com.yandex.disk.rest.json.DiskInfo;
import com.yandex.disk.rest.json.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.receiving.client.impl.ReceivingClientImpl;
import ru.yandex.market.wms.servicebus.api.external.startrek.dto.YandexDiskSpaceDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class YandexDiskServiceImplTest {

    @Mock
    private YandexDiskClientImpl yandexDiskClient;
    @Mock
    private ReceivingClientImpl receivingClient;
    @InjectMocks
    private YandexDiskServiceImpl yandexDiskService;

    @Test
    public void publicFolderCreate() {
        //given
        when(yandexDiskClient.getResource(any())).thenReturn(new Resource());

        //when
        Resource createdDirectory = yandexDiskService.createPublicDirectory("path");

        //then
        assertThat("Directory is not created", createdDirectory, notNullValue());
        verify((yandexDiskClient), times(2)).getResource(eq("path"));
        verify((yandexDiskClient), times(0)).createDirectory(eq("path"));
        verify((yandexDiskClient), times(1)).shareResourceByPath(eq("path"));
    }

    @Test
    public void publicFolderCreate_failed() {
        //given
        when(yandexDiskClient.getResource(any())).thenReturn(null);

        //when
        Resource createdDirectory = yandexDiskService.createPublicDirectory("path");

        //then
        assertThat("Directory is not created", createdDirectory, nullValue());
        verify((yandexDiskClient), times(1)).getResource(eq("path"));
    }

    @Test
    public void publicFolderCreate_new() {
        //given
        when(yandexDiskClient.getResource(any())).thenReturn(new Resource());

        //when
        Resource createdDirectory = yandexDiskService.createPublicDirectory("path");

        //then
        assertThat("Directory is not created", createdDirectory, notNullValue());
        verify((yandexDiskClient), times(2)).getResource(eq("path"));
        verify((yandexDiskClient), times(0)).createDirectory(eq("path"));
        verify((yandexDiskClient), times(1)).shareResourceByPath(eq("path"));
    }

    @Test
    public void publicFolderCreate_shared() {
        //given
        Resource resourceMock = mock(Resource.class);
        when(resourceMock.getPublicUrl()).thenReturn("http://yandex.ru/");
        when(yandexDiskClient.getResource(any())).thenReturn(resourceMock);

        //when
        Resource createdDirectory = yandexDiskService.createPublicDirectory("path");

        //then
        assertThat("Directory is not created", createdDirectory, notNullValue());
        verify((yandexDiskClient), times(2)).getResource(eq("path"));
        verify((yandexDiskClient), times(0)).createDirectory(eq("path"));
        verify((yandexDiskClient), times(0)).shareResourceByPath(eq("path"));
    }

    @Test
    void removeShippedAnomalyDirectories() {
        //given
        Resource resourceMock1 = mock(Resource.class);
        when(resourceMock1.getName()).thenReturn("dir1");
        Resource resourceMock2 = mock(Resource.class);
        when(resourceMock2.getName()).thenReturn("dir2");
        Resource resourceMock3 = mock(Resource.class);
        when(resourceMock3.getName()).thenReturn("dir3");
        when(yandexDiskClient.getDirectories(eq("/")))
                .thenReturn(List.of(resourceMock1, resourceMock2, resourceMock3));
        when(receivingClient.getReceiptsWithNotShippedAnomalies(any())).thenReturn(newArrayList("dir2"));

        //when
        List<String> deleted = yandexDiskService.removeShippedAnomalyDirectories();

        //then
        verify(yandexDiskClient, times(1)).delete("dir1");
        verify(yandexDiskClient, times(0)).delete("dir2");
        verify(yandexDiskClient, times(1)).delete("dir3");
        assertThat("2 dirs deleted", deleted.size(), equalTo(2));
        assertThat("2 dirs deleted", deleted, hasItem("dir1"));
        assertThat("2 dirs deleted", deleted, not(hasItem("dir2")));
        assertThat("2 dirs deleted", deleted, hasItem("dir3"));
    }

    @Test
    void getSpaceInfo() {
        //given
        DiskInfo diskInfo = mock(DiskInfo.class);
        when(diskInfo.getTotalSpace()).thenReturn(1000_000_000L);
        when(diskInfo.getUsedSpace()).thenReturn(500_000_000L);
        when(yandexDiskClient.getDiskInfo()).thenReturn(diskInfo);

        //when
        YandexDiskSpaceDto spaceInfo = yandexDiskService.getSpaceInfo();

        //then
        assertThat("Used space", spaceInfo.getUsedSpace(), equalTo(500_000_000L));
        assertThat("Used space", spaceInfo.getTotalSpace(), equalTo(1000_000_000L));
    }
}
