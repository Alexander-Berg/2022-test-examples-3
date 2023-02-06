package ru.yandex.market.mboc.tms.web.test;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.mboc.app.security.SecuredRoles;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.users.UserRoles;
import ru.yandex.market.mboc.tms.executors.ImportMbiPartnersExecutor;
import ru.yandex.market.mboc.tms.executors.UploadApprovedMappingsErpExecutor;
import ru.yandex.market.mboc.tms.executors.UploadApprovedMappingsMdmExecutor;
import ru.yandex.market.mboc.tms.executors.UploadApprovedMappingsYtExecutor;

@RequestMapping("/api/dev")
@SecuredRoles(UserRoles.DEVELOPER)
@RestController
public class TestController {
    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private final ImportMbiPartnersExecutor importMbiPartnersExecutor;
    private final SupplierRepository supplierRepository;
    private UploadApprovedMappingsYtExecutor uploadApprovedMappingsYtExecutor;
    private UploadApprovedMappingsErpExecutor uploadApprovedMappingsErpExecutor;
    private UploadApprovedMappingsMdmExecutor uploadApprovedMappingsMdmExecutor;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TestController(
        UploadApprovedMappingsYtExecutor uploadApprovedMappingsYtExecutor,
        UploadApprovedMappingsErpExecutor uploadApprovedMappingsErpExecutor,
        UploadApprovedMappingsMdmExecutor uploadApprovedMappingsMdmExecutor,
        ImportMbiPartnersExecutor importMbiPartnersExecutor,
        SupplierRepository supplierRepository) {
        this.uploadApprovedMappingsYtExecutor = uploadApprovedMappingsYtExecutor;
        this.uploadApprovedMappingsErpExecutor = uploadApprovedMappingsErpExecutor;
        this.uploadApprovedMappingsMdmExecutor = uploadApprovedMappingsMdmExecutor;
        this.importMbiPartnersExecutor = importMbiPartnersExecutor;
        this.supplierRepository = supplierRepository;
    }

    @GetMapping("/upload-approved-offers")
    public String uploadApprovedOffers() {
        uploadApprovedMappingsYtExecutor.doRealJob(null);
        uploadApprovedMappingsErpExecutor.doRealJob(null);
        uploadApprovedMappingsMdmExecutor.doRealJob(null);
        return "Done";
    }

    @GetMapping("/import-suppliers")
    public List<Supplier> testImportSuppliers() throws Exception {
        importMbiPartnersExecutor.doRealJob(null);
        return supplierRepository.findAll();
    }
}
