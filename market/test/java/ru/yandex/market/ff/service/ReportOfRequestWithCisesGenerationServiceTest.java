package ru.yandex.market.ff.service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.Identifier;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.enums.IdentifierType;
import ru.yandex.market.ff.service.implementation.ReportOfRequestWithCisesGenerationService;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportOfRequestWithCisesGenerationServiceTest extends ActGenerationServiceTest {

    private static final long SHOP_REQUEST_ID = 1;

    @Test
    @DatabaseSetup("classpath:service/xlsx-report/crossdock-requests.xml")
    @ExpectedDatabase(value = "classpath:service/xlsx-report/crossdock-requests.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void testReportGeneration() throws IOException, InvalidFormatException {
        RequestItemService itemService =
                mock(RequestItemService.class);
        ReportOfRequestWithCisesGenerationService reportOfRequestWithCisesGenerationService =
                new ReportOfRequestWithCisesGenerationService(itemService) {
                    @NotNull
                    @Override
                    public DocumentType getType() {
                        return DocumentType.REPORT_OF_WITHDRAW_WITH_CISES;
                    }
                };

        UnitPartialId unitPartialId21 = mock(UnitPartialId.class);
        when(unitPartialId21.getType()).thenReturn(RegistryUnitIdType.IMEI);
        when(unitPartialId21.getValue()).thenReturn("Imei");

        UnitPartialId unitPartialId22 = mock(UnitPartialId.class);
        when(unitPartialId22.getType()).thenReturn(RegistryUnitIdType.CIS);
        when(unitPartialId22.getValue()).thenReturn("cisssss1");

        UnitPartialId unitPartialId41 = mock(UnitPartialId.class);
        when(unitPartialId41.getType()).thenReturn(RegistryUnitIdType.CIS);
        when(unitPartialId41.getValue()).thenReturn("cisssss2");

        UnitPartialId unitPartialId42 = mock(UnitPartialId.class);
        when(unitPartialId42.getType()).thenReturn(RegistryUnitIdType.CIS);
        when(unitPartialId42.getValue()).thenReturn("cisssss3");

        UnitPartialId unitPartialId51 = mock(UnitPartialId.class);
        when(unitPartialId51.getType()).thenReturn(RegistryUnitIdType.CIS);
        when(unitPartialId51.getValue()).thenReturn("cisssss4");

        UnitPartialId unitPartialId52 = mock(UnitPartialId.class);
        when(unitPartialId52.getType()).thenReturn(RegistryUnitIdType.CIS);
        when(unitPartialId52.getValue()).thenReturn("cisssss5");

        UnitPartialId unitPartialId61 = mock(UnitPartialId.class);
        when(unitPartialId61.getType()).thenReturn(RegistryUnitIdType.CIS);
        when(unitPartialId61.getValue()).thenReturn("cisssss6");

        UnitPartialId unitPartialId62 = mock(UnitPartialId.class);
        when(unitPartialId62.getType()).thenReturn(RegistryUnitIdType.CIS);
        when(unitPartialId62.getValue()).thenReturn("cisssss7");

        //

        RegistryUnitId registryUnit2Id = mock(RegistryUnitId.class);
        when(registryUnit2Id.getParts()).thenReturn(null);

        RegistryUnitId registryUnit3Id = mock(RegistryUnitId.class);
        when(registryUnit3Id.getParts()).thenReturn(new LinkedHashSet<>(List.of(unitPartialId21, unitPartialId22)));

        RegistryUnitId registryUnit4Id = mock(RegistryUnitId.class);
        when(registryUnit4Id.getParts()).thenReturn(Set.of());

        RegistryUnitId registryUnit5Id = mock(RegistryUnitId.class);
        when(registryUnit5Id.getParts()).thenReturn(new LinkedHashSet<>(List.of(unitPartialId41, unitPartialId42)));

        RegistryUnitId registryUnit6Id = mock(RegistryUnitId.class);
        when(registryUnit6Id.getParts()).thenReturn(new LinkedHashSet<>(List.of(unitPartialId51, unitPartialId52)));

        RegistryUnitId registryUnit7Id = mock(RegistryUnitId.class);
        when(registryUnit7Id.getParts()).thenReturn(new LinkedHashSet<>(List.of(unitPartialId61, unitPartialId62)));

        //

        Identifier identifier1 = mock(Identifier.class);
        when(identifier1.getType()).thenReturn(IdentifierType.RECEIVED);
        when(identifier1.getIdentifiers()).thenReturn(null);

        Identifier identifier2 = mock(Identifier.class);
        when(identifier2.getType()).thenReturn(IdentifierType.RECEIVED_UNFIT);
        when(identifier2.getIdentifiers()).thenReturn(registryUnit2Id);

        Identifier identifier3 = mock(Identifier.class);
        when(identifier3.getType()).thenReturn(IdentifierType.RECEIVED_UNFIT);
        when(identifier3.getIdentifiers()).thenReturn(registryUnit3Id);

        Identifier identifier4 = mock(Identifier.class);
        when(identifier4.getType()).thenReturn(IdentifierType.DECLARED);
        when(identifier4.getIdentifiers()).thenReturn(registryUnit4Id);

        Identifier identifier5 = mock(Identifier.class);
        when(identifier5.getType()).thenReturn(IdentifierType.RECEIVED);
        when(identifier5.getIdentifiers()).thenReturn(registryUnit5Id);

        Identifier identifier6 = mock(Identifier.class);
        when(identifier6.getType()).thenReturn(IdentifierType.RECEIVED_UNFIT);
        when(identifier6.getIdentifiers()).thenReturn(registryUnit6Id);

        Identifier identifier7 = mock(Identifier.class);
        when(identifier7.getType()).thenReturn(IdentifierType.RECEIVED);
        when(identifier7.getIdentifiers()).thenReturn(registryUnit7Id);

        //

        RequestItem firstItem = mock(RequestItem.class);
        when(firstItem.getArticle()).thenReturn("SKU1");
        when(firstItem.getName()).thenReturn("Name 1");
        when(firstItem.getRequestItemIdentifiers())
                .thenReturn(new LinkedHashSet<>(List.of(identifier1, identifier2, identifier3, identifier4)));

        RequestItem secondItem = mock(RequestItem.class);
        when(secondItem.getArticle()).thenReturn("SKU2");
        when(secondItem.getName()).thenReturn("Name 2");
        when(secondItem.getRequestItemIdentifiers())
                .thenReturn(new LinkedHashSet<>(List.of(identifier5, identifier6)));

        RequestItem thirdItem = mock(RequestItem.class);
        when(thirdItem.getArticle()).thenReturn("SKU3");
        when(thirdItem.getName()).thenReturn("Name 3");
        when(thirdItem.getRequestItemIdentifiers())
                .thenReturn(Set.of(identifier7));

        RequestItem fourthItem = mock(RequestItem.class);
        when(fourthItem.getRequestItemIdentifiers())
                .thenReturn(null);

        //

        when(itemService.findAllByRequestIdWithIdentifiersFetched(anyCollection())).thenReturn(new LinkedHashSet<>(
                List.of(
                        firstItem,
                        secondItem,
                        thirdItem,
                        fourthItem
                )));

        ShopRequest shopRequest = mock(ShopRequest.class);
        when(shopRequest.getId()).thenReturn(SHOP_REQUEST_ID);

        assertXlsxActGeneration(SHOP_REQUEST_ID,
                "report_of_withdraw_with_cises.xlsx",
                id -> reportOfRequestWithCisesGenerationService.generateReport(shopRequest));
    }
}
