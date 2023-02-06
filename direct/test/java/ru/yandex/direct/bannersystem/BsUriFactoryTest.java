package ru.yandex.direct.bannersystem;

import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.bannersystem.exception.BsUriFactoryException;
import ru.yandex.direct.bannersystem.handle.BsHandleSpec;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BsUriFactoryTest {
    private static final String TEST_PATH = "/export/test.cgi";
    private static final String GOOD_HOST = "http://bs1.yandex.ru:81";
    private static final String GOOD_HOST_PRE = "http://bs1-pre.yandex.ru";
    private static final String BAD_HOST_NO_SCHEME = "bs2.yandex.ru";
    private static final String BAD_HOST_UNPARSABLE = "yandex.r*u";

    private BsHandleSpec bsHandle;

    private BsUriFactory getFactory(BsHostType type, String host, boolean preProd) {
        EnumMap<BsHostType, String> hostUrls = new EnumMap<>(BsHostType.class);
        hostUrls.put(type, host);
        if (!preProd) {
            return new BsUriFactory(hostUrls);
        }

        EnumMap<BsHostType, String> prodUrls = new EnumMap<>(BsHostType.class);
        prodUrls.put(BsHostType.EXPORT, GOOD_HOST);
        return new BsUriFactory(prodUrls, hostUrls);
    }

    @Before
    public void before() {
        bsHandle = mock(BsHandleSpec.class);
        when(bsHandle.getUrlPath()).thenReturn(TEST_PATH);
    }

    @Test
    public void getValidUrl() {
        when(bsHandle.getHostType()).thenReturn(BsHostType.EXPORT);

        URI uri = getFactory(BsHostType.EXPORT, GOOD_HOST, false).getProdUri(bsHandle);
        assertThat(uri, notNullValue());
        assertThat(uri.toASCIIString(), is("http://bs1.yandex.ru:81/export/test.cgi"));
    }

    @Test(expected = BsUriFactoryException.class)
    public void factoryExceptionWithInvalidHost() {
        BsUriFactory factory = new BsUriFactory(Collections.emptyMap());
        factory.convertStringToUri(BAD_HOST_UNPARSABLE);
    }

    @Test(expected = BsUriFactoryException.class)
    public void factoryExceptionWithNoScheme() {
        BsUriFactory factory = new BsUriFactory(Collections.emptyMap());
        factory.convertStringToUri(BAD_HOST_NO_SCHEME);
    }

    @Test
    public void getUrlWithPreProd() {
        when(bsHandle.getHostType()).thenReturn(BsHostType.FAST_EXPORT);
        when(bsHandle.hasPreProd()).thenReturn(true);

        URI uri = getFactory(BsHostType.FAST_EXPORT, GOOD_HOST_PRE, true).getPreProdUri(bsHandle);
        assertThat(uri, notNullValue());
        assertThat(uri.toASCIIString(), is("http://bs1-pre.yandex.ru/export/test.cgi"));
    }

    @Test(expected = BsUriFactoryException.class)
    public void getUrlWithPreProdForNoPreProdPath() {
        when(bsHandle.getHostType()).thenReturn(BsHostType.FAST_EXPORT);
        when(bsHandle.hasPreProd()).thenReturn(false);

        getFactory(BsHostType.FAST_EXPORT, GOOD_HOST, true).getPreProdUri(bsHandle);
    }

    @Test(expected = BsUriFactoryException.class)
    public void getUrlWithPreProdWhenNoPreProdSet() {
        when(bsHandle.getHostType()).thenReturn(BsHostType.EXPORT);
        when(bsHandle.hasPreProd()).thenReturn(true);

        getFactory(BsHostType.EXPORT, GOOD_HOST, false).getPreProdUri(bsHandle);
    }
}
