package ru.yandex.market.gutgin.tms.pipeline.csku;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.UpdateRatingTaskAction;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcExternalServiceRequestDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcExternalServiceRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.service.DataCampOfferBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.SAVE_AND_PUBLISH_OK;

public class UpdateRatingTaskActionTest extends DBDcpStateGenerator {

    @Autowired
    GcSkuTicketDao gcSkuTicketDao;

    @Autowired
    GcExternalServiceRequestDao gcExternalServiceRequestDao;

    ModelStorageHelper modelStorageHelper;
    SkuRatingEvaluator skuRatingEvaluator;


    UpdateRatingTaskAction updateRatingTaskAction;

    @Override
    public void setUp() {
        super.setUp();
        modelStorageHelper = Mockito.mock(ModelStorageHelper.class);
        skuRatingEvaluator = Mockito.mock(SkuRatingEvaluator.class);
        updateRatingTaskAction = new UpdateRatingTaskAction(gcSkuTicketDao, modelStorageHelper,
                skuRatingEvaluator, gcExternalServiceRequestDao);
    }

    @Test
    public void happyPath() throws InvalidProtocolBufferException {
        Integer expectedRating = 25;
        List<GcSkuTicket> tickets = generateTickets(1, onlyNameSettings(), SAVE_AND_PUBLISH_OK);
        tickets.forEach(ticket -> ticket.setResultMboPskuId(ticket.getId()));
        gcSkuTicketDao.update(tickets);
        long dataBucketId = tickets.stream().findFirst().orElseThrow().getDataBucketId();

        Set<Long> ticketIds = tickets
                .stream().map(GcSkuTicket::getResultMboPskuId).collect(Collectors.toSet());

        Mockito.when(modelStorageHelper.getSavedSkuMap(ticketIds))
                .thenReturn(getSavedSkuMap(ticketIds, Optional.empty(), true));

        Mockito.when(skuRatingEvaluator.evaluate(Mockito.any()))
                .thenReturn(expectedRating);

        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
                .thenAnswer(invocationOnMock ->
                        new ModelStorageHelper.SaveGroupResponse(
                                invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class),
                                ModelCardApi.SaveModelsGroupResponse.newBuilder()
                                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                                .setStatus(ModelStorage.OperationStatusType.OK))
                                        .build()));

        updateRatingTaskAction.doRun(new ProcessDataBucketData(dataBucketId));

        List<GcSkuTicket> afterUpdate = gcSkuTicketDao.findAll();

        Assert.assertEquals(afterUpdate.stream().map(GcSkuTicket::getStatus).collect(Collectors.toSet()),
                Set.of(GcSkuTicketStatus.RATING_CALCULATION)
        );

        List<GcExternalServiceRequest> requests = gcExternalServiceRequestDao.findAll();
        Assert.assertEquals(1, requests.size());
        GcExternalServiceRequest request = requests.get(0);
        ModelCardApi.SaveModelsGroupRequest mboRequest = ModelCardApi.SaveModelsGroupRequest
                .parseFrom(request.getRequest().toByteArray());
        Assert.assertEquals((long) afterUpdate.get(0).getResultMboPskuId(),
                mboRequest.getModelsRequest(0).getModels(0).getId());

        Assert.assertTrue(mboRequest.getModelsRequest(0).getModels(0)
                .getParameterValuesList().stream().allMatch(parameterValue ->
                        (parameterValue.getParamId() == KnownParameters.CURRENT_RATING.getId() &&
                                parameterValue.getXslName().equals(KnownParameters.CURRENT_RATING.getXslName()) &&
                                parameterValue.getNumericValue().equals(String.valueOf(expectedRating)))
                                || (parameterValue.getParamId() == 1L)
                ));
    }

    @Test
    public void checkThatRatingIsUpdating() throws InvalidProtocolBufferException {
        Integer expectedRating = 25;
        List<GcSkuTicket> tickets = generateTickets(1, onlyNameSettings(), SAVE_AND_PUBLISH_OK);
        tickets.forEach(ticket -> ticket.setResultMboPskuId(ticket.getId()));
        gcSkuTicketDao.update(tickets);
        long dataBucketId = tickets.stream().findFirst().orElseThrow().getDataBucketId();

        Set<Long> ticketIds = tickets
                .stream().map(GcSkuTicket::getResultMboPskuId).collect(Collectors.toSet());

        Mockito.when(modelStorageHelper.getSavedSkuMap(ticketIds))
                .thenReturn(getSavedSkuMap(ticketIds, Optional.of(10L), true));

        Mockito.when(skuRatingEvaluator.evaluate(Mockito.any()))
                .thenReturn(expectedRating);

        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
                .thenAnswer(invocationOnMock ->
                        new ModelStorageHelper.SaveGroupResponse(
                                invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class),
                                ModelCardApi.SaveModelsGroupResponse.newBuilder()
                                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                                .setStatus(ModelStorage.OperationStatusType.OK))
                                        .build()));

        updateRatingTaskAction.doRun(new ProcessDataBucketData(dataBucketId));

        List<GcSkuTicket> afterUpdate = gcSkuTicketDao.findAll();

        Assert.assertEquals(afterUpdate.stream().map(GcSkuTicket::getStatus).collect(Collectors.toSet()),
                Set.of(GcSkuTicketStatus.RATING_CALCULATION)
        );

        List<GcExternalServiceRequest> requests = gcExternalServiceRequestDao.findAll();
        Assert.assertEquals(1, requests.size());
        GcExternalServiceRequest request = requests.get(0);
        ModelCardApi.SaveModelsGroupRequest mboRequest = ModelCardApi.SaveModelsGroupRequest
                .parseFrom(request.getRequest().toByteArray());
        Assert.assertEquals((long) afterUpdate.get(0).getResultMboPskuId(),
                mboRequest.getModelsRequest(0).getModels(0).getId());

        Assert.assertTrue(mboRequest.getModelsRequest(0).getModels(0)
                .getParameterValuesList().stream().allMatch(parameterValue ->
                        (parameterValue.getParamId() == KnownParameters.CURRENT_RATING.getId() &&
                                parameterValue.getXslName().equals(KnownParameters.CURRENT_RATING.getXslName()) &&
                                parameterValue.getNumericValue().equals(String.valueOf(expectedRating)))
                                || (parameterValue.getParamId() == 1L)
                ));
    }

    @Test
    public void checkThatExceptionThrowsWhenNoParentModel() throws InvalidProtocolBufferException {
        List<GcSkuTicket> tickets = generateTickets(1, onlyNameSettings(), SAVE_AND_PUBLISH_OK);
        tickets.forEach(ticket -> ticket.setResultMboPskuId(ticket.getId()));
        gcSkuTicketDao.update(tickets);
        long dataBucketId = tickets.stream().findFirst().orElseThrow().getDataBucketId();

        Set<Long> ticketIds = tickets
                .stream().map(GcSkuTicket::getResultMboPskuId).collect(Collectors.toSet());

        Mockito.when(modelStorageHelper.getSavedSkuMap(ticketIds))
                .thenReturn(getSavedSkuMap(ticketIds, Optional.of(10L), false));


        Assert.assertThrows(IllegalStateException.class, () ->
                updateRatingTaskAction.doRun(new ProcessDataBucketData(dataBucketId)));
    }

    @Test
    public void taskActionFailWhenCardApiResponseNotOk() throws InvalidProtocolBufferException {
        Integer expectedRating = 25;
        List<GcSkuTicket> tickets = generateTickets(1, onlyNameSettings(), SAVE_AND_PUBLISH_OK);
        tickets.forEach(ticket -> ticket.setResultMboPskuId(ticket.getId()));
        gcSkuTicketDao.update(tickets);
        long dataBucketId = tickets.stream().findFirst().orElseThrow().getDataBucketId();

        Set<Long> ticketIds = tickets
                .stream().map(GcSkuTicket::getResultMboPskuId).collect(Collectors.toSet());

        Mockito.when(modelStorageHelper.getSavedSkuMap(ticketIds))
                .thenReturn(getSavedSkuMap(ticketIds, Optional.empty(), true));

        Mockito.when(skuRatingEvaluator.evaluate(Mockito.any()))
                .thenReturn(expectedRating);

        when(modelStorageHelper.executeSaveModelRequest(any(ModelCardApi.SaveModelsGroupRequest.class)))
                .thenAnswer(invocationOnMock ->
                        new ModelStorageHelper.SaveGroupResponse(
                                invocationOnMock.getArgument(0, ModelCardApi.SaveModelsGroupRequest.class),
                                ModelCardApi.SaveModelsGroupResponse.newBuilder()
                                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR))
                                        .build()));

        ProcessTaskResult<ProcessDataBucketData> taskResult =
                updateRatingTaskAction.doRun(new ProcessDataBucketData(dataBucketId));

        Assert.assertTrue(taskResult.hasProblems());


        List<GcSkuTicket> afterUpdate = gcSkuTicketDao.findAll();

        Assert.assertEquals(afterUpdate.stream().map(GcSkuTicket::getStatus).collect(Collectors.toSet()),
                Set.of(GcSkuTicketStatus.SAVE_AND_PUBLISH_OK)
        );

        List<GcExternalServiceRequest> requests = gcExternalServiceRequestDao.findAll();
        Assert.assertEquals(1, requests.size());
        GcExternalServiceRequest request = requests.get(0);
        ModelCardApi.SaveModelsGroupRequest mboRequest = ModelCardApi.SaveModelsGroupRequest
                .parseFrom(request.getRequest().toByteArray());
        Assert.assertEquals((long) afterUpdate.get(0).getResultMboPskuId(),
                mboRequest.getModelsRequest(0).getModels(0).getId());

        Assert.assertTrue(mboRequest.getModelsRequest(0).getModels(0)
                .getParameterValuesList().stream().allMatch(parameterValue ->
                        (parameterValue.getParamId() == KnownParameters.CURRENT_RATING.getId() &&
                                parameterValue.getXslName().equals(KnownParameters.CURRENT_RATING.getXslName()) &&
                                parameterValue.getNumericValue().equals(String.valueOf(expectedRating)))
                                || (parameterValue.getParamId() == 1L)
                ));
    }


    private Map<Long, ModelStorage.Model> getSavedSkuMap(Set<Long> skuIds, Optional<Long> rating,
                                                         boolean addParentModel) {
        Map<Long, ModelStorage.Model> result = new HashMap<>();

        skuIds.forEach(skuId -> {
            long modelId = skuId * 100L;
            result.put(skuId, generateSku(skuId, modelId, rating));
            if (addParentModel) {
                result.put(modelId, generateModel(modelId));
            }
        });

        return result;
    }

    private ModelStorage.Model generateSku(Long skuId, long modelId, Optional<Long> rating) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder
                .setId(skuId)
                .addRelations(ModelStorage.Relation.newBuilder()
                        .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                        .setId(modelId)
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(1L)
                        .build());
        rating.ifPresent(aLong -> builder.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownParameters.CURRENT_RATING.getId())
                .setNumericValue(String.valueOf(aLong))
                .build()));
        return builder.build();
    }

    private ModelStorage.Model generateModel(long modelId) {
        return ModelStorage.Model.newBuilder()
                .setId(modelId)
                .build();
    }


    private Consumer<DatacampOffer> onlyNameSettings() {
        return offer -> {
            offer.setData(
                    new DataCampOfferBuilder(
                            offer.getCreateTime(),
                            offer.getBusinessId(),
                            CATEGORY_ID,
                            offer.getOfferId()
                    ).withActualNameAndTitle("valid title").build()
            );
        };
    }
}
