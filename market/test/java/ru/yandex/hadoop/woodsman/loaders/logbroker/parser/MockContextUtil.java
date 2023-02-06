package ru.yandex.hadoop.woodsman.loaders.logbroker.parser;

import org.mockito.Mockito;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.IpRegionLookupService;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Created by oroboros on 21.05.15.
 */
public class MockContextUtil {

    public static IpRegionLookupService getIpRegionLookupService() throws UnknownHostException {
        IpRegionLookupService ipRegionLookupService = mock(IpRegionLookupService.class);

        doAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0].equals("msk_ip")) {
                return 213L;
            } else {
                return 1L;
            }
        }).when(ipRegionLookupService).getRegionId(Mockito.anyString());

        doAnswer(invocationOnMock -> invocationOnMock.getArguments()[0].equals("ya_ip"))
                .when(ipRegionLookupService)
                .isYandex(any(InetAddress.class));

        return ipRegionLookupService;
    }
}
