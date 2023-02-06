package ru.yandex.market.mboc.tms.service.uee;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.micrometer.core.instrument.Metrics;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.ir.uee.model.UserRun;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferToSmStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.OfferToSm;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.SmBatch;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.queue.ForSMSuggestQueueRepository;
import ru.yandex.market.mboc.common.services.idxapi.pics.PicrobotApiServiceMock;
import ru.yandex.market.mboc.common.services.idxapi.pics.dto.ImageSignatureLayer;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.uee.repository.OfferToSmRepository;
import ru.yandex.market.mboc.common.uee.repository.SmBatchRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createOffer;

public class UeeSendServiceTest extends BaseDbTestClass {
    private static final String BASE_PATH = "//tmp/uee";
    private static final int DEFAULT_ACCOUNT = 1;
    private static final String DEFAULT_YT_POOL = "default";
    private static final String DEFAULT_YT_CLUSTER = "hahn";
    private static final int BATCH_TIMEOUT_SEC = 1;
    public static final String IMAGE1 = "http://image1.png";

    private TestYt testYt;
    private UeeSendService ueeSendService;
    private UeeServiceMock ueeService;
    private PicrobotApiServiceMock picrobotApiService;

    @Resource
    private OfferRepository offerRepository;
    @Resource
    private OfferToSmRepository offerToSmRepository;
    @Resource
    private SmBatchRepository smBatchRepository;
    @Resource
    private SupplierRepository supplierRepository;
    @Resource
    private ForSMSuggestQueueRepository forSMSuggestQueueRepository;


    @Before
    public void setUp() {
        testYt = new TestYt();
        StorageKeyValueServiceMock storageKeyValueService = new StorageKeyValueServiceMock();
        storageKeyValueService.putValue("smartmatcher.confidence.threshold.default", 0.0);
        UeeYtService ueeYtService = new UeeYtServiceImpl(UnstableInit.simple(testYt), BASE_PATH, storageKeyValueService);
        ueeService = new UeeServiceMock();
        picrobotApiService = new PicrobotApiServiceMock();
        picrobotApiService.putSignature(IMAGE1, Map.of(ImageSignatureLayer.SIGNATURE_V6, "1.1 0.002 0.2"));
        StorageKeyValueService keyValueServiceMock = new StorageKeyValueServiceMock();
        keyValueServiceMock.putValue(UeeSendService.UEE_ACCOUNT_ID_KV_KEY, DEFAULT_ACCOUNT);
        keyValueServiceMock.putValue(UeeSendService.UEE_YT_POOL_KV_KEY, DEFAULT_YT_POOL);
        keyValueServiceMock.putValue(UeeSendService.SM_VERSION_KV_KEY, "default");
        keyValueServiceMock.putValue(UeeSendService.INPUT_DUMP_DIR_KV_KEY, "//home/market/testing/ir/mboc-offers-dump");
        keyValueServiceMock.putValue(UeeSendService.OUTPUT_DUMP_DIR_KV_KEY, "//home/market/testing/ir/mboc-sm-output-dump");
        ueeSendService = new UeeSendService(
            Metrics.globalRegistry,
            ueeYtService,
            offerToSmRepository,
            offerRepository,
            smBatchRepository,
            ueeService,
            forSMSuggestQueueRepository,
            picrobotApiService,
            transactionTemplate,
            keyValueServiceMock,
            DEFAULT_YT_CLUSTER,
            5,
            BATCH_TIMEOUT_SEC);
    }

    @Test
    public void sendSuggest() throws InterruptedException {
        Supplier supplier1 = new Supplier(1, "Test supplier", null, null);
        supplierRepository.insert(supplier1);
        Offer offer = createOffer(supplier1);
        offer.storeOfferContent(OfferContent.builder().sourcePicUrls(IMAGE1).description("description").build());
        offer.setProcessingStatusInternal(Offer.ProcessingStatus.IN_SMARTMATCHER);
        offerRepository.insertOffer(offer);

        Thread.sleep(TimeUnit.SECONDS.toMillis(BATCH_TIMEOUT_SEC + 1));
        ueeSendService.sendSuggest(List.of(offer.getId()));

        Optional<SmBatch> smBatchOpt = smBatchRepository.findNotEnded().stream()
            .max(Comparator.comparing(SmBatch::getId));

        assertThat(smBatchOpt).isPresent();

        SmBatch smBatch = smBatchOpt.get();

        assertThat(smBatch.getEndTs()).isNull();
        assertThat(smBatch.getStartTs()).isNotNull();
        assertThat(smBatch.getYtRequestPath()).startsWith(BASE_PATH);

        Integer userRunId = smBatch.getUserRunId();
        UserRun userRun = ueeService.getUserRun(userRunId);

        assertThat(userRun).isNotNull();
        assertThat(userRun.getFieldMappings()).containsOnlyKeys(
            UeeInputTableDefinition.REQUIRED_COLUMNS.stream()
                .map(UeeInputTableDefinition.Columns::getColumnName)
                .collect(Collectors.toList())
        );
        assertThat(userRun.getNotificationRecipients()).isNotEmpty();

        List<OfferToSm> offerToSms = offerToSmRepository.find(
            OfferToSmRepository.Filter.builder().smBatchId(smBatch.getId()).build()
        );

        assertThat(offerToSms).hasSize(1).singleElement()
            .extracting(OfferToSm::getOfferId, OfferToSm::getStatus)
            .containsExactly(offer.getId(), OfferToSmStatus.SENT);

        Offer offerSaved = offerRepository.getOfferById(offer.getId());

        assertThat(offerSaved)
            .extracting(Offer::getProcessingStatus)
            .isEqualTo(Offer.ProcessingStatus.IN_SMARTMATCHER);

        assertThat(testYt.cypress().exists(YPath.simple(smBatch.getYtRequestPath()))).isTrue();

        List<YTreeMapNode> nodeList = testYt.tables().readToList(YPath.simple(smBatch.getYtRequestPath()),
            YTableEntryTypes.YSON);

        assertThat(nodeList).hasSize(1);

        assertThat(nodeList.get(0).asMap())
            .containsOnlyKeys(
                UeeInputTableDefinition.Columns.OFFER_ID.getColumnName(),
                UeeInputTableDefinition.Columns.TITLE.getColumnName(),
                UeeInputTableDefinition.Columns.VENDOR_ID.getColumnName(),
                UeeInputTableDefinition.Columns.CATEGORY_ID.getColumnName(),
                UeeInputTableDefinition.Columns.CURRENT_THRESHOLD.getColumnName(),
                UeeInputTableDefinition.Columns.DESCRIPTION.getColumnName(),
                UeeInputTableDefinition.Columns.SIGNATURES.getColumnName()
            );
    }
}
