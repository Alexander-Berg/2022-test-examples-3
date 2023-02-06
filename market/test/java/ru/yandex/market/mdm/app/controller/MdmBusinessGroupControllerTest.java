package ru.yandex.market.mdm.app.controller;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessGroupForUi;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierForUi;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

/**
 * @author dmserebr
 * @date 18/05/2021
 */
public class MdmBusinessGroupControllerTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    private MdmBusinessGroupController controller;

    @Before
    public void before() {
        controller = new MdmBusinessGroupController(mdmSupplierCachingService, mdmSupplierRepository);
    }

    @Test
    public void testGetBusinessGroupOk() {
        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(1).setName("ООО Скаляр").setType(MdmSupplierType.BUSINESS),
            new MdmSupplier().setId(10).setName("ООО Вектор").setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(1).setBusinessEnabled(true),
            new MdmSupplier().setId(11).setName("ООО Тензор").setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(1).setBusinessEnabled(true)
        );

        ResponseEntity<MdmBusinessGroupForUi> response = controller.get(10);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isEqualTo(
            new MdmBusinessGroupForUi(
                new MdmSupplierForUi(1, "ООО Скаляр", MdmSupplierType.BUSINESS, null),
                List.of(
                    new MdmSupplierForUi(10, "ООО Вектор", MdmSupplierType.THIRD_PARTY, true),
                    new MdmSupplierForUi(11, "ООО Тензор", MdmSupplierType.THIRD_PARTY, true)
                )
            )
        );
    }

    @Test
    public void testGetBusinessGroupNotFound() {
        ResponseEntity<MdmBusinessGroupForUi> response = controller.get(12345);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(response.getBody()).isNull();
    }
}
