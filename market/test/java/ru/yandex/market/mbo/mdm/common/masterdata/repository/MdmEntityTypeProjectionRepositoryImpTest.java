package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mdm.http.MdmBase;


public class MdmEntityTypeProjectionRepositoryImpTest extends MdmBaseDbTestClass {
    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    private MdmEntityTypeProjectionRepositoryImp mdmEntityTypeProjectionRepository;

    MdmBase.MdmEntityType.Builder baseMdmEntityTypeBuilder = MdmBase.MdmEntityType.newBuilder()
        .setMdmId(1)
        .setMdmEntityKind(MdmBase.MdmEntityType.EntityKind.SERVICE)
        .setInternalName("category")
        .setRuTitle("тест категория")
        .addAttributes(MdmBase.MdmAttribute.newBuilder()
            .setMdmId(3391)
            .setMdmEntityTypeId(1)
            .setDataType(MdmBase.MdmAttribute.DataType.ENUM)
            .addOptions(MdmBase.MdmEnumOption.newBuilder()
                .setMdmId(3392)
                .setMdmAttributeId(3391)
                .setValue("тест enum значение")
                .build())
            .build());

    @Before
    public void up() {
        mdmEntityTypeProjectionRepository =
            new MdmEntityTypeProjectionRepositoryImp(jdbcTemplate, transactionTemplate);
    }


    @Test
    public void testCorrectReadAndWrite() {
        List<MdmBase.MdmEntityType> mdmEntityTypeToWrite = List.of(baseMdmEntityTypeBuilder.build());
        mdmEntityTypeProjectionRepository.replaceAllByMdmEntityTypes(mdmEntityTypeToWrite);

        List<MdmBase.MdmEntityType> mdmEntityTypeFromRead = mdmEntityTypeProjectionRepository.findAllMdmEntityTypes();

        Assertions.assertThat(mdmEntityTypeFromRead).containsExactlyInAnyOrderElementsOf(mdmEntityTypeToWrite);
    }

    @Test
    public void testCorrectTruncateBetweenWrite() {
        List<MdmBase.MdmEntityType> mdmEntityTypeToWriteFirst = List.of(baseMdmEntityTypeBuilder.build());
        mdmEntityTypeProjectionRepository.replaceAllByMdmEntityTypes(mdmEntityTypeToWriteFirst);

        List<MdmBase.MdmEntityType> mdmEntityTypeToWriteSecond = List.of(baseMdmEntityTypeBuilder.setMdmId(2).build());
        mdmEntityTypeProjectionRepository.replaceAllByMdmEntityTypes(mdmEntityTypeToWriteSecond);

        List<MdmBase.MdmEntityType> mdmEntityTypeFromRead = mdmEntityTypeProjectionRepository.findAllMdmEntityTypes();

        Assertions.assertThat(mdmEntityTypeFromRead).containsExactlyInAnyOrderElementsOf(mdmEntityTypeToWriteSecond);
    }

}
