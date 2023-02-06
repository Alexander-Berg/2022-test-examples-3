package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import Market.UltraControllerServiceData.UltraController;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.pipeline.csku.OneStepValidationTaskAction;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.DataBucketTicketAutoValidation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.TitleLengthValidation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.TitleUpperCaseValidation;
import ru.yandex.market.gutgin.tms.utils.CategoryDataHelperMock;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;
import ru.yandex.market.partner.content.common.csku.util.OfferParametersActualizer;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.service.DataCampOfferBuilder;

import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.VALIDATION_STARTED;

public class OneStepValidationTaskActionTest extends DBDcpStateGenerator {

    @Test
    public void happyPath() {
        int ticketCount = 10;
        DataBucketTicketAutoValidation cskuTitleLengthValidation =
                new TitleLengthValidation(gcSkuValidationDao, gcSkuTicketDao);
        DataBucketTicketAutoValidation cskuTitleUpperCaseValidation =
                new TitleUpperCaseValidation(gcSkuValidationDao, gcSkuTicketDao);

        Set<DataBucketTicketAutoValidation> validations = Set.of(cskuTitleLengthValidation,
                cskuTitleUpperCaseValidation);

        OneStepValidationTaskAction oneStepValidationTaskAction = new OneStepValidationTaskAction(gcSkuTicketDao,
                gcSkuValidationDao, validations, Set.of());

        generateTickets(ticketCount, titleUppercaseWithHtmlTagsAndNoDescriptionSettings(), VALIDATION_STARTED);

        ProcessTaskResult<ProcessDataBucketData> validationResult =
                oneStepValidationTaskAction.apply(new ProcessDataBucketData(dataBucketId));

        Assert.assertNotNull(validationResult);
        Assert.assertNotNull(validationResult.getResult());
        Assert.assertEquals(validationResult.getResult().getDataBucketId(), dataBucketId);

        Long[] ticketIds = gcSkuTicketDao.getTicketsByDataBucket(dataBucketId).stream()
                .map(GcSkuTicket::getId).toArray(Long[]::new);

        List<GcSkuValidation> cskuTitleLengthValidationList =
                gcSkuValidationDao.getGcSkuValidations(cskuTitleLengthValidation.getValidationType(), ticketIds);

        Assert.assertEquals(ticketCount, cskuTitleLengthValidationList.size());


        List<GcSkuValidation> cskuTitleUpperCaseValidationList =
                gcSkuValidationDao.getGcSkuValidations(cskuTitleUpperCaseValidation.getValidationType(), ticketIds);

        Assert.assertEquals(ticketCount, cskuTitleUpperCaseValidationList.size());
    }

    private Consumer<DatacampOffer> titleUppercaseWithHtmlTagsAndNoDescriptionSettings() {
        return datacampOffer -> datacampOffer.setData(
                new DataCampOfferBuilder(
                        datacampOffer.getCreateTime(),
                        datacampOffer.getBusinessId(),
                        CATEGORY_ID,
                        datacampOffer.getOfferId()
                ).withActualNameAndTitle("TITLE_IN_UPPER_CASE_WITH<div></div>_HTML_TAGS")
                        .build()
        );
    }

    private Consumer<DatacampOffer> formalizedParametersOffer() {
        return datacampOffer -> datacampOffer.setData(
                new DataCampOfferBuilder(
                        datacampOffer.getCreateTime(),
                        datacampOffer.getBusinessId(),
                        CATEGORY_ID,
                        datacampOffer.getOfferId()
                ).withActualNameAndTitle("TITLE_IN_UPPER_CASE_WITH<div></div>_HTML_TAGS")
                        .withFormalizedParamPosition(UltraController.FormalizedParamPosition.newBuilder()
                                .setParamId(FORMILIZED_ID)
                                .setType(UltraController.FormalizedParamType.NUMERIC)
                                .setNumberValue(1.0)
                                .build())
                        .build()
        );
    }


    @Test
    public void checkInvalidFormalizedButValidWithoutFormalized() {
        int ticketCount = 10;
        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("testing", "");
        CategoryDataHelperMock categoryDataHelperMock = new CategoryDataHelperMock();
        DataBucketTicketAutoValidation special = new SpecialValidation(gcSkuValidationDao,
                gcSkuTicketDao, categoryDataHelperMock);


        Set<DataBucketTicketAutoValidation> validations = Set.of(special);

        OneStepValidationTaskAction oneStepValidationTaskAction = new OneStepValidationTaskAction(gcSkuTicketDao,
                gcSkuValidationDao, Set.of(), validations);

        generateTickets(ticketCount, formalizedParametersOffer(), VALIDATION_STARTED);

        ProcessTaskResult<ProcessDataBucketData> validationResult =
                oneStepValidationTaskAction.apply(new ProcessDataBucketData(dataBucketId));

        Assert.assertNotNull(validationResult);
        Assert.assertNotNull(validationResult.getResult());
        Assert.assertEquals(validationResult.getResult().getDataBucketId(), dataBucketId);

        Long[] ticketIds = gcSkuTicketDao.getTicketsByDataBucket(dataBucketId).stream()
                .map(GcSkuTicket::getId).toArray(Long[]::new);

        List<GcSkuValidation> specialValidationList =
                gcSkuValidationDao.getGcSkuValidations(special.getValidationType(), ticketIds);

        Assert.assertEquals(ticketCount, specialValidationList.size());

        Assert.assertTrue(specialValidationList.stream().allMatch(GcSkuValidation::getIsOk));
        Assert.assertTrue(specialValidationList.stream().allMatch(
                gcSkuValidation -> gcSkuValidation.getFailData() == null));

        List<GcSkuValidation> formalizedParamsValidationList =
                gcSkuValidationDao.getGcSkuValidations(GcSkuValidationType.FORMALIZED_PARAMS, ticketIds);

        Assert.assertEquals(ticketCount, formalizedParamsValidationList.size());

        Assert.assertTrue(formalizedParamsValidationList.stream().allMatch(GcSkuValidation::getIsOk));
        Assert.assertTrue(formalizedParamsValidationList.stream().allMatch(
                gcSkuValidation -> gcSkuValidation.getFailData() != null));

        Assert.assertTrue(formalizedParamsValidationList.stream()
                        .flatMap(gcSkuValidation -> gcSkuValidation.getFailData().getParams().stream())
                .allMatch(
                paramInfo -> paramInfo.isFormalized() && FORMILIZED_ID == paramInfo.getParamId()));
    }


    public class SpecialValidation extends DataBucketTicketAutoValidation {

        CategoryDataHelper categoryDataHelper;

        public SpecialValidation(GcSkuValidationDao gcSkuValidationDao, GcSkuTicketDao gcSkuTicketDao,
                                 CategoryDataHelper categoryDataHelper) {
            super(gcSkuValidationDao, gcSkuTicketDao);
            this.categoryDataHelper = categoryDataHelper;
        }

        @Override
        public GcSkuValidationType getValidationType() {
            // тут не важно какой, главное не GcSkuValidationType.FORMALIZED_PARAMS
            return GcSkuValidationType.TITLE_UPPER_CASE;
        }

        @Override
        public ProcessTaskResult<List<TicketValidationResult>> validate(List<GcSkuTicket> tickets) {
            // Для случая со всеми параметрами вернем невалидный формализованный параметр
            List<TicketValidationResult> ticketValidationResultList = tickets.stream()
                    .map(gcSkuTicket -> {
                        List<MarketParameterValueWrapper> params =
                                OfferParametersActualizer.getParamsFromOfferRaw(gcSkuTicket.getDatacampOffer(),
                                        categoryDataHelper.getCategoryData(gcSkuTicket.getCategoryId()),
                                        GcSkuTicketType.CSKU).stream().filter(marketParameterValueWrapper ->
                                          MarketParameterValueWrapper.MarketParameterValueWrapperType.FORMALIZED.equals(
                                        marketParameterValueWrapper.getMarketParameterValueWrapperType()))
                                        .collect(Collectors.toList());
                        if (params.isEmpty()) {
                            throw new IllegalStateException("Для валидации нужны формализованные параметры");
                        }
                        List<ParamInfo> paramInfoList = params.stream().map(param ->
                                new ParamInfo(param.getParamId(), param.getParamName(), true))
                                .collect(Collectors.toList());
                        FailData failData = new FailData(paramInfoList);
                        return TicketValidationResult.invalid(gcSkuTicket.getId(), List.of(), failData);
                    }).collect(Collectors.toList());
            return ProcessTaskResult.success(ticketValidationResultList);
        }

        @Override
        public ProcessTaskResult<List<TicketValidationResult>> validate(List<GcSkuTicket> tickets, Map<Long,
                Set<Long>> invalidParametersForTicketIds) {
            // Здесь вернем, что валидный
            List<TicketValidationResult> ticketValidationResultList = tickets.stream()
                    .map(gcSkuTicket -> TicketValidationResult.valid(gcSkuTicket.getId()))
                    .collect(Collectors.toList());
            return ProcessTaskResult.success(ticketValidationResultList);
        }
    }
}
