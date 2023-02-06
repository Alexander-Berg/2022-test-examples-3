package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsFullProductAccess;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsPartialProductAccess;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsReadAllProductAccess;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsOperatorProductAccessService;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class InternalCampaignAddValidationTypeSupportAccessTest {
    private static final int SHARD = 1;
    private static final Long FULL_ACCESS_OPERATOR_UID = 1L;
    private static final Long READONLY_ACCESS_OPERATOR_UID = 2L;
    private static final Long PARTIAL_ACCESS_OPERATOR_UID = 3L;
    private static final ClientId PRODUCT_CLIENT_ID = ClientId.fromLong(4L);

    private static final Set<Long> ALL_PLACES = Set.of(1L, 2L, 3L);
    private static final Set<Long> PARTIAL_ACCESS_PLACES = Set.of(1L);
    private static final Long VALID_PLACE_IN_PARTIAL_ACCESS_LIST = 1L;
    private static final Long VALID_PLACE_NOT_IN_PARTIAL_ACCESS_LIST = 2L;
    private static final Long INVALID_PLACE = 4L;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private InternalAdsOperatorProductAccessService internalAdsOperatorProductAccessService;

    @Mock
    private InternalAdsProductService internalAdsProductService;

    @InjectMocks
    private InternalCampaignAddValidationTypeSupport internalCampaignAddValidationTypeSupport;

    private final Long operatorUid;
    private final Long placeId;
    private final boolean expectErrors;

    public InternalCampaignAddValidationTypeSupportAccessTest(
            @SuppressWarnings("unused") String operatorDescription, Long operatorUid,
            @SuppressWarnings("unused") String placeDescription, Long placeId,
            boolean expectErrors) {
        this.operatorUid = operatorUid;
        this.placeId = placeId;
        this.expectErrors = expectErrors;
    }

    @Parameterized.Parameters(name = "{0}, place = {2} => {4}")
    public static Collection<Object[]> parameters() {
        return List.of(
                new Object[]{"FULL_ACCESS_OPERATOR_UID", FULL_ACCESS_OPERATOR_UID,
                        "null", null,
                        false},
                new Object[]{"FULL_ACCESS_OPERATOR_UID", FULL_ACCESS_OPERATOR_UID,
                        "0L", 0L,
                        false},
                new Object[]{"FULL_ACCESS_OPERATOR_UID", FULL_ACCESS_OPERATOR_UID,
                        "VALID_PLACE_IN_PARTIAL_ACCESS_LIST", VALID_PLACE_IN_PARTIAL_ACCESS_LIST,
                        true},
                new Object[]{"FULL_ACCESS_OPERATOR_UID", FULL_ACCESS_OPERATOR_UID,
                        "VALID_PLACE_NOT_IN_PARTIAL_ACCESS_LIST", VALID_PLACE_NOT_IN_PARTIAL_ACCESS_LIST,
                        true},
                new Object[]{"FULL_ACCESS_OPERATOR_UID", FULL_ACCESS_OPERATOR_UID,
                        "INVALID_PLACE", INVALID_PLACE,
                        true},
                new Object[]{"READONLY_ACCESS_OPERATOR_UID", READONLY_ACCESS_OPERATOR_UID,
                        "VALID_PLACE_IN_PARTIAL_ACCESS_LIST", VALID_PLACE_IN_PARTIAL_ACCESS_LIST,
                        false},
                new Object[]{"PARTIAL_ACCESS_OPERATOR_UID", PARTIAL_ACCESS_OPERATOR_UID,
                        "VALID_PLACE_IN_PARTIAL_ACCESS_LIST", VALID_PLACE_IN_PARTIAL_ACCESS_LIST,
                        true},
                new Object[]{"PARTIAL_ACCESS_OPERATOR_UID", PARTIAL_ACCESS_OPERATOR_UID,
                        "VALID_PLACE_NOT_IN_PARTIAL_ACCESS_LIST", VALID_PLACE_NOT_IN_PARTIAL_ACCESS_LIST,
                        false},
                new Object[]{"PARTIAL_ACCESS_OPERATOR_UID", PARTIAL_ACCESS_OPERATOR_UID,
                        "INVALID_PLACE", INVALID_PLACE,
                        false}
        );
    }

    @Before
    public void setUp() {
        when(internalAdsOperatorProductAccessService.getAccess(FULL_ACCESS_OPERATOR_UID, PRODUCT_CLIENT_ID))
                .thenReturn(new InternalAdsFullProductAccess(FULL_ACCESS_OPERATOR_UID, PRODUCT_CLIENT_ID,
                        () -> ALL_PLACES));

        when(internalAdsOperatorProductAccessService.getAccess(READONLY_ACCESS_OPERATOR_UID, PRODUCT_CLIENT_ID))
                .thenReturn(new InternalAdsReadAllProductAccess(READONLY_ACCESS_OPERATOR_UID, PRODUCT_CLIENT_ID));

        when(internalAdsOperatorProductAccessService.getAccess(PARTIAL_ACCESS_OPERATOR_UID, PRODUCT_CLIENT_ID))
                .thenReturn(new InternalAdsPartialProductAccess(PARTIAL_ACCESS_OPERATOR_UID, PRODUCT_CLIENT_ID,
                        PARTIAL_ACCESS_PLACES));

        when(internalAdsProductService.getProduct(PRODUCT_CLIENT_ID))
                .thenReturn(new InternalAdsProduct().withName("product"));
    }

    @Test
    public void testValidation() {
        InternalCampaign internalCampaign = new InternalAutobudgetCampaign()
                .withId(1L)
                .withPlaceId(placeId)
                .withPageId(Collections.emptyList())
                .withIsMobile(false);

        ValidationResult<List<InternalCampaign>, ?> vr = internalCampaignAddValidationTypeSupport.validate(
                CampaignValidationContainer.create(SHARD, operatorUid, PRODUCT_CLIENT_ID),
                new ValidationResult<>(List.of(internalCampaign)));

        assertThat(vr.hasAnyErrors()).isEqualTo(!expectErrors);
    }
}
