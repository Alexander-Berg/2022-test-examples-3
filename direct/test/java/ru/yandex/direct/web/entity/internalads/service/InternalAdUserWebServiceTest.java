package ru.yandex.direct.web.entity.internalads.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.internalads.model.AddProductAccessResponse;
import ru.yandex.direct.web.entity.internalads.model.GetUserResponse;
import ru.yandex.direct.web.entity.internalads.model.ModifyProductAccess;
import ru.yandex.direct.web.entity.internalads.model.RemoveProductAccess;
import ru.yandex.direct.web.entity.internalads.model.UpdateProductAccessResponse;
import ru.yandex.direct.web.entity.internalads.model.WebInternalAdProductAccess;
import ru.yandex.direct.web.entity.internalads.model.WebInternalAdUser;
import ru.yandex.direct.web.entity.internalads.model.WebProductAccessType;

import static org.assertj.core.api.Assertions.assertThat;

@DirectWebTest
@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class InternalAdUserWebServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private InternalAdUserWebService service;

    @Autowired
    private Steps steps;

    @Autowired
    private UserService userService;

    @Autowired
    InternalAdsProductService internalAdsProductService;

    private ClientId managerClientId;
    private String managerLogin;
    private String managerFio;
    private ClientId productClientId;
    private String productLogin;
    private String productName;
    private final WebProductAccessType accessType1;
    private final Set<Long> placeIds1;
    private final WebProductAccessType accessType2;
    private final Set<Long> placeIds2;

    public InternalAdUserWebServiceTest(WebProductAccessType accessType1, Set<Long> placeIds1,
                                        WebProductAccessType accessType2, Set<Long> placeIds2) {
        this.accessType1 = accessType1;
        this.placeIds1 = placeIds1;
        this.accessType2 = accessType2;
        this.placeIds2 = placeIds2;
    }

    private enum Accesses {
        FULL(WebProductAccessType.FULL, Set.of()),
        FULL_NULL(WebProductAccessType.FULL, null),
        READONLY(WebProductAccessType.READONLY, Set.of()),
        READONLY_NULL(WebProductAccessType.READONLY, null),
        PARTIAL(WebProductAccessType.FULL, new LinkedHashSet<>(List.of(1L, 2L, 3L)));

        private final WebProductAccessType accessType;

        @Nullable
        private final Set<Long> placeIds;

        Accesses(WebProductAccessType accessType, @Nullable Set<Long> placeIds) {
            this.accessType = accessType;
            this.placeIds = placeIds;
        }
    }

    @Parameterized.Parameters(name = "{0} ({1}) -> {2} ({3})")
    public static Collection<Object[]> parameters() {
        return Stream.of(Accesses.values())
                .flatMap(access1 -> Stream.of(Accesses.values())
                        .map(access2 -> new Object[]{
                                access1.accessType, access1.placeIds,
                                access2.accessType, access2.placeIds }))
                .collect(Collectors.toList());
    }

    @Before
    public void init() {
        ClientInfo managerClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.INTERNAL_AD_MANAGER);
        managerClientId = managerClientInfo.getClientId();
        managerLogin = managerClientInfo.getLogin();
        User managerUser = Objects.requireNonNull(userService.getUser(managerClientInfo.getUid()));
        managerFio = managerUser.getFio();

        ClientInfo productClientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        productClientId = productClientInfo.getClientId();
        productLogin = productClientInfo.getLogin();
        InternalAdsProduct product = internalAdsProductService.getProduct(productClientId);
        productName = product.getName();
    }

    @Test
    public void testInternalAdUserWebService() {
        WebInternalAdUser emptyAccess = new WebInternalAdUser()
                .withClientId(managerClientId.asLong())
                .withLogin(managerLogin)
                .withUsername(managerFio)
                .withAccessibleProducts(List.of());

        // в самом начале у менеджера нет доступа ни к чему
        checkGetManager(managerClientId, emptyAccess);

        // добавим доступ, убедимся, что он появился
        WebInternalAdProductAccess expectedAccess1 = new WebInternalAdProductAccess()
                .withProductClientId(productClientId.asLong())
                .withProductLogin(productLogin)
                .withProductName(productName)
                .withAccessType(accessType1)
                .withPlaceIds(expectedPlaceIds(accessType1, placeIds1));
        checkAddAccess(managerClientId, productAccessRequest(productClientId, accessType1, placeIds1),
                expectedAccess1);

        checkGetManager(managerClientId, new WebInternalAdUser()
                .withClientId(managerClientId.asLong())
                .withLogin(managerLogin)
                .withUsername(managerFio)
                .withAccessibleProducts(List.of(expectedAccess1)));

        // удалим доступ, убедимся, что он удалился
        checkDeleteAccess(managerClientId, productClientId);
        checkGetManager(managerClientId, emptyAccess);

        // добавим доступ обратно и опять убедимся, что он появился
        checkAddAccess(managerClientId, productAccessRequest(productClientId, accessType1, placeIds1),
                expectedAccess1);

        checkGetManager(managerClientId, new WebInternalAdUser()
                .withClientId(managerClientId.asLong())
                .withLogin(managerLogin)
                .withUsername(managerFio)
                .withAccessibleProducts(List.of(expectedAccess1)));

        // поменяем доступ на другой доступ и убедимся, что он поменялся
        WebInternalAdProductAccess expectedAccess2 = new WebInternalAdProductAccess()
                .withProductClientId(productClientId.asLong())
                .withProductLogin(productLogin)
                .withProductName(productName)
                .withAccessType(accessType2)
                .withPlaceIds(expectedPlaceIds(accessType2, placeIds2));

        checkUpdateAccess(managerClientId, productAccessRequest(productClientId, accessType2, placeIds2),
                expectedAccess2);

        checkGetManager(managerClientId, new WebInternalAdUser()
                .withClientId(managerClientId.asLong())
                .withLogin(managerLogin)
                .withUsername(managerFio)
                .withAccessibleProducts(List.of(expectedAccess2)));

    }

    private static ModifyProductAccess productAccessRequest(ClientId productClientId,
                                                            WebProductAccessType accessType,
                                                            Set<Long> placeIds) {
        return new ModifyProductAccess()
                .withAccessType(accessType)
                .withProductClientId(productClientId.asLong())
                .withPlaceIds(placeIds);
    }

    private void checkGetManager(ClientId managerClientId, WebInternalAdUser expectedWebInternalAdUser) {
        GetUserResponse manager = service.getManager(managerClientId);
        assertThat(manager.isSuccessful()).isTrue();
        assertThat(manager.getResult()).isEqualTo(expectedWebInternalAdUser);
    }

    private void checkAddAccess(ClientId managerClientId, ModifyProductAccess accessRequest,
                                WebInternalAdProductAccess expectedProductAccess) {
        WebResponse result = service.addUserProductAccess(managerClientId, accessRequest);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result).isInstanceOf(AddProductAccessResponse.class);
        assertThat(((AddProductAccessResponse) result).getResult()).isEqualTo(expectedProductAccess);
    }

    private void checkUpdateAccess(ClientId managerClientId, ModifyProductAccess accessRequest,
                                   WebInternalAdProductAccess expectedProductAccess) {
        WebResponse result = service.updateManagerProductAccess(managerClientId, accessRequest);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result).isInstanceOf(UpdateProductAccessResponse.class);
        assertThat(((UpdateProductAccessResponse) result).getResult()).isEqualTo(expectedProductAccess);
    }

    private void checkDeleteAccess(ClientId managerClientId, ClientId productClientId) {
        WebResponse result = service.removeUserProductAccess(managerClientId, List.of(new RemoveProductAccess()
                .withProductClientId(productClientId.asLong())));
        assertThat(result.isSuccessful()).isTrue();
    }

    @Nullable
    private static Set<Long> expectedPlaceIds(WebProductAccessType accessType, @Nullable Set<Long> savedPlaceIds) {
        switch (accessType) {
            case READONLY:
                return null;
            case FULL:
                if (savedPlaceIds == null || savedPlaceIds.isEmpty()) {
                    return null;
                }
                return savedPlaceIds;
            default:
                throw new IllegalStateException("Unexpected value: " + accessType);
        }
    }
}
