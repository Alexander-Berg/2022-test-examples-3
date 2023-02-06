package ru.yandex.market.billing.tasks.shopdata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class ShopDataExecutorTest extends FunctionalTest {

    @Autowired
    @Qualifier("shopDataExecutor")
    Executor shopDataExecutor;

    @Autowired
    NamedHistoryMdsS3Client namedHistoryMdsS3Client;

    @Test
    @DbUnitDataSet(before = "ShopDataExecutorTest.before.csv")
    void test() {
        String shopData = buildShopsDat().trim();
        String expectedShopData = StringTestUtil.getString(
                this.getClass().getResourceAsStream("expected_shop_data.txt"));
        assertEquals(expectedShopData, shopData);
    }

    private String buildShopsDat() {
        Mockito.reset(namedHistoryMdsS3Client);
        AtomicReference<String> shopdata = new AtomicReference<>();
        Mockito.when(namedHistoryMdsS3Client.upload(anyString(), any())).thenAnswer((inv) -> {
            ContentProvider provider = inv.getArgument(1);
            shopdata.set(IOUtils.readLines(provider.getInputStream(), StandardCharsets.UTF_8)
                    .stream()
                    .sorted()
                    .collect(Collectors.joining("\n")));
            return null;
        });
        shopDataExecutor.doJob(null);
        verify(namedHistoryMdsS3Client).upload(any(), any());
        return shopdata.get();
    }

    static class LocalFsS3Client implements NamedHistoryMdsS3Client {

        private final String path;

        LocalFsS3Client(String path) {
            this.path = path;
        }

        @Nonnull
        @Override
        public ResourceLocation upload(@Nonnull String configurationName, @Nonnull ContentProvider source) {
            Path location = Paths.get(path);
            location = location.resolve(configurationName);
            try {
                Files.copy(source.getInputStream(), location);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            System.out.println(location);
            return ResourceLocation.create(path, configurationName);
        }

        @Nonnull
        @Override
        public <D> D downloadLast(@Nonnull String configurationName, @Nonnull ContentConsumer<D> destination) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public ResourceListing deleteOld(@Nonnull String configurationName) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public URL getUrl(@Nonnull ResourceLocation location) {
            throw new UnsupportedOperationException();
        }

    }
}
