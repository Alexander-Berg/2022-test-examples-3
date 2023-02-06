package ru.yandex.market.mbo.db.modelstorage;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.modelstorage.mbo_category.MboCategoryMappingsService;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingChangeType;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierType;
import ru.yandex.market.mbo.security.MboRoles;
import ru.yandex.market.mbo.user.UserManagerMock;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author moskovkin@yandex-team.ru
 * @since 13.03.19
 */
public class MboCategoryMappingServiceTest {
    private static final int USER_ID = 22;
    private static final int SUPPLIER_ID = 1;
    private static final String SUPPLIER_SKU_ID = "test ssku";
    private static final long OLD_SKU_ID = 1;
    private static final long NEW_SKU_ID = 2;

    private MboCategoryMappingsService mboCategoryMappingsService;
    private MboMappingsService mbocMappingsService;

    @Before
    public void before() {

        UserManagerMock userManagerMock = new UserManagerMock();
        userManagerMock.addRole(USER_ID, MboRoles.SKU_MAPPING_OPERATOR);

        mbocMappingsService = Mockito.mock(MboMappingsService.class);
        Mockito.doAnswer(invocation -> {
            MboMappings.UpdateMappingsRequest request = invocation.getArgument(0);
            MboMappings.ProviderProductInfoResponse.Builder result = MboMappings.ProviderProductInfoResponse
                .newBuilder();

            result.setStatus(MboMappings.ProviderProductInfoResponse.Status.OK);
            request.getUpdatesList().forEach(update -> result.addResults(
                MboMappings.ProviderProductInfoResponse.ProductResult.newBuilder()
                    .setStatus(MboMappings.ProviderProductInfoResponse.Status.OK)
                    .build()
                )
            );

            return result.build();
        }).when(mbocMappingsService).updateMappings(any(MboMappings.UpdateMappingsRequest.class));

        Mockito.doAnswer(invocation -> {
            MboMappings.SearchMappingsByBusinessKeysRequest request = invocation.getArgument(0);
            MboMappings.SearchMappingsResponse.Builder result = MboMappings.SearchMappingsResponse.newBuilder();

            result.setStatus(MboMappings.SearchMappingsResponse.Status.OK);
            request.getKeysList().forEach(key -> result.addOffers(
                ru.yandex.market.mboc.http.SupplierOffer.Offer.newBuilder()
                    .setShopSkuId(key.getOfferId())
                    .setSupplierId(key.getBusinessId())
                    .setApprovedMapping(
                        ru.yandex.market.mboc.http.SupplierOffer.Mapping.newBuilder()
                            .setSkuId(NEW_SKU_ID)
                            .build()
                    )
                    .build()
                )
            );

            return result.build();
        }).when(mbocMappingsService).searchMappingsByBusinessKeys(
            any(MboMappings.SearchMappingsByBusinessKeysRequest.class));

        mboCategoryMappingsService = new MboCategoryMappingsService();
        mboCategoryMappingsService.setMboMappingsService(mbocMappingsService);
        mboCategoryMappingsService.setUserManager(userManagerMock);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void serviceShouldPassChangeTypeCorrectly() {
        SupplierOffer offer = SkuMappingsTestUtils.createMapping(SUPPLIER_ID, SUPPLIER_SKU_ID,
            OLD_SKU_ID, SupplierType.TYPE_THIRD_PARTY);

        mboCategoryMappingsService.updateMappings(
            Collections.singletonList(offer),
            MappingChangeType.SKU_REMOVAL_DUPLICATE,
            Collections.emptyList(),
            USER_ID
        );
        ArgumentCaptor<MboMappings.UpdateMappingsRequest> captor =
            ArgumentCaptor.forClass(MboMappings.UpdateMappingsRequest.class);
        Mockito.verify(mbocMappingsService, Mockito.times(1))
            .updateMappings(captor.capture());
        Assertions.assertThat(captor.getValue().getRequestInfo().getChangeType())
            .isEqualTo(MboMappings.ProductUpdateRequestInfo.ChangeType.SKU_REMOVAL_DUPLICATE);

        mboCategoryMappingsService.updateMappings(
            Collections.singletonList(offer),
            MappingChangeType.MOVE_MAPPINGS,
            Collections.emptyList(),
            USER_ID
        );

        Mockito.verify(mbocMappingsService, Mockito.times(2))
            .updateMappings(captor.capture());
        Assertions.assertThat(captor.getValue().getRequestInfo().getChangeType())
            .isEqualTo(MboMappings.ProductUpdateRequestInfo.ChangeType.MOVE_MAPPINGS);

        mboCategoryMappingsService.updateMappings(
            Collections.singletonList(offer),
            MappingChangeType.SKU_REMOVAL_SPLIT,
            Collections.emptyList(),
            USER_ID
        );
        Mockito.verify(mbocMappingsService, Mockito.times(3))
            .updateMappings(captor.capture());
        Assertions.assertThat(captor.getValue().getRequestInfo().getChangeType())
            .isEqualTo(MboMappings.ProductUpdateRequestInfo.ChangeType.SKU_REMOVAL_SPLIT);
    }
}
