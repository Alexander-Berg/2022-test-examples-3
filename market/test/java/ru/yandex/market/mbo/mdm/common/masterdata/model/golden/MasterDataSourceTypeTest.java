package ru.yandex.market.mbo.mdm.common.masterdata.model.golden;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.MSKU_SOURCE_PREFIX;

/**
 * @author dmserebr
 * @date 15/03/2021
 */
public class MasterDataSourceTypeTest {

    @Test
    public void testExtractSpecificTypeFromSubtype() {
        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_ADMIN"))
        ).isEqualTo(pojo(MasterDataSourceType.MDM_ADMIN, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, MSKU_SOURCE_PREFIX, "MDM_ADMIN"))
        ).isEqualTo(pojo(MasterDataSourceType.MDM_ADMIN, MSKU_SOURCE_PREFIX));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MSKU_INHERIT"))
        ).isEqualTo(pojo(MasterDataSourceType.MSKU_INHERIT, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_OPERATOR"))
        ).isEqualTo(pojo(MasterDataSourceType.MDM_OPERATOR, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "TOOL"))
        ).isEqualTo(pojo(MasterDataSourceType.TOOL, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_DEFAULT"))
        ).isEqualTo(pojo(MasterDataSourceType.MDM_DEFAULT, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_UNKNOWN"))
        ).isEqualTo(pojo(MasterDataSourceType.MDM_UNKNOWN, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "IRIS_MDM"))
        ).isEqualTo(pojo(MasterDataSourceType.IRIS_MDM, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MBO_OPERATOR"))
        ).isEqualTo(pojo(MasterDataSourceType.MBO_OPERATOR, "abc"));
    }

    @Test
    public void testNonMdmTypesDontUseSubtype() {
        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.MEASUREMENT, "abc", "MDM_ADMIN"))
        ).isEqualTo(pojo(MasterDataSourceType.MEASUREMENT, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.WAREHOUSE, "abc", "MDM_ADMIN"))
        ).isEqualTo(pojo(MasterDataSourceType.WAREHOUSE, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.SUPPLIER, "abc", "MDM_ADMIN"))
        ).isEqualTo(pojo(MasterDataSourceType.SUPPLIER, "abc"));

        Assertions.assertThat(
            MasterDataSourceType.proto2pojo(proto(MdmIrisPayload.MasterDataSource.AUTO, "abc", "MDM_ADMIN"))
        ).isEqualTo(pojo(MasterDataSourceType.AUTO, "abc"));
    }

    @Test
    public void testMdmSourceConvertsToSubtype() {
        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.MDM_ADMIN, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_ADMIN"));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.TOOL, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "TOOL"));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.MDM_OPERATOR, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_OPERATOR"));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.MSKU_INHERIT, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MSKU_INHERIT"));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.IRIS_MDM, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "IRIS_MDM"));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.MBO_OPERATOR, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MBO_OPERATOR"));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.MDM_UNKNOWN, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_UNKNOWN"));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.MDM_DEFAULT, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MDM, "abc", "MDM_DEFAULT"));
    }

    @Test
    public void testNonMdmSourcesDontConvertToSubtype() {
        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.AUTO, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.AUTO, "abc", ""));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.WAREHOUSE, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.WAREHOUSE, "abc", ""));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.MEASUREMENT, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.MEASUREMENT, "abc", ""));

        Assertions.assertThat(
            MasterDataSourceType.pojo2proto(pojo(MasterDataSourceType.SUPPLIER, "abc"))
        ).isEqualTo(proto(MdmIrisPayload.MasterDataSource.SUPPLIER, "abc", ""));
    }

    @Test
    public void testExtractOriginalMasterDataSource() {
        Assertions.assertThat(MasterDataSourceType.extractOriginalMasterDataSource(
            new MasterDataSource(MasterDataSourceType.WAREHOUSE, "145"))).isEqualTo(
            new MasterDataSource(MasterDataSourceType.WAREHOUSE, "145"));

        Assertions.assertThat(MasterDataSourceType.extractOriginalMasterDataSource(
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "577858"))).isEqualTo(
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "577858"));

        Assertions.assertThat(MasterDataSourceType.extractOriginalMasterDataSource(
            new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, "dmserebr"))).isEqualTo(
            new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, "dmserebr"));

        Assertions.assertThat(MasterDataSourceType.extractOriginalMasterDataSource(
            new MasterDataSource(MasterDataSourceType.MSKU_INHERIT,
                "msku:123456 supplier_id:1234 shop_sku:QWJKEH-asdklQW warehouse:145"))).isEqualTo(
            new MasterDataSource(MasterDataSourceType.WAREHOUSE, "145"));

        Assertions.assertThat(MasterDataSourceType.extractOriginalMasterDataSource(
            new MasterDataSource(MasterDataSourceType.MSKU_INHERIT,
                "msku:123456 supplier_id:1234 shop_sku:QWJKEH-asdklQW measurement:172"))).isEqualTo(
            new MasterDataSource(MasterDataSourceType.MEASUREMENT, "172"));

        Assertions.assertThat(MasterDataSourceType.extractOriginalMasterDataSource(
            new MasterDataSource(MasterDataSourceType.MSKU_INHERIT,
                "msku:123456 supplier_id:1234 shop_sku:QWJKEH-asdklQW supplier:577858"))).isEqualTo(
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "577858"));

        Assertions.assertThat(MasterDataSourceType.extractOriginalMasterDataSource(
            new MasterDataSource(MasterDataSourceType.MSKU_INHERIT,
                "supplier_id:1234 shop_sku:QWJKEH-asdklQW supplier:577858"))).isEqualTo(
            new MasterDataSource(MasterDataSourceType.MSKU_INHERIT,
                "supplier_id:1234 shop_sku:QWJKEH-asdklQW supplier:577858"));
    }

    private static MasterDataSource pojo(MasterDataSourceType type, String sourceId) {
        return new MasterDataSource(type, sourceId);
    }

    private static MdmIrisPayload.Associate proto(MdmIrisPayload.MasterDataSource type, String srcId, String subtype) {
        return MdmIrisPayload.Associate.newBuilder()
            .setType(type)
            .setSubtype(subtype)
            .setId(srcId)
            .build();
    }
}
