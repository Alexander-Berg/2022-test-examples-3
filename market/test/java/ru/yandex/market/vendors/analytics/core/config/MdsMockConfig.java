package ru.yandex.market.vendors.analytics.core.config;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.spring.configuration.MdsS3LocationConfiguration;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
@Configuration
@Import(MdsS3LocationConfiguration.class)
public class MdsMockConfig {

    @Bean
    public MdsS3Client mdsS3Client() {
        var mdsS3Client = mock(MdsS3Client.class);
        var storage = new HashMap<String, Map<String, byte[]>>();

        when(mdsS3Client.download(any(ResourceLocation.class), any(ContentConsumer.class))).thenAnswer(invocation -> {
            ResourceLocation resourceLocation = (ResourceLocation) invocation.getArguments()[0];
            Map<String, byte[]> bucket = storage.getOrDefault(resourceLocation.getBucketName(), emptyMap());
            byte[] bytes = bucket.getOrDefault(resourceLocation.getKey(), new byte[0]);
            ContentConsumer consumer = (ContentConsumer) invocation.getArguments()[1];
            return consumer.consume(new ByteArrayInputStream(bytes));
        });

        doAnswer(invocation -> {
            ContentProvider provider = (ContentProvider) invocation.getArguments()[1];
            byte[] bytes = IOUtils.toByteArray(provider.getInputStream());
            ResourceLocation location = (ResourceLocation) invocation.getArguments()[0];
            storage.computeIfAbsent(location.getBucketName(), s -> new HashMap<>()).put(location.getKey(), bytes);
            return null;
        }).when(mdsS3Client).upload(any(ResourceLocation.class), any(ContentProvider.class));

        doAnswer(invocation -> {
            ResourceLocation[] locations = (ResourceLocation[]) invocation.getArguments()[0];
            StreamEx.of(locations)
                    .forEach(location -> storage.get(location.getBucketName()).remove(location.getKey()));
            return null;
        }).when(mdsS3Client).delete(any(ResourceLocation[].class));

        return mdsS3Client;
    }
}
