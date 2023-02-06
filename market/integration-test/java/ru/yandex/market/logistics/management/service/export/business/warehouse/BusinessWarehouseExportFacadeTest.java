package ru.yandex.market.logistics.management.service.export.business.warehouse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;
import ru.yandex.market.logistics.management.util.TestUtil;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.eq;

class BusinessWarehouseExportFacadeTest extends AbstractContextualTest {
    @Autowired
    private BusinessWarehouseExportFacade businessWarehouseExportFacade;

    @Autowired
    private MdsS3BucketClient mdsClient;

    @Test
    @DisplayName("Успешный экспорт файла с информацией о бизнес-складах")
    @DatabaseSetup("/data/service/export/business/warehouse/prepare_data.xml")
    void export() throws Exception {
        businessWarehouseExportFacade.instantUpdate();

        ArgumentCaptor<ContentProvider> contentCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        Mockito.verify(mdsClient).upload(
            eq("business_warehouse_info/business_warehouse_info.json"),
            contentCaptor.capture()
        );

        InputStream jsonStream = contentCaptor.getValue().getInputStream();
        String actual = IOUtils.toString(jsonStream, StandardCharsets.UTF_8);
        assertThatJson(actual).isEqualTo(TestUtil.pathToJson("data/service/export/business/warehouse/export.json"));
    }
}
