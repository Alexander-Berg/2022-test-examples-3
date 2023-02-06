package ru.yandex.market.logistics.management.service.export.partner.customerinfo;

import java.io.IOException;
import java.io.InputStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@CleanDatabase
class PartnerCustomerInfoExportTest extends AbstractContextualTest {

    private static final String EXPECTED_PATH = "partner_customer_info/partner_customer_info.json";

    @Autowired
    private PartnerCustomerInfoExportFacade partnerCustomerInfoExportFacade;

    @Autowired
    private MdsS3BucketClient mdsClient;

    @Test
    @Sql("/data/service/export/partner/customerinfo/prepare_data.sql")
    void testExport() throws IOException {
        partnerCustomerInfoExportFacade.instantUpdate();

        ArgumentCaptor<String> captorPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ContentProvider> captorContent = ArgumentCaptor.forClass(ContentProvider.class);
        Mockito.verify(mdsClient).upload(captorPath.capture(), captorContent.capture());

        String path = captorPath.getValue();
        softly.assertThat(path)
            .as("mds upload path of partner customer infos must be equal")
            .isEqualTo(EXPECTED_PATH);

        InputStream jsonStream = captorContent.getValue().getInputStream();
        String actual = toString(jsonStream);
        assertThatJson(actual)
            .as("mds file content of partner customer info must be equal")
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(TestUtil.pathToJson("data/service/export/partner/customerinfo/partner_customer_info.json"));
    }

    @Test
    @DatabaseSetup("/data/service/export/partner/customerinfo/prepare_dropoff_connected_shop_partners.xml")
    void testExportForDropoffConnectedShopPartners() throws IOException {
        partnerCustomerInfoExportFacade.instantUpdate();

        ArgumentCaptor<String> captorPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ContentProvider> captorContent = ArgumentCaptor.forClass(ContentProvider.class);
        Mockito.verify(mdsClient).upload(captorPath.capture(), captorContent.capture());

        String path = captorPath.getValue();
        softly.assertThat(path)
            .as("mds upload path of partner customer infos must be equal")
            .isEqualTo(EXPECTED_PATH);

        InputStream jsonStream = captorContent.getValue().getInputStream();
        String actual = toString(jsonStream);
        assertThatJson(actual)
            .as("mds file content of partner customer info must be equal")
            .isEqualTo(TestUtil.pathToJson(
                "data/service/export/partner/customerinfo/dropoff_connected_shop_partners.json"
            ));
    }

    private static String toString(InputStream inputStream) throws IOException {
        byte[] data = new byte[inputStream.available()];
        inputStream.read(data);
        return new String(data);
    }
}
