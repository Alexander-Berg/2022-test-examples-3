package ru.yandex.direct.core.entity.campaign.service.accesschecker;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.api5.Api5CampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignAccessType;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CampaignSubObjectAccessConstraintTest {
    private static final CampaignAccessDefects ACCESS_DEFECTS = new CampaignAccessDefects.Builder()
            .withTypeNotAllowable(accessError(AccessTestDefectIds.TYPE_NOT_ALLOWABLE))
            .withNotVisible(accessError(AccessTestDefectIds.NOT_VISIBLE))
            .withTypeNotSupported(accessError(AccessTestDefectIds.NOT_SUPPORTED))
            .withNoRights(accessError(AccessTestDefectIds.NO_RIGHTS))
            .withArchivedModification(accessError(AccessTestDefectIds.ARCHIVED))
            .build();

    private static final Function<Long, Long> CHECKED_OBJECT_ID_PROVIDER = id -> 10 * id;

    private static final Long OBJECT_ID = 1L;
    private static final Long CHECKED_OBJECT_ID = CHECKED_OBJECT_ID_PROVIDER.apply(OBJECT_ID);
    private static final CampaignAccessibiltyChecker API_5_CAMPAIGN_ACCESSIBILITY_CHECKER =
            Api5CampaignAccessibilityChecker.getApi5AccessibilityChecker();

    @Mock
    private CampaignSubObjectAccessChecker checker;

    private CampaignSubObjectAccessConstraint readWriteConstraint;
    private CampaignSubObjectAccessConstraint readConstraint;

    private static Function<Long, Defect> accessError(AccessTestDefectIds defectId) {
        return objectId -> new Defect<>(defectId, objectId);
    }

    @Before
    public void setUp() throws Exception {
        when(checker.objectInAllowableCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(true);
        when(checker.objectInVisibleCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(true);
        when(checker.objectInWritableAndEditableCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(true);
        when(checker.objectInArchivedCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(false);

        readWriteConstraint = new CampaignSubObjectAccessConstraint(
                checker,
                CHECKED_OBJECT_ID_PROVIDER,
                ACCESS_DEFECTS,
                CampaignAccessType.READ_WRITE,
                new AffectedCampaignIdsContainer());
        readConstraint = new CampaignSubObjectAccessConstraint(
                checker,
                CHECKED_OBJECT_ID_PROVIDER,
                ACCESS_DEFECTS,
                CampaignAccessType.READ,
                new AffectedCampaignIdsContainer());
    }

    @Test
    public void readWriteSuccess() {
        Defect actualDefect = readWriteConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isNull();
    }

    @Test
    public void readSuccess() {
        Defect actualDefect = readConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isNull();
    }

    @Test
    public void readWriteTypeNotAllowable() {
        when(checker.objectInAllowableCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(false);

        Defect actualDefect = readWriteConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isEqualTo(ACCESS_DEFECTS.getTypeNotAllowable().apply(OBJECT_ID));
    }

    @Test
    public void readTypeNotAllowable() {
        when(checker.objectInAllowableCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(false);

        Defect actualDefect = readConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isEqualTo(ACCESS_DEFECTS.getTypeNotAllowable().apply(OBJECT_ID));
    }

    @Test
    public void readWriteNotVisible() {
        when(checker.objectInVisibleCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(false);

        Defect actualDefect = readWriteConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isEqualTo(ACCESS_DEFECTS.getNotVisible().apply(OBJECT_ID));
    }

    @Test
    public void readNotVisible() {
        when(checker.objectInVisibleCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(false);

        Defect actualDefect = readConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isEqualTo(ACCESS_DEFECTS.getNotVisible().apply(OBJECT_ID));
    }

    @Test
    public void readWriteNoRights() {
        when(checker.objectInWritableAndEditableCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(false);

        Defect actualDefect = readWriteConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isEqualTo(ACCESS_DEFECTS.getNoRights().apply(OBJECT_ID));
    }

    @Test
    public void readNoRights() {
        when(checker.objectInWritableAndEditableCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(false);

        Defect actualDefect = readConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isNull();
    }

    @Test
    public void readWriteArchivedCampaign() {
        when(checker.objectInArchivedCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(true);

        Defect actualDefect = readWriteConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isEqualTo(ACCESS_DEFECTS.getArchivedModification().apply(OBJECT_ID));
    }

    @Test
    public void readArchivedCampaign() {
        when(checker.objectInArchivedCampaign(eq(CHECKED_OBJECT_ID))).thenReturn(true);

        Defect actualDefect = readConstraint.apply(OBJECT_ID);

        assertThat(actualDefect).isNull();
    }

    private enum AccessTestDefectIds implements DefectId<Long> {
        TYPE_NOT_ALLOWABLE,
        NOT_VISIBLE,
        NOT_SUPPORTED,
        NO_RIGHTS,
        ARCHIVED
    }
}
