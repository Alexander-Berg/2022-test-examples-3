package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_SKU_TICKET;

public class SkuSemanticChangeValidationTest extends DBDcpStateGenerator {


    private static final int SHOP_ID = 1;
    private static final long PSKU_ID = 1;

    private SkuSemanticChangeValidation validation;

    @Before
    public void setUp() {
        super.setUp();
        CategoryDataHelper categoryDataHelper = Mockito.mock(CategoryDataHelper.class);
        ModelStorageHelper modelStorageHelper = Mockito.mock(ModelStorageHelper.class);
        MboMappingsServiceHelper mboMappingsServiceHelper = Mockito.mock(MboMappingsServiceHelper.class);

        Mockito.when(modelStorageHelper.findModelsMap(Mockito.anyCollection())).thenReturn(mboResponse());
        Mockito.when(mboMappingsServiceHelper.searchBaseOfferMappingsByMarketSkuId(Mockito.anyCollection(),
                Mockito.eq(Collections.singletonList(MboMappings.MappingKind.APPROVED_MAPPING))))
                .thenReturn(mboCategoryResponse());

        validation = new SkuSemanticChangeValidation(gcSkuValidationDao, gcSkuTicketDao,
                modelStorageHelper, mboMappingsServiceHelper, Collections.emptySet(), categoryDataHelper, 0.5);
    }

    @Test
    public void testSameNameWithNoForeignMapping() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(4, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                        .getTitleBuilder().setValue("Монитор DELL S2721D 27\", серый").build();
            });
            initOffer(CATEGORY_ID, offers.get(1), builder -> {
                builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                        .getTitleBuilder().setValue("Монитор DELL S2721D 27\", серый").build();
            });
            initOffer(CATEGORY_ID, offers.get(2), builder -> {
                builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                        .getTitleBuilder().setValue("Монитор DELL S2721D 27\", серый").build();
            });
            initOffer(CATEGORY_ID, offers.get(3), builder -> {
                builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                        .getTitleBuilder().setValue("Монитор DELL S2721D 27\", серый").build();
            });
            offers.get(2).setBusinessId(SHOP_ID);
            offers.get(3).setBusinessId(SHOP_ID);

        });
        setExistingPskuId(gcSkuTickets.get(0).getId(), PSKU_ID);
        setExistingPskuId(gcSkuTickets.get(1).getId(), PSKU_ID + 1);
        setExistingPskuId(gcSkuTickets.get(2).getId(), PSKU_ID + 2);
        setExistingPskuId(gcSkuTickets.get(3).getId(), PSKU_ID + 3);
        gcSkuTickets = gcSkuTicketDao.findAll();


        ProcessTaskResult<List<TicketValidationResult>> result = validation.validate(gcSkuTickets);

        assertThat(result.getResult().size()).isEqualTo(4);
        assertThat(result.getResult().stream().filter(TicketValidationResult::isValid).count()).isEqualTo(3);
        assertThat(result.getResult().stream().filter(t -> !t.isValid()).count()).isEqualTo(1);
    }

    @Test
    public void testDifferentNameWithNoForeignMapping() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                        .getTitleBuilder().setValue("Монитор DELL S2721D 27\", серый").build();
            });
        });

        setExistingPskuId(gcSkuTickets.get(0).getId(), PSKU_ID + 1);
        gcSkuTickets = gcSkuTicketDao.findAll();

        ProcessTaskResult<List<TicketValidationResult>> result = validation.validate(gcSkuTickets);

        assertThat(result.getResult().size()).isEqualTo(1);
        assertThat(result.getResult().stream().filter(TicketValidationResult::isValid).count()).isEqualTo(1);
    }

    @Test
    public void testDifferentNameWithSelfMapping() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                        .getTitleBuilder().setValue("Монитор DELL S2721D 27\", серый").build();
            });
            offers.get(0).setBusinessId(SHOP_ID);

        });

        setExistingPskuId(gcSkuTickets.get(0).getId(), PSKU_ID + 2);
        gcSkuTickets = gcSkuTicketDao.findAll();

        ProcessTaskResult<List<TicketValidationResult>> result = validation.validate(gcSkuTickets);

        assertThat(result.getResult().size()).isEqualTo(1);
        assertThat(result.getResult().stream().filter(TicketValidationResult::isValid).count()).isEqualTo(1);
    }

    @Test
    public void testDifferentNameWithForeignMapping() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                        .getTitleBuilder().setValue("Монитор DELL S2721D 27\", серый").build();
            });
            offers.get(0).setBusinessId(SHOP_ID);
        });

        setExistingPskuId(gcSkuTickets.get(0).getId(), PSKU_ID + 3);
        gcSkuTickets = gcSkuTicketDao.findAll();

        ProcessTaskResult<List<TicketValidationResult>> result = validation.validate(gcSkuTickets);

        assertThat(result.getResult().size()).isEqualTo(1);
        assertThat(result.getResult().stream().filter(t -> !t.isValid()).count()).isEqualTo(1);
    }

    private Map<Long, ModelStorage.Model> mboResponse() {
        Map<Long, ModelStorage.Model> mboResponse = new HashMap<>();
        ModelStorage.Model.Builder skuBuilder1 = ModelStorage.Model.newBuilder();
        ModelStorage.Model.Builder skuBuilder2 = ModelStorage.Model.newBuilder();
        ModelStorage.Model.Builder skuBuilder3 = ModelStorage.Model.newBuilder();
        ModelStorage.Model.Builder skuBuilder4 = ModelStorage.Model.newBuilder();

        //одинаковые имена(валидный)
        skuBuilder1.addTitlesBuilder().setValue("Монитор DELL S2721D 27\", серый");
        skuBuilder1.setId(PSKU_ID);
        mboResponse.put(PSKU_ID, skuBuilder1.build());

        //разные имена, но нет маппинга(валидный)
        skuBuilder2.addTitlesBuilder().setValue("no name");
        skuBuilder2.setId(PSKU_ID + 1);
        mboResponse.put(PSKU_ID + 1, skuBuilder2.build());

        //разные имена, есть свой маппинг(валидный)
        skuBuilder3.addTitlesBuilder().setValue("no name");
        skuBuilder3.setId(PSKU_ID + 2);
        mboResponse.put(PSKU_ID + 2, skuBuilder3.build());

        //разные имена, есть чужой маппинг(не валидный)
        skuBuilder4.addTitlesBuilder().setValue("no name");
        skuBuilder4.setId(PSKU_ID + 3);
        mboResponse.put(PSKU_ID + 3, skuBuilder4.build());

        return mboResponse;
    }

    private List<SupplierOffer.Offer> mboCategoryResponse() {
        SupplierOffer.Offer.Builder builder1 = SupplierOffer.Offer.newBuilder();
        SupplierOffer.Offer.Builder builder2 = SupplierOffer.Offer.newBuilder();
        builder1.setSupplierId(SHOP_ID);
        builder2.setSupplierId(SHOP_ID + 100500);
        builder1.getApprovedMappingBuilder().setSkuId(PSKU_ID + 2).build();
        builder2.getApprovedMappingBuilder().setSkuId(PSKU_ID + 3).build();

        return Arrays.asList(builder1.build(), builder2.build());
    }

    private void setExistingPskuId(Long ticketId, Long pskuId) {
        gcSkuTicketDao.dsl().update(GC_SKU_TICKET)
                .set(GC_SKU_TICKET.EXISTING_MBO_PSKU_ID, pskuId)
                .set(GC_SKU_TICKET.UPDATE_DATE, gcSkuTicketDao.now())
                .where(GC_SKU_TICKET.ID.eq(ticketId))
                .execute();
    }
}
