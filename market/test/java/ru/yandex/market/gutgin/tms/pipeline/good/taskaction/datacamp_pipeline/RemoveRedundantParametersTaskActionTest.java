package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.ParameterInfoBuilder;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.*;

public class RemoveRedundantParametersTaskActionTest extends DBDcpStateGenerator {

    private CategoryInfoProducer categoryInfoProducer;
    private RemoveRedundantParametersTaskAction removeRedundantParametersTaskAction;
    private CategoryData categoryData;
    private static final long MIGRATED_PARAM_FROM_1 = 222L;
    private static final long MIGRATED_PARAM_TO_1 = 333L;
    private static final long MIGRATED_PARAM_FROM_2 = 444L;
    private static final long MIGRATED_PARAM_TO_2 = 555L;
    private static final Set<Long> GOOD_PARAMETERS =
            Sets.newHashSet(1L, 2L, MIGRATED_PARAM_FROM_1, MIGRATED_PARAM_TO_1);
    private static final Set<Long> REDUNDANT_PARAMETERS =
            Sets.newHashSet(3L, MIGRATED_PARAM_FROM_2, MIGRATED_PARAM_TO_2);

    @Before
    public void setUp() {
        super.setUp();
        this.categoryInfoProducer = getCategoryInfoProducer();
        removeRedundantParametersTaskAction = new RemoveRedundantParametersTaskAction(categoryInfoProducer,
                gcSkuTicketDao,
                gcSkuValidationDao);
    }

    private CategoryInfoProducer getCategoryInfoProducer() {
        CategoryInfoProducer categoryInfoProducer = Mockito.mock(CategoryInfoProducer.class);
        Long2LongMap migratedParamMap = new Long2LongOpenHashMap();
        migratedParamMap.put(MIGRATED_PARAM_FROM_1, MIGRATED_PARAM_TO_1);
        migratedParamMap.put(MIGRATED_PARAM_FROM_2, MIGRATED_PARAM_TO_2);
        this.categoryData = mock(CategoryData.class);
        when(categoryData.getHid()).thenReturn(CATEGORY_ID);

        when(categoryInfoProducer.extractCategoryInfo(any(), anyLong(), any()))
                .thenReturn(CategoryInfo.newBuilder()
                        .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_UI))
                        .addParameter(
                                ParameterInfoBuilder.asNumeric()
                                        .setId(1)
                                        .setName("1")
                                        .setXslName("1")
                                        .setImportContentType(ImportContentType.DCP_UI)
                                        .build()
                        )
                        .addParameter(
                                ParameterInfoBuilder.asString()
                                        .setId(2)
                                        .setName("2")
                                        .setXslName("2")
                                        .setImportContentType(ImportContentType.DCP_UI)
                                        .build()
                        )
                        .addParameter(
                                ParameterInfoBuilder.asString()
                                        .setId(MIGRATED_PARAM_TO_1)
                                        .setName("555")
                                        .setXslName("555")
                                        .setImportContentType(ImportContentType.DCP_UI)
                                        .build()
                        )
                        .setId(1L)
                        .setMigratedParams(migratedParamMap)
                        .build(ImportContentType.DCP_UI));
       when(categoryInfoProducer.getCategoryData(CATEGORY_ID)).thenReturn(categoryData);
        return categoryInfoProducer;
    }

    @Test
    public void testAllOffersHaveCorrectParameters() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addNumericParam(1, "1", "50", builder);
                addStrParam(2, "2","2", builder);
            });
            initOffer(CATEGORY_ID, offers.get(1), builder -> {
                addNumericParam(1, "1", "150", builder);
                addStrParam(2, "2","2", builder);
            });
            initOffer(CATEGORY_ID, offers.get(2), builder -> {
                addNumericParam(1, "1", "250", builder);
                addStrParam(2, "2","2", builder);
                addStrParam((int) MIGRATED_PARAM_FROM_1, "222", "222", builder);
            });
        });

        ProcessTaskResult<ProcessDataBucketData> result = this.removeRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(gcSkuTickets
                        .stream()
                        .map(GcSkuTicket::getId)
                        .toArray(Long[]::new));

        assertThat(tickets.size()).isEqualTo(gcSkuTickets.size());
        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .noneMatch(param -> REDUNDANT_PARAMETERS.contains(param.getParamId()))).isTrue());

        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .allMatch(param -> GOOD_PARAMETERS.contains(param.getParamId()))).isTrue());
    }

    @Test
    public void testOffersHaveRedundantParameters() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addNumericParam(1, "1", "50", builder);
            });
            initOffer(CATEGORY_ID, offers.get(1), builder -> {
                addNumericParam(1, "1", "150", builder);
                addStrParam(2, "2","ValueNumberTwo", builder);
            });
            initOffer(CATEGORY_ID, offers.get(2), builder -> {
                addNumericParam(1, "1", "250", builder);
                addStrParam(2, "2","ValueNumberTwoAndAHalf", builder);
                addStrParam(3, "3","3", builder);
                addStrParam((int) MIGRATED_PARAM_FROM_2, "444","444", builder);
            });
        });

        ProcessTaskResult<ProcessDataBucketData> result = this.removeRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(gcSkuTickets
                .stream()
                .map(GcSkuTicket::getId)
                .toArray(Long[]::new));

        assertThat(tickets.size()).isEqualTo(gcSkuTickets.size());

        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .noneMatch(param -> REDUNDANT_PARAMETERS.contains(param.getParamId()))).isTrue());

        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .allMatch(param -> GOOD_PARAMETERS.contains(param.getParamId()))).isTrue());
    }

    @Test
    public void testOffersHaveRedundantMigratedParameters() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addNumericParam(1, "1", "50", builder);
            });
            initOffer(CATEGORY_ID, offers.get(1), builder -> {
                addNumericParam(1, "1", "150", builder);
                addStrParam(2, "2","ValueNumberTwo", builder);
            });
            initOffer(CATEGORY_ID, offers.get(2), builder -> {
                addNumericParam(1, "1", "250", builder);
                addStrParam(2, "2","ValueNumberTwoAndAHalf", builder);
                addStrParam(3, "3","3", builder);
                addStrParam((int) MIGRATED_PARAM_FROM_2, "444","444", builder);
                addStrParam(222, "222", "222", builder);
                addStrParam(333, "333", "222", builder);
            });
        });

        ProcessTaskResult<ProcessDataBucketData> result = this.removeRedundantParametersTaskAction
                .runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
        assertThat(result.getResult().getDataBucketId()).isEqualTo(dataBucketId);

        List<GcSkuTicket> tickets = gcSkuTicketDao.fetchById(gcSkuTickets
                .stream()
                .map(GcSkuTicket::getId)
                .toArray(Long[]::new));

        assertThat(tickets.size()).isEqualTo(gcSkuTickets.size());

        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .noneMatch(param -> REDUNDANT_PARAMETERS.contains(param.getParamId()))).isTrue());

        tickets.forEach(ticket ->
                assertThat(ticket.getDatacampOffer().getContent().getPartner()
                        .getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                        .allMatch(param -> GOOD_PARAMETERS.contains(param.getParamId()))).isTrue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorOnOffersHaveMultipleCategories() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addNumericParam(1, "1", "50", builder);
            });
            initOffer(CATEGORY_ID, offers.get(1), builder -> {
                addNumericParam(2, "2", "150", builder);
            });
        });
        gcSkuTickets.get(0).setCategoryId(111111L);
        gcSkuTickets.get(1).setCategoryId(222222L);

        this.removeRedundantParametersTaskAction.runOnTickets(gcSkuTickets, new ProcessDataBucketData(dataBucketId));
    }
}
