package ru.yandex.market.partner.content.common;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.config.CommonTestConfig;
import ru.yandex.market.partner.content.common.db.dao.AbstractProcessDao;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.dao.PartnerContentDao;
import ru.yandex.market.partner.content.common.db.dao.PartnerPictureService;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.WaitUntilFinishedPipelineDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcRawSkuDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.TicketProcessRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Source;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileOptionalResultData;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;
import ru.yandex.market.partner.content.common.helpers.BusinessIdXlsExtractor;
import ru.yandex.market.partner.content.common.service.mappings.PartnerShopService;
import ru.yandex.market.robot.db.ParameterValueComposer;
import ru.yandex.market.test.util.random.RandomBean;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class, classes = CommonTestConfig.class)
@Transactional
public abstract class BaseDcpExcelDBStateGenerator {
    protected static final long SEED = 1234567L;
    protected static final int PARTNER_SHOP_ID = 123;
    protected static final long CATEGORY_ID = 1234L;
    protected static final int SOURCE_ID = 4321;
    protected static final int BUSINESS_ID = 665544;

    protected EnhancedRandom generator;

    protected long requestId;
    protected long processId;
    protected ProcessFileData processFileData;
    protected ProcessFileOptionalResultData processFileOptionalResultData;


    protected static final MboParameters.Category CATEGORY = MboParameters.Category.newBuilder()
        .setHid(CATEGORY_ID)
        .addName(MboParameters.Word.newBuilder().setName("Category " + CATEGORY_ID).setLangId(225))
        .addParameter(MboParameters.Parameter.newBuilder()
            .setId(ParameterValueComposer.VENDOR_ID).setXslName(ParameterValueComposer.VENDOR)
            .setValueType(MboParameters.ValueType.ENUM)
            .addName(MboParameters.Word.newBuilder().setLangId(225).setName("производитель")))
        .addParameter(MboParameters.Parameter.newBuilder()
            .setId(ParameterValueComposer.NAME_ID).setXslName(ParameterValueComposer.NAME)
            .setValueType(MboParameters.ValueType.STRING))
        .addParameter(MboParameters.Parameter.newBuilder()
            .setId(ParameterValueComposer.BARCODE_ID).setXslName(ParameterValueComposer.BARCODE)
            .setValueType(MboParameters.ValueType.STRING))
        .addParameter(MboParameters.Parameter.newBuilder()
            .setId(ParameterValueComposer.VENDOR_CODE_ID).setXslName(ParameterValueComposer.VENDOR_CODE)
            .setValueType(MboParameters.ValueType.STRING))
        .addParameter(MboParameters.Parameter.newBuilder()
            .setId(ParameterValueComposer.ALIASES_ID).setXslName(ParameterValueComposer.ALIASES)
            .setValueType(MboParameters.ValueType.STRING))
        .build();

    @Autowired
    @Qualifier("jooq.config.configuration")
    protected Configuration configuration;
    @Autowired
    protected PartnerContentDao partnerContentDao;
    @Autowired
    protected FileProcessDao fileProcessDao;
    @Autowired
    protected SourceDao sourceDao;
    @Autowired
    protected PartnerShopService partnerShopService;
    @Autowired
    protected TicketProcessRequestDao ticketProcessRequestDao;
    @Autowired
    protected AbstractProcessDao abstractProcessDao;
    @Autowired
    protected PartnerPictureService partnerPictureService;
    @Autowired
    protected BusinessIdXlsExtractor businessIdXlsExtractor;
    @Autowired
    protected FileDataProcessRequestDao fileDataProcessRequestDao;
    @Autowired
    protected WaitUntilFinishedPipelineDao waitUntilFinishedPipelineDao;
    @Autowired
    GcRawSkuDao gcRawSkuDao;

    @Before
    public void setUp() {
        requestId = createFileDataProcessRequest(SOURCE_ID);
        processId = createFileProcessId(requestId);
        processFileData = new ProcessFileData(requestId, processId);
        processFileOptionalResultData = new ProcessFileOptionalResultData(requestId, processId, true);
        generator = RandomBean.defaultRandom();
        generator.setSeed(SEED);

        createSource(SOURCE_ID, BUSINESS_ID);
    }

    protected DSLContext dsl() {
        return DSL.using(configuration);
    }

    protected void createSource(int sourceId, @Nullable Integer partnerShopId) {
        sourceDao.insert(new Source(sourceId, "Source " + sourceId, null, partnerShopId, false, null));
    }

    protected long createFileProcessId(long fileDataProcessRequestId) {
        return fileProcessDao.insertNewFileProcess(fileDataProcessRequestId, ProcessType.DCP_FILE_PROCESS);
    }

    protected long createAbstractProcessId(long requestId, ProcessType processType) {
        return abstractProcessDao.create(requestId, processType);
    }


    protected long createFileDataProcessRequest(int sourceId) {
        return createFileDataProcessRequest(sourceId, null, BUSINESS_ID);
    }

    protected long createFileDataProcessRequest(int sourceId, int businessId) {
        return createFileDataProcessRequest(sourceId, null, businessId);
    }

    protected long createFileDataProcessRequest(int sourceId, Boolean ignoreWhiteBackgroundCheck, int businessId) {
        return partnerContentDao.createFileDataProcessRequest(
            sourceId, businessId, "http://some.url", false, FileType.DCP_SINGLE_EXCEL,
            null, ignoreWhiteBackgroundCheck, false, false);
    }

    public List<GcRawSku> generateGcRawSkus(int amount) {
        return generateGcRawSkus(amount, rawSkus -> { });
    }

    public List<GcRawSku> generateGcRawSkus(int amount, Long processId) {
        return generateGcRawSkus(amount, processId, rawSkus -> { });
    }

    public List<GcRawSku> generateGcRawSkus(int amount, Consumer<List<RawSku>> skuSettings) {
        return generateGcRawSkus(amount, null, skuSettings);
    }

    public List<GcRawSku> generateGcRawSkus(int amount, Long processId, Consumer<List<RawSku>> skuSettings) {
        AtomicInteger uniqualizer = new AtomicInteger(0);

        Iterator<String> uniqSKUNamesIterator = generator.objects(String.class, amount)
                .collect(Collectors.toList()).iterator();

        Iterator<Integer> uniqGroupIdIterator = generator.objects(Integer.class, amount)
                .collect(Collectors.toList()).iterator();

        List<RawSku> rawSkus = generator.objects(RawSku.class, amount)
                .peek(s -> s.setGroupId(uniqGroupIdIterator.next().toString()))
                .peek(s -> s.setCategoryId(CATEGORY_ID))
                .peek(s -> s.setShopSku(uniqSKUNamesIterator.next() + "_" + uniqualizer.get()))
                .peek(s -> s.setRowIndex(uniqualizer.getAndIncrement()))
                .collect(Collectors.toList());
        skuSettings.accept(rawSkus);


        if (processId == null) {
            processId = this.processId;
        }
        gcRawSkuDao.saveRawSkus(processId, rawSkus);

        return gcRawSkuDao.fetchByFileProcessId(processId);
    }
}
