package ru.yandex.market.mbo.common.mbi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author york
 * @since 25.07.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ShopsProviderTest {

    private MbiDataExtractorService service;

    @Before
    public void init() {
        service = Mockito.mock(MbiDataExtractorService.class);
        Mockito.when(service.downloadFile(anyString(), any())).thenAnswer(invocation -> {
            String filename = invocation.getArgument(0);
            MbiDataExtractorService.MbiDownloadCallback<?> callback = invocation.getArgument(1);
            InputStream is = ShopsProviderTest.class.getResourceAsStream("/" + filename);
            Object o = callback.download(is);
            return o;
        });
    }

    @Test
    public void testProvider() {
        ShopsProvider provider = new ShopsProvider(service);
        Assert.assertEquals(2, provider.getShopNames().size());
    }

}
