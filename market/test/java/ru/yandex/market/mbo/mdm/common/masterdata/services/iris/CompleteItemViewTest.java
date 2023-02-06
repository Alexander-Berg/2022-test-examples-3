package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.lightmapper.ProtobufMapper;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class CompleteItemViewTest extends MdmBaseDbTestClass {

    private static final int SEED = 3;

    @Autowired
    private ReferenceItemRepository referenceItemRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private MdmSupplierRepository supplierRepository;

    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void name() {
        MdmIrisPayload.MdmIdentifier itemId = random.nextObject(MdmIrisPayload.MdmIdentifier.class);
        itemId = itemId.toBuilder().setSupplierId(random.nextInt()).build();
        MdmIrisPayload.ReferenceInformation information = random.nextObject(MdmIrisPayload.ReferenceInformation.class);

        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(itemId)
            .addInformation(information)
            .build();

        MdmIrisPayload.CompleteItem completeItem = MdmIrisPayload.CompleteItem.newBuilder()
            .setItemId(itemId)
            .addTrustworthyInformation(information)
            .build();

        supplierRepository.insert(new MdmSupplier().setId((int) itemId.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        referenceItemRepository.insert(new ReferenceItemWrapper(item));

        ProtobufMapper<MdmIrisPayload.CompleteItem> mapper = new ProtobufMapper<>(
            () -> MdmIrisPayload.CompleteItem.getDefaultInstance()
        );

        List<MdmIrisPayload.CompleteItem> found = jdbcTemplate.query(
            "select complete_item" +
                " from mdm.v_reference_complete_item" +
                " where supplier_id = :supplierId and shop_sku = :shopSku",
            ImmutableMap.of("supplierId", itemId.getSupplierId(), "shopSku", itemId.getShopSku()),
            mapper.toRowMapper()
        );

        Assertions.assertThat(found).containsExactly(completeItem);
    }
}
