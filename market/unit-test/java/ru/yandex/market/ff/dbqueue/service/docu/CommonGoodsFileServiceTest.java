package ru.yandex.market.ff.dbqueue.service.docu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.RequestDocumentService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.WorkbookProviderService;
import ru.yandex.market.ff.service.implementation.utils.RegistryUnitPredicates;
import ru.yandex.market.ff.service.registry.RegistryChainStateService;
import ru.yandex.market.ff.service.util.excel.export.NoncomplientItemsWorkbookBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class CommonGoodsFileServiceTest {

    @Test
    void doBuild() throws IOException {
        RegistryChainStateService r = mock(RegistryChainStateService.class);
        ShopRequestFetchingService f = mock(ShopRequestFetchingService.class);
        RequestDocumentService d = mock(RequestDocumentService.class);
        NoncomplientItemsWorkbookBuilder workbookBuilder = mock(NoncomplientItemsWorkbookBuilder.class);

        Mockito.doAnswer(invocation -> {
            OutputStream argument = invocation.getArgument(0);
            argument.write("myDocumentBytes".getBytes(StandardCharsets.UTF_8));
            return argument;
        }).when(workbookBuilder).build(any(OutputStream.class));

        WorkbookProviderService wbProvider = mock(WorkbookProviderService.class);
        when(wbProvider.getGoodsFileBuilder(any())).thenReturn(workbookBuilder);

        when(r.getFinalStateOfRegistryChain(anyLong(), any())).thenReturn(List.of(RegistryUnit.builder().build()));
        ShopRequest shopRequest = mock(ShopRequest.class);
        when(shopRequest.getId()).thenReturn(123L);
        when(shopRequest.getType()).thenReturn(RequestType.SUPPLY);

        when(f.getRequestOrThrow(anyLong())).thenReturn(shopRequest);

        CommonGoodsFileService service = new CommonGoodsFileService(r, f, d, wbProvider);

        service.doBuild(DocumentType.ADDITIONAL_SUPPLY_ACCEPTABLE_GOODS, RegistryUnitPredicates.ANOMALY_UNIT.and(
                RegistryUnitPredicates.allCountsMatching(RegistryUnitPredicates.ACCEPTABLE_UNIT_COUNT)), 132L);
        ArgumentCaptor<InputStream> myStream = ArgumentCaptor.forClass(InputStream.class);

        Mockito.verify(d, times(1))
                .create(eq(123L), myStream.capture(), eq(DocumentType.ADDITIONAL_SUPPLY_ACCEPTABLE_GOODS),
                        eq(FileExtension.XLSX));


        Assertions.assertEquals("myDocumentBytes",
                new String(myStream.getValue().readAllBytes(), StandardCharsets.UTF_8));
    }
}
