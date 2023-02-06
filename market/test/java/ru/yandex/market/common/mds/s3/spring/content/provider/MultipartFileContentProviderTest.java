package ru.yandex.market.common.mds.s3.spring.content.provider;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link MultipartFileContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class MultipartFileContentProviderTest {

    @Test
    public void testProviderPositive() throws Exception {
        final InputStream inputStream = mock(InputStream.class);
        final MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        final MultipartFileContentProvider contentProvider = new MultipartFileContentProvider(multipartFile);
        final InputStream actualStream = contentProvider.getInputStream();
        assertThat(inputStream, equalTo(actualStream));

        verify(multipartFile, times(1)).getInputStream();
        verifyNoMoreInteractions(multipartFile);
    }

    @Test(expected = MdsS3Exception.class)
    public void testProviderNegative() throws Exception {
        final MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getInputStream()).thenThrow(IOException.class);

        final MultipartFileContentProvider contentProvider = new MultipartFileContentProvider(multipartFile);
        fail(String.valueOf(contentProvider.getInputStream()));
    }

}
