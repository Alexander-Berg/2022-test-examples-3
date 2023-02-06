package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.configuration.properties.MbiMdsS3Properties;
import ru.yandex.market.logistics.tarifficator.service.mds.MdsS3Service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public abstract class AbstractMbiMdsS3Test extends AbstractContextualTest {
    private ResourceLocation location;

    @Autowired
    protected MdsS3Service mdsS3Service;
    @Autowired
    protected MdsS3Client mbiMdsS3Client;
    @Autowired
    protected ResourceLocationFactory mbiResourceLocationFactory;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected MbiMdsS3Properties properties;

    protected String result;

    protected abstract String getMdsPath();

    @BeforeEach
    void onBefore() {
        result = null;
        location = ResourceLocation.create(properties.getBucket(), getMdsPath());
        when(mbiResourceLocationFactory.createLocation(eq(getMdsPath()))).thenReturn(location);
    }

    @AfterEach
    void afterEach() {
        verify(mbiMdsS3Client).upload(
            eq(location),
            any(ContentProvider.class)
        );
        verifyNoMoreInteractions(mbiMdsS3Client);
    }

    protected void mockMdsClientWithResult() {
        doAnswer(invocation -> {
            StreamContentProvider provider = invocation.getArgument(1);
            result = IOUtils.toString(provider.getInputStream(), StandardCharsets.UTF_8);
            return null;
        }).when(mbiMdsS3Client).upload(
            eq(location),
            any(ContentProvider.class)
        );
    }

    protected void mockMdsClientWithError(String message) {
        doThrow(new MdsS3Exception(message))
            .when(mbiMdsS3Client).upload(
                eq(location),
                any(ContentProvider.class)
            );
    }
}
