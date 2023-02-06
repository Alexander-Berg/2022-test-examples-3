package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MetadataProviderMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmAttributeToMdmParamConverterImpl;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.BmdmPathKeeper;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmBase;

public class BmdmEnumAttributeValuesToParamValuesConverterTest {
    private static final long ATTRIBUTE_ID = 99;
    private static final long ENTITY_TYPE_ID = 100;
    private static final long PARAM_ID = 99;
    private static final List<MdmBase.MdmEnumOption> OPTIONS = options();
    private static final MdmBase.MdmAttribute ATTRIBUTE = attribute();
    private static final MdmBase.MdmEntityType ENTITY_TYPE = entityType();
    private static final List<MdmBase.MdmExternalReference> OPTION_REFERENCES = optionReferences();
    private static final MdmBase.MdmExternalReference ATTRIBUTE_REFERENCE = attributeReference();
    private BmdmTypedAttributeValuesToParamValuesConverter converter;

    private static List<MdmBase.MdmEnumOption> options() {
        return LongStream.range(1, 5)
            .mapToObj(i -> MdmBase.MdmEnumOption.newBuilder()
                .setMdmAttributeId(ATTRIBUTE_ID)
                .setMdmId(i)
                .setValue("Option" + i)
                .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1L << i))
                .build())
            .collect(Collectors.toList());
    }

    private static List<MdmBase.MdmExternalReference> optionReferences() {
        return LongStream.range(1, 5)
            .mapToObj(i -> MdmBase.MdmExternalReference.newBuilder()
                .setMdmId(Integer.MAX_VALUE + i)
                .setExternalId(10 + i)
                .setExternalSystem(MdmBase.MdmExternalSystem.OLD_MDM)
                .setPath(new BmdmPathKeeper()
                    .addBmdmEntity(ENTITY_TYPE_ID)
                    .addBmdmAttribute(ATTRIBUTE_ID)
                    .addPathSegment(i, MdmBase.MdmMetaType.MDM_ENUM_OPTION)
                    .getPath())
                .build())
            .collect(Collectors.toList());
    }

    private static MdmBase.MdmAttribute attribute() {
        return MdmBase.MdmAttribute.newBuilder()
            .setMdmId(ATTRIBUTE_ID)
            .setMdmEntityTypeId(ENTITY_TYPE_ID)
            .setInternalName("some_test_attribute")
            .setDataType(MdmBase.MdmAttribute.DataType.ENUM)
            .setIsMultivalue(true)
            .setRuTitle("Тестовый атрибут")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .addAllOptions(OPTIONS)
            .build();
    }

    private static MdmBase.MdmExternalReference attributeReference() {
        return MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(Integer.MAX_VALUE + ATTRIBUTE_ID)
            .setExternalId(PARAM_ID)
            .setExternalSystem(MdmBase.MdmExternalSystem.OLD_MDM)
            .setAttribute(MdmBase.MdmAttributeExtRefDetails.newBuilder()
                .setExternalName("test_xsl_name"))
            .setPath(new BmdmPathKeeper()
                .addBmdmEntity(ENTITY_TYPE_ID)
                .addBmdmAttribute(ATTRIBUTE_ID)
                .getPath())
            .build();
    }

    private static MdmBase.MdmEntityType entityType() {
        return MdmBase.MdmEntityType.newBuilder()
            .setMdmId(ENTITY_TYPE_ID)
            .setInternalName("some_test_entity_type")
            .setMdmEntityKind(MdmBase.MdmEntityType.EntityKind.STRUCT)
            .setRuTitle("Тестовый тип сущности")
            .addAttributes(ATTRIBUTE)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    @Before
    public void setUp() throws Exception {
        MetadataProviderMock metadataProvider = new MetadataProviderMock();
        metadataProvider.addEntityType(ENTITY_TYPE);
        metadataProvider.addExternalReferences(OPTION_REFERENCES);
        metadataProvider.addExternalReference(ATTRIBUTE_REFERENCE);
        converter = new BmdmEnumAttributeValuesToParamValuesConverter(
            new BmdmAttributeToMdmParamConverterImpl(metadataProvider)
        );
    }

    @Test
    public void testSaveOptionsOrder() {
        // given
        Random random = new Random("BMDM is in my heart.".hashCode());
        List<MdmAttributeValue> values = OPTIONS.stream()
            .map(MdmBase.MdmEnumOption::getMdmId)
            .map(id -> MdmAttributeValue.newBuilder().setOption(id).build())
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(values, random);
        MdmAttributeValues attributeValues = MdmAttributeValues.newBuilder()
            .setMdmAttributeId(ATTRIBUTE_ID)
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceType("SUPPLIER")
                .setSourceId("812"))
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(813L))
            .addAllValues(values)
            .build();

        // when
        Map<Long, MdmParamValue> converted = new LinkedHashMap<>();
        converter.parseParamValues(
            ATTRIBUTE,
            attributeValues,
            pv -> converted.put(pv.getMdmParamId(), pv),
            new BmdmPathKeeper().addBmdmEntity(ENTITY_TYPE_ID)
        );
        MdmAttributeValues convertedBack = converter.buildAttributeValuesFromParamValues(
            ATTRIBUTE,
            converted,
            new BmdmPathKeeper().addBmdmEntity(ENTITY_TYPE_ID)
        ).orElseThrow();

        // then
        Assertions.assertThat(converted).hasSize(1);
        Assertions.assertThat(converted.get(PARAM_ID).getOptions())
            .map(MdmParamOption::getId)
            .map(id -> id - 10)
            .map(id -> MdmAttributeValue.newBuilder().setOption(id).build())
            .containsExactlyElementsOf(values);
        Assertions.assertThat(convertedBack).isEqualTo(attributeValues);
    }

    @Test
    public void testOptionRenders() {
        // given
        List<MdmAttributeValue> values = OPTIONS.stream()
            .map(MdmBase.MdmEnumOption::getMdmId)
            .map(id -> MdmAttributeValue.newBuilder().setOption(id).build())
            .collect(Collectors.toCollection(ArrayList::new));

        MdmAttributeValues attributeValues = MdmAttributeValues.newBuilder()
            .setMdmAttributeId(ATTRIBUTE_ID)
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceType("SUPPLIER")
                .setSourceId("812"))
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(813L))
            .addAllValues(values)
            .build();

        List<MdmParamOption> expectedMdmParamOptions = OPTIONS.stream()
            .map(bmdmOption -> new MdmParamOption(10 + bmdmOption.getMdmId()).setRenderedValue(bmdmOption.getValue()))
            .collect(Collectors.toList());

        // when
        Map<Long, MdmParamValue> converted = new LinkedHashMap<>();
        converter.parseParamValues(
            ATTRIBUTE,
            attributeValues,
            pv -> converted.put(pv.getMdmParamId(), pv),
            new BmdmPathKeeper().addBmdmEntity(ENTITY_TYPE_ID)
        );

        // then
        Assertions.assertThat(converted).hasSize(1);
        Assertions.assertThat(converted.get(PARAM_ID).getOptions()).containsExactlyElementsOf(expectedMdmParamOptions);
    }
}
