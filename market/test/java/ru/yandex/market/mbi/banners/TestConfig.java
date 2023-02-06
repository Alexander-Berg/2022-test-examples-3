package ru.yandex.market.mbi.banners;

import java.util.List;

import javax.sql.DataSource;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.common.bunker.BunkerWritingApi;
import ru.yandex.market.common.bunker.loader.BunkerLoader;
import ru.yandex.market.mbi.banners.shop.BannerService;
import ru.yandex.market.mbi.banners.shop.db.BannerDao;
import ru.yandex.market.mbi.banners.shop.db.DbBannerService;
import ru.yandex.market.mbi.banners.supplier.SupplierBannerDao;
import ru.yandex.market.mbi.banners.yt.YtCluster;
import ru.yandex.market.mbi.banners.yt.YtSupplierIdsDao;
import ru.yandex.market.mbi.banners.yt.YtTemplate;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@Configuration
public class TestConfig {

    @Autowired
    public DataSource mbiDbDataSource;

    @Autowired
    private YtSupplierIdsDao ytSupplierIdsDao;

    @Bean
    public BannerService bannerService() {
        return new DbBannerService(bannerDao());
    }

    @Bean
    public BannerDao bannerDao() {
        var template = new NamedParameterJdbcTemplate(mbiDbDataSource);
        return new BannerDao(template);
    }

    @Bean
    public SupplierBannerDao supplierBannerDao() {
        var template = new NamedParameterJdbcTemplate(mbiDbDataSource);
        return new SupplierBannerDao(template, ytSupplierIdsDao);
    }

    @Bean
    public YtTemplate bannerYtTemplate(
            @Value("#{'${yt.mbi.bluebanners.hosts}'.split(',')}") List<String> bannerYtHosts
    ) {
        return new YtTemplate(new YtCluster[]{
                new YtCluster(bannerYtHosts.get(0), mock(Yt.class)),
                new YtCluster(bannerYtHosts.get(1), mock(Yt.class))
        });
    }

    @Bean
    public BunkerWritingApi bunkerWritingApi() {
        return mock(BunkerWritingApi.class);
    }

    @Bean
    public BunkerLoader bunkerLoader() {
        return mock(BunkerLoader.class);
    }
}
