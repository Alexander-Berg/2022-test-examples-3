package ru.yandex.market.mboc.common.services.cargotype;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeDto;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;

public class DownloadCargoTypeServiceImplTest {
    private LMSClient lmsClient = Mockito.mock(LMSClient.class);
    private DownloadCargoTypeServiceImpl downloadCargoTypeServiceImpl = new DownloadCargoTypeServiceImpl(lmsClient);

    @Test
    @SuppressWarnings({"checkstyle:magicNumber"})
    public void whenDownloadsCorrectJsonShouldParseCargoTypesCorrectly() {
        Mockito.doReturn(List.of(
            new CargoTypeDto(22L, 0, "не определен"),
            new CargoTypeDto(23L, 10, "документы и ценные бумаги")
        ))
            .when(lmsClient).getAllCargoTypes();

        Assertions.assertThat(downloadCargoTypeServiceImpl.downloadCargoTypes())
            .containsExactly(
                new CargoType(0, "не определен", null),
                new CargoType(10, "документы и ценные бумаги", null)
            );
    }

    @Test
    public void whenDownloadsCorrectJsonShouldParseCargoTypesCorrectlyEmptyTest() {
        Assertions.assertThat(downloadCargoTypeServiceImpl.downloadCargoTypes())
            .isEmpty();
    }
}
