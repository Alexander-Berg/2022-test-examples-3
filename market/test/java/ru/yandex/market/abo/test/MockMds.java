package ru.yandex.market.abo.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.impl.NamedHistoryMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.api.impl.PureHistoryMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;

/**
 * @author imelnikov
 */
@Configuration
public class MockMds {

    @Bean
    public MdsS3Client mdsS3Client() throws Exception {
        return MockFactory.getMdsS3Client();
    }

    @Bean
    public PureHistoryMdsS3Client pureHistoryMdsS3Client(MdsS3Client mdsS3Client, KeyGenerator keyGenerator) {
        return new PureHistoryMdsS3ClientImpl(mdsS3Client, keyGenerator);
    }

    @Bean
    public NamedHistoryMdsS3Client historyMdsS3Client(
            PureHistoryMdsS3Client pureHistoryMdsS3Client,
            ResourceConfigurationProvider resourceConfigurationProvider) {

        return new NamedHistoryMdsS3ClientImpl(pureHistoryMdsS3Client, resourceConfigurationProvider);
    }

}
