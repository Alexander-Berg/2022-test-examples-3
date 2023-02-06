package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ru.yandex.direct.core.entity.client.model.AgencyStatus;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus;
import ru.yandex.direct.core.entity.client.model.TinType;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.Region;

import static com.google.common.base.Preconditions.checkState;

public final class TestClients {

    public static final RbacRole DEFAULT_ROLE = RbacRole.CLIENT;

    public static final TinType TEST_TIN_TYPE = TinType.LEGAL;
    public static final String TEST_TIN = "0123456789";


    private TestClients() {
    }

    public static Client defaultClient(Long chiefUid) {
        return defaultClient(chiefUid, DEFAULT_ROLE);
    }

    public static Client defaultClient(Long chiefUid, RbacRole role) {
        return defaultClient(role)
                .withChiefUid(chiefUid);
    }

    public static Client defaultClient() {
        return defaultClient(RbacRole.CLIENT);
    }

    public static Client defaultInternalClient() {
        RbacRole internalRole = RbacRole.SUPER;
        checkState(internalRole.isInternal());
        return defaultClient(internalRole);
    }

    public static Client defaultClient(RbacRole role) {
        return new Client()
                .withId(null)
                .withRole(role)
                .withName("wow wow palehchi")
                .withWorkCurrency(CurrencyCode.RUB)
                .withCreateDate(LocalDateTime.now())
                .withAllowCreateScampBySubclient(true)
                .withDeletedReps(null)
                .withAgencyUrl(null)
                .withAgencyStatus(AgencyStatus.AA)
                .withPrimaryManagerUid(null)
                .withPrimaryBayanManagerUid(null)
                .withPrimaryGeoManagerUid(null)
                .withAllowCreateScampBySubclient(false)
                .withCountryRegionId(Region.RUSSIA_REGION_ID)
                .withFaviconBlocked(false)
                .withNonResident(false)
                .withAgencyClientId(null)
                .withIsConversionMultipliersPopupDisabled(true)
                .withSuspendVideo(false)
                .withFeatureAccessAutoVideo(false)
                .withFeatureContextRelevanceMatchAllowed(false)
                .withFeatureContextRelevanceMatchInterfaceOnly(false)
                .withCantUnblock(false)
                .withTinType(TEST_TIN_TYPE)
                .withTin(TEST_TIN)
                .withAutoOverdraftLimit(BigDecimal.valueOf(0))
                .withOverdraftLimit(BigDecimal.valueOf(0))
                .withDebt(BigDecimal.valueOf(0))
                .withPhoneVerificationStatus(PhoneVerificationStatus.VERIFIED);
    }
}
