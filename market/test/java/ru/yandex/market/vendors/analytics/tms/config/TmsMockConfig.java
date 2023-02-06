package ru.yandex.market.vendors.analytics.tms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.common.mds.s3.spring.configuration.MdsS3LocationConfiguration;
import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.vendors.analytics.core.data.DataCheckInfo;
import ru.yandex.market.vendors.analytics.core.model.ga.importer.response.PurchaseInfo;
import ru.yandex.market.vendors.analytics.core.security.AnalyticsTvmClient;
import ru.yandex.market.vendors.analytics.tms.service.mail.MailService;
import ru.yandex.market.vendors.analytics.tms.service.yt.OfferExporter;
import ru.yandex.market.vendors.analytics.tms.service.yt.ShopApplicationsExporter;
import ru.yandex.market.vendors.analytics.tms.service.yt.ShopCategoryAccessLevelExporter;
import ru.yandex.market.vendors.analytics.tms.service.yt.ShopDomainsExporter;
import ru.yandex.market.vendors.analytics.tms.service.yt.ShopMailSalesExporter;
import ru.yandex.market.vendors.analytics.tms.service.yt.UserPartnerRoleExporter;
import ru.yandex.market.vendors.analytics.tms.service.yt.VendorCategoriesExporter;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableWriter;

import static org.mockito.Mockito.mock;

/**
 * @author antipov93.
 */
@Configuration
@Import(MdsS3LocationConfiguration.class)
public class TmsMockConfig {

    @Bean
    public JobService jobService() {
        return mock(JobService.class);
    }

    @Bean
    public MailService mailService() {
        return mock(MailService.class);
    }

    @Bean
    public ShopDomainsExporter shopDomainsExporter() {
        return mock(ShopDomainsExporter.class);
    }

    @Bean
    public ShopApplicationsExporter shopApplicationsExporter() {
        return mock(ShopApplicationsExporter.class);
    }

    @Bean
    public OfferExporter offerExporter() {
        return mock(OfferExporter.class);
    }

    @Bean
    public ShopCategoryAccessLevelExporter shopCategoryAccessLevelExporter() {
        return mock(ShopCategoryAccessLevelExporter.class);
    }

    @Bean
    public ShopMailSalesExporter shopMailSalesExporter() {
        return mock(ShopMailSalesExporter.class);
    }

    @Bean
    public AnalyticsTvmClient analyticsTvmClient() {
        return mock(AnalyticsTvmClient.class);
    }

    @Bean
    public YtTableWriter<PurchaseInfo> gaDataWriter() {
        return mock(YtTableWriter.class);
    }

    @Bean
    public YtTableWriter<DataCheckInfo> dataCheckInfoWriter() {
        return mock(YtTableWriter.class);
    }

    @Bean
    public VendorCategoriesExporter vendorCategoriesExporter() {
        return mock(VendorCategoriesExporter.class);
    }

    @Bean
    public UserPartnerRoleExporter UserPartnerRoleExporter() {
        return mock(UserPartnerRoleExporter.class);
    }
}
