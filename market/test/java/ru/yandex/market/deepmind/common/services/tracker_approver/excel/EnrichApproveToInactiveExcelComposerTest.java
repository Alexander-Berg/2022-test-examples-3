package ru.yandex.market.deepmind.common.services.tracker_approver.excel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;
import ru.yandex.market.mbo.excel.ExcelFile;

import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.COREFIX_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.HEADERS;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.INACTIVE_REASON_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.MSKU_ID_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.MSKU_TITLE_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.SHOP_SKU_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.SUPPLIER_ID_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer.SUPPLIER_KEY;

public class EnrichApproveToInactiveExcelComposerTest extends DeepmindBaseDbTestClass {

    private final Long mskuId = 11112L;
    private final String ticket = "TEST-1";
    @Autowired
    TrackerApproverDataRepository trackerApproverDataRepository;
    @Autowired
    TrackerApproverTicketRepository trackerApproverTicketRepository;
    private EnrichApproveToInactiveExcelComposer enrichApproveToInactiveExcelComposer;
    private Msku msku;
    private Supplier supplier;
    private MskuInfo mskuInfo;

    @Before
    public void setup() {
        int supplierId = 1;
        var serviceOfferReplica = new ServiceOfferReplica()
            .setSupplierId(supplierId)
            .setShopSku("a")
            .setMskuId(mskuId);
        msku = new Msku()
            .setTitle("Test msku 1")
            .setId(mskuId);

        mskuInfo = new MskuInfo().setMarketSkuId(mskuId).setInTargetAssortment(true);

        supplier = new Supplier().setSupplierType(SupplierType.FIRST_PARTY).setName("supplier name").setId(supplierId);

        MskuInfoRepository mskuInfoRepository = Mockito.mock(MskuInfoRepository.class);
        Mockito.when(mskuInfoRepository.findByIdsMap(Mockito.any()))
            .thenReturn(Map.of(mskuId, mskuInfo));

        ServiceOfferReplicaRepository serviceOfferReplicaRepository =
            Mockito.mock(ServiceOfferReplicaRepository.class);
        Mockito.when(serviceOfferReplicaRepository.findOffersByKeys(Mockito.anyList()))
            .thenReturn(List.of(serviceOfferReplica));

        var deepmindMskuRepository = Mockito.mock(MskuRepository.class);
        Mockito.when(deepmindMskuRepository.findMap(Mockito.any())).thenReturn(Map.of(mskuId, msku));

        SupplierRepository supplierRepository = Mockito.mock(SupplierRepository.class);
        Mockito.when(supplierRepository.findByIdsMap(Mockito.any())).thenReturn(Map.of(supplierId, supplier));

        enrichApproveToInactiveExcelComposer = new EnrichApproveToInactiveExcelComposer(
            deepmindMskuRepository, supplierRepository,
            serviceOfferReplicaRepository, mskuInfoRepository
        );
    }

    @Test
    public void testCorrectExcelFile() {
        String type = "test";
        trackerApproverTicketRepository.save(
            new TrackerApproverTicketRawStatus()
                .setTicket(ticket)
                .setState(TicketState.NEW)
                .setType(type)
                .setMeta(JsonWrapper.fromObject("deprecated version")));

        var sskuInRepo = List.of(
            new ServiceOfferKey(1, "a"));

        var trackerApproverData = sskuInRepo.stream()
            .map(key -> new TrackerApproverRawData()
                .setTicket(ticket)
                .setKey(JsonWrapper.fromObject(key))
                .setType(type)
            ).collect(Collectors.toList());

        trackerApproverDataRepository.save(trackerApproverData);

        ExcelFile actual = enrichApproveToInactiveExcelComposer.processKeys(sskuInRepo, "\"deprecated version\"");

        ExcelFile.Builder builder = new ExcelFile.Builder();

        for (int i = 0; i < HEADERS.size(); i++) {
            //row with column number
            builder.addHeader(HEADERS.get(i).getName());
            builder.setSubHeaderValue(1, i, i + 1);
        }
        int row = 2;

        builder.setValue(row, INACTIVE_REASON_KEY, "\"deprecated version\"");
        builder.setValue(row, MSKU_ID_KEY, mskuId);
        builder.setValue(row, SHOP_SKU_KEY, sskuInRepo.get(0).getShopSku());
        builder.setValue(row, SUPPLIER_ID_KEY, sskuInRepo.get(0).getSupplierId());
        builder.setValue(row, MSKU_TITLE_KEY, msku.getTitle());
        builder.setValue(row, SUPPLIER_KEY, supplier.getName());
        builder.setValue(row, COREFIX_KEY, mskuInfo.getInTargetAssortment() ? 1 : 0);

        ExcelFile expected = builder.build();
        Assertions.assertThat(actual).isEqualTo(expected);
    }
}
