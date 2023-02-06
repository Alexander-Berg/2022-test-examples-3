package ru.yandex.direct.core.entity.internalads.service;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.internalads.model.InternalAdsFullProductAccess;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsManagerProductAccess;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsManagerProductAccessType;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsNoProductAccess;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsOperatorProductAccess;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsPartialProductAccess;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsReadAllProductAccess;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
@CoreTest
public class InternalAdsOperatorProductAccessServiceTest {
    @Autowired
    private InternalAdsOperatorProductAccessService service;

    @Autowired
    private InternalAdsManagerProductAccessService productAccessService;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private Steps steps;

    private ClientInfo product;

    @Before
    public void setUp() {
        product = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        steps.internalAdPlaceSteps().ensurePlacesArePresent();
    }

    @Test
    public void test_super() {
        ClientInfo operator = createOperator(RbacRole.SUPER);
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertThat(access).isInstanceOf(InternalAdsFullProductAccess.class);
    }

    @Test
    public void test_superReader() {
        ClientInfo operator = createOperator(RbacRole.SUPERREADER);
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertThat(access).isInstanceOf(InternalAdsReadAllProductAccess.class);
    }

    @Test
    public void test_internalAdAdmin() {
        ClientInfo operator = createOperator(RbacRole.INTERNAL_AD_ADMIN);
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertThat(access).isInstanceOf(InternalAdsFullProductAccess.class);
    }

    @Test
    public void test_internalAdManager_noAccess() {
        ClientInfo operator = createOperator(RbacRole.INTERNAL_AD_MANAGER);
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertThat(access).isInstanceOf(InternalAdsNoProductAccess.class);
    }

    @Test
    public void test_internalAdManager_fullAccess() {
        ClientInfo operator = createOperator(RbacRole.INTERNAL_AD_MANAGER);
        productAccessService.addProductAccess(new InternalAdsManagerProductAccess(
                operator.getClientId(), product.getClientId(), InternalAdsManagerProductAccessType.FULL, null));
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertThat(access).isInstanceOf(InternalAdsFullProductAccess.class);
    }

    @Test
    public void test_internalAdManager_readAllAccess() {
        ClientInfo operator = createOperator(RbacRole.INTERNAL_AD_MANAGER);
        productAccessService.addProductAccess(new InternalAdsManagerProductAccess(
                operator.getClientId(), product.getClientId(), InternalAdsManagerProductAccessType.READONLY, null));
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertThat(access).isInstanceOf(InternalAdsReadAllProductAccess.class);
    }

    @Test
    public void test_internalAdManager_partialAccess() {
        ClientInfo operator = createOperator(RbacRole.INTERNAL_AD_MANAGER);
        List<Long> validPlaceIds = placeService.getValidPlaceIds();
        assertThat(validPlaceIds.size()).isGreaterThan(1);

        Set<Long> placeIds = Set.of(validPlaceIds.get(0));
        productAccessService.addProductAccess(new InternalAdsManagerProductAccess(
                operator.getClientId(), product.getClientId(), InternalAdsManagerProductAccessType.PARTIAL, placeIds));
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertSoftly(softly -> {
            softly.assertThat(access).isInstanceOf(InternalAdsPartialProductAccess.class);
            softly.assertThat(access.placesWithWriteAccess()).isEqualTo(placeIds);
        });
    }

    @Test
    public void test_internalAdSuperreader() {
        ClientInfo operator = createOperator(RbacRole.INTERNAL_AD_SUPERREADER);
        InternalAdsOperatorProductAccess access = service.getAccess(operator.getUid(), product.getClientId());
        assertThat(access).isInstanceOf(InternalAdsReadAllProductAccess.class);
    }

    private ClientInfo createOperator(RbacRole role) {
        return steps.clientSteps().createDefaultClientWithRole(role);
    }
}
