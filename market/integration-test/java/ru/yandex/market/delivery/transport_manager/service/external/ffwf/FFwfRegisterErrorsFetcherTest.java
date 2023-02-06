package ru.yandex.market.delivery.transport_manager.service.external.ffwf;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.MboRequestItemErrorDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTOContainer;
import ru.yandex.market.ff.client.dto.RequestItemErrorDTO;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FFwfRegisterErrorsFetcherTest extends AbstractContextualTest {
    @Autowired
    private FFwfRegisterErrorsFetcher fetcher;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ffwfClient);
    }

    @DatabaseSetup({
        "/repository/facade/register_facade/fetch_registries.xml",
        "/repository/facade/register_facade/register_links.xml",
        "/repository/facade/register_facade/transportation_tasks_for_regiseter_errors_fetch.xml",
    })
    @ExpectedDatabase(
        value = "/repository/facade/register_facade/after/fetch_registry_unit_errors_with_denied_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void updateErrors() {
        RequestItemDTOContainer itemsContainer = new RequestItemDTOContainer(1, 0, 2);

        RequestItemDTO item1 = new RequestItemDTO();
        item1.setArticle("428278");
        item1.setSupplierId(965190L);
        RequestItemErrorDTO err1 = new RequestItemErrorDTO();
        err1.setType(RequestItemErrorType.ASSORTMENT_SKU_NOT_FOUND);
        item1.setValidationErrors(List.of(err1));
        MboRequestItemErrorDTO mboErr1 = new MboRequestItemErrorDTO();
        mboErr1.setType("mboc.msku.error.supply-forbidden.category.warehouse");
        item1.setMboErrors(List.of(mboErr1));
        itemsContainer.addItem(item1);

        RequestItemDTO item2 = new RequestItemDTO();
        item2.setArticle("428279");
        item2.setSupplierId(965190L);
        RequestItemErrorDTO err2 = new RequestItemErrorDTO();
        err2.setType(RequestItemErrorType.ASSORTMENT_SKU_NOT_FOUND);
        item2.setValidationErrors(List.of(err2));
        itemsContainer.addItem(item2);

        when(ffwfClient.getRequestItems(Mockito.any())).thenReturn(itemsContainer);
        fetcher.fetchAndProcessRegisterErrors(1L, true);

        verify(ffwfClient).getRequestItems(Mockito.argThat(argument ->
            argument.getRequestId() == 1L &&
                argument.getPage() == 0 &&
                argument.getSize() == Integer.MAX_VALUE)
        );
    }
}
