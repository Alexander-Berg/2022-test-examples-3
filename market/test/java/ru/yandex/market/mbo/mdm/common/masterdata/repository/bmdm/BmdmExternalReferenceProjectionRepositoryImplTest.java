package ru.yandex.market.mbo.mdm.common.masterdata.repository.bmdm;

import java.time.Instant;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mdm.http.MdmBase;

public class BmdmExternalReferenceProjectionRepositoryImplTest extends MdmBaseDbTestClass {
    @Autowired
    private BmdmExternalReferenceProjectionRepository bmdmExternalReferenceProjectionRepository;

    @Test
    public void testInsertAndGet() {
        MdmBase.MdmExternalReference mdmExternalReference = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(12345L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
                    .setMdmId(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_TYPE_ID))
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setType(MdmBase.MdmMetaType.MDM_ATTR)
                    .setMdmId(KnownBmdmIds.FLAT_GOLD_MSKU_ENTITY_MSKU_ID_ATTRIBUTE_ID)))
            .setExternalSystem(MdmBase.MdmExternalSystem.OLD_MDM)
            .setExternalId(KnownMdmParams.MSKU_ID_REFERENCE)
            .setAttribute(MdmBase.MdmAttributeExtRefDetails.newBuilder()
                .setExternalName("msku_id")
                .setExternalRuTitle(StringValue.newBuilder()
                    .setValue("ID MSKU"))
                .setExternalTypeHint(MdmBase.MdmAttributeExternalReferenceTypeHint.SAME)
                .setAuxExternalData(Any.newBuilder()
                    .setValue(ByteString.copyFromUtf8("Very interesting")))
                .build())
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder()
                .setFrom(Instant.now().toEpochMilli()))
            .build();
        bmdmExternalReferenceProjectionRepository.replaceAll(List.of(mdmExternalReference));
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll()).containsOnly(mdmExternalReference);
        bmdmExternalReferenceProjectionRepository.replaceAll(List.of());
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll()).isEmpty();
    }

    @Test
    public void testInsertReplaceAndGetGoldMskuReferences() {
        //Initially empty
        bmdmExternalReferenceProjectionRepository.replaceAll(List.of());
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll()).isEmpty();

        // Insert flat gold msku references
        bmdmExternalReferenceProjectionRepository.replaceAll(TestBmdmUtils.FLAT_GOLD_MSKU_EXTERNAL_REFERENCES);
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll())
            .containsExactlyInAnyOrderElementsOf(TestBmdmUtils.FLAT_GOLD_MSKU_EXTERNAL_REFERENCES);

        // Replace with deep gold msku references
        bmdmExternalReferenceProjectionRepository.replaceAll(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
        Assertions.assertThat(bmdmExternalReferenceProjectionRepository.findAll())
            .containsExactlyInAnyOrderElementsOf(TestBmdmUtils.GOLD_MSKU_EXTERNAL_REFERENCES);
    }
}
