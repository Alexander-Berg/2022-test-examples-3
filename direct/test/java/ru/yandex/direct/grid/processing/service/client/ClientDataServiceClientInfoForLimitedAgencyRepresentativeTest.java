package ru.yandex.direct.grid.processing.service.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class ClientDataServiceClientInfoForLimitedAgencyRepresentativeTest {
    private static final Long OPERATOR_UID = RandomNumberUtils.nextPositiveLong();
    private static final Long AGENCY_CHIEF_UID = RandomNumberUtils.nextPositiveLong();
    private static final Long SOME_UID = RandomNumberUtils.nextPositiveLong();

    @Mock
    private RbacService rbacService;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @InjectMocks
    private ClientDataService clientDataService;

    @Test
    public void checkIsLimitedAgencyRepresentativeRequestsAgencyInfo_limitedRepForSelfAgency() {
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(eq(AGENCY_CHIEF_UID));
        doReturn(AGENCY_CHIEF_UID).when(rbacService).getChief(eq(OPERATOR_UID));
        assertThat(clientDataService.isLimitedAgencyRepresentativeRequestsAgencyInfo(OPERATOR_UID, AGENCY_CHIEF_UID))
                .isTrue();
    }

    @Test
    public void checkIsLimitedAgencyRepresentativeRequestsAgencyInfo_notForAgency() {
        SoftAssertions softAssertions = new SoftAssertions();
        for (RbacRole value : RbacRole.values()) {
            if (value != RbacRole.AGENCY) {
                doReturn(value).when(rbacService).getUidRole(eq(AGENCY_CHIEF_UID));
                softAssertions
                        .assertThat(clientDataService.isLimitedAgencyRepresentativeRequestsAgencyInfo(OPERATOR_UID,
                                AGENCY_CHIEF_UID))
                        .isFalse();
            }
        }
        softAssertions.assertAll();
    }

    @Test
    public void checkIsLimitedAgencyRepresentativeRequestsAgencyInfo_limitedRepNotForSelfAgency() {
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(eq(AGENCY_CHIEF_UID));
        doReturn(SOME_UID).when(rbacService).getChief(eq(OPERATOR_UID));
        assertThat(clientDataService.isLimitedAgencyRepresentativeRequestsAgencyInfo(OPERATOR_UID, AGENCY_CHIEF_UID))
                .isFalse();
    }

}
