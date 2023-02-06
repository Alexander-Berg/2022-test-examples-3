package ru.yandex.market.billing.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.util.MoreMbiMatchers.xmlEquals;

public class PhoneVisibilityScheduleExportExecutorTest extends FunctionalTest {

    @Autowired
    private PhoneVisibilityScheduleExportExecutor phoneVisibilityScheduleExportExecutor;

    @Autowired
    private NamedHistoryMdsS3Client namedHistoryMdsS3Client;

    private String result;

    @Test
    @DbUnitDataSet(before = "phoneVisibilityScheduleExportExecutorTest.before.csv")
    public void phoneVisibilityScheduleExportExecutorTest() {
        when(namedHistoryMdsS3Client.upload(any(), any(ContentProvider.class)))
                .then(invocation -> {
                    result = readFully(invocation.getArgument(1));
                    return null;
                });

        phoneVisibilityScheduleExportExecutor.doJob(null);

        String expectedXml =
                StringTestUtil.getString(getClass(), "PhoneVisibilityScheduleExportExecutorTest.result.xml");

        assertThat(result, xmlEquals(expectedXml));
    }

    private String readFully(ContentProvider provider) {
        try {
            return IOUtils.toString(provider.getInputStream(), Charsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
