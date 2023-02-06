package ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.reference;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mdm.http.MdmBase;

public class ConstantBmdmExternalReferenceProviderTest {
    private static final MdmBase.MdmExternalReference REF_1 = MdmBase.MdmExternalReference.newBuilder()
        .setMdmId(100)
        .setPath(MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .setMdmId(1))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .setMdmId(2)))
        .setExternalSystem(MdmBase.MdmExternalSystem.OLD_MDM)
        .setExternalId(10001L)
        .build();
    private static final MdmBase.MdmExternalReference REF_2 = MdmBase.MdmExternalReference.newBuilder()
        .setMdmId(101)
        .setPath(MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .setMdmId(1))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .setMdmId(3))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .setMdmId(4))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .setMdmId(5))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_BOOL)
                .setMdmId(-2)))
        .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
        .setBool(MdmBase.MdmBooleanExtRefDetails.newBuilder().setValue(false))
        .setExternalId(10002L)
        .build();
    private static final MdmBase.MdmExternalReference REF_3 = MdmBase.MdmExternalReference.newBuilder()
        .setMdmId(102)
        .setPath(MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .setMdmId(1))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .setMdmId(3))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .setMdmId(4))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .setMdmId(5))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_BOOL)
                .setMdmId(-1)))
        .setExternalSystem(MdmBase.MdmExternalSystem.MBO)
        .setExternalId(10003L)
        .setBool(MdmBase.MdmBooleanExtRefDetails.newBuilder().setValue(true))
        .build();
    private static final MdmBase.MdmExternalReference REF_4 = MdmBase.MdmExternalReference.newBuilder()
        .setMdmId(103)
        .setPath(MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                .setMdmId(7))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setType(MdmBase.MdmMetaType.MDM_ATTR)
                .setMdmId(8)))
        .setExternalSystem(MdmBase.MdmExternalSystem.LMS)
        .setExternalId(10004L)
        .build();
    private static final ConstantBmdmExternalReferenceProvider PROVIDER =
        new ConstantBmdmExternalReferenceProvider(List.of(REF_1, REF_2, REF_3, REF_4));

    @Test
    public void testSearchAllRootEntityRefs() {
        BmdmExternalReferenceFilter allEntityType1 = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(1))
                .build(),
            Set.of(),
            Set.of()
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(allEntityType1))
            .containsExactlyInAnyOrder(REF_1, REF_2, REF_3);

        BmdmExternalReferenceFilter allEntityType7 = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(7))
                .build(),
            Set.of(),
            Set.of()
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(allEntityType7)).containsExactlyInAnyOrder(REF_4);
    }

    @Test
    public void testSearchAll() {
        BmdmExternalReferenceFilter all = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder().build(),
            Set.of(),
            Set.of()
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(all))
            .containsExactlyInAnyOrder(REF_1, REF_2, REF_3, REF_4);
    }

    @Test
    public void testSearchBySystem() {
        BmdmExternalReferenceFilter oldMdm = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder().build(),
            Set.of(MdmBase.MdmExternalSystem.OLD_MDM),
            Set.of()
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(oldMdm)).containsExactlyInAnyOrder(REF_1);


        BmdmExternalReferenceFilter mbo = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder().build(),
            Set.of(MdmBase.MdmExternalSystem.MBO),
            Set.of()
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(mbo)).containsExactlyInAnyOrder(REF_2, REF_3);

        BmdmExternalReferenceFilter lms = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder().build(),
            Set.of(MdmBase.MdmExternalSystem.LMS),
            Set.of()
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(lms)).containsExactlyInAnyOrder(REF_4);
    }

    @Test
    public void testSearchByMetaType() {
        BmdmExternalReferenceFilter attributes = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder().build(),
            Set.of(),
            Set.of(MdmBase.MdmMetaType.MDM_ATTR)
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(attributes)).containsExactlyInAnyOrder(REF_1, REF_4);


        BmdmExternalReferenceFilter bools = new BmdmExternalReferenceFilter(
            MdmBase.MdmPath.newBuilder().build(),
            Set.of(),
            Set.of(MdmBase.MdmMetaType.MDM_BOOL)
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(bools)).containsExactlyInAnyOrder(REF_2, REF_3);
    }

    @Test
    public void testSearchByFullPath() {
        BmdmExternalReferenceFilter ref2 = new BmdmExternalReferenceFilter(
            REF_2.getPath(),
            Set.of(),
            Set.of()
        );
        Assertions.assertThat(PROVIDER.findExternalReferences(ref2)).containsExactlyInAnyOrder(REF_2);
    }
}
