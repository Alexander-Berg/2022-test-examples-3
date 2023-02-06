package ru.yandex.market.admin.mapping;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class WarehouseMappingControllerTest extends FunctionalTest {

    @Autowired
    WarehouseMappingController controller;

    @Autowired
    MdsS3Client mdsS3Client;

    @Autowired
    ResourceLocationFactory resourceLocationFactory;

    private static final long FAKE_ID = 1;

    @BeforeEach
    void setUp() throws MalformedURLException {
        Mockito.doReturn(ResourceLocation.create("bucket", "key")).when(resourceLocationFactory).createLocation(Mockito.anyString());
        Mockito.doReturn((new URL("https", "mds.net", ""))).when(mdsS3Client).getUrl(Mockito.any());
    }

    @Test
    @DisplayName("Успешное создание асинхронной задачи")
    @DbUnitDataSet(after = "WarehouseMappingControllerTest.after.csv")
    void testSuccess() {
        controller.createMappings(12321L, FAKE_ID, new MockMultipartFile("mappingfile.xlsx", new byte[]{}));
        Mockito.verify(mdsS3Client).upload(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("При ошибке сохранения в базу не сохранится файл в MDS S3")
    void testError() {
        Mockito.doReturn(null).when(mdsS3Client).getUrl(Mockito.any());
        Assertions.assertThrows(NullPointerException.class, () -> controller.createMappings(12321L, FAKE_ID, new MockMultipartFile("mappingfile.xlsx", new byte[]{})));
        Mockito.verify(mdsS3Client, Mockito.never()).upload(Mockito.any(), Mockito.any());
    }
}
