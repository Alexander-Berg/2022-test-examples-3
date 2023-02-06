package ru.yandex.market.delivery.transport_manager.config;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.delivery.transport_manager.config.s3.MdsS3Properties;
import ru.yandex.market.delivery.transport_manager.service.s3.S3FileReader;
import ru.yandex.market.delivery.transport_manager.service.s3.S3FileWriter;

@ContextConfiguration
public class MdsS3Configuration {
    @Bean
    private MdsS3Properties mdsS3Properties() {
        return new MdsS3Properties("gruzin-storage-test");
    }

    @Bean
    @Primary
    public S3FileWriter s3FileWriter(
        MdsS3Client client,
        ResourceLocationFactory resourceLocationFactory
    ) {
        return new S3FileWriter(client, resourceLocationFactory);
    }

    @Bean("gruzinS3FileWriter")
    @Qualifier("gruzinS3FileWriter")
    public S3FileWriter gruzinS3FileWriter(
        MdsS3Client client,
        MdsS3Properties mdsS3Properties
    ) {
        return new S3FileWriter(client, gruzinResourceLocationFactory(mdsS3Properties));
    }

    @Bean("gruzinS3FileReader")
    @Qualifier("gruzinS3FileReader")
    public S3FileReader gruzinS3FileReader(
        MdsS3Client client,
        MdsS3Properties mdsS3Properties
    ) {
        return new S3FileReader(client, gruzinResourceLocationFactory(mdsS3Properties));
    }

    @Nonnull
    private ResourceLocationFactory gruzinResourceLocationFactory(MdsS3Properties mdsS3Properties) {
        return ResourceLocationFactory.create(mdsS3Properties.getGruzinBucketName(), null);
    }
}
