package ru.yandex.market.mdm.app.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampService;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessStage;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.BusinessSwitchInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.BusinessSwitchTransport;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmBusinessStageSwitcher;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.service.HelloGrpcTestingService;
import ru.yandex.market.mboc.app.security.SecuredRolesIgnore;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.document.DocumentFilter;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.MdmEntity;
import ru.yandex.market.mdm.http.MdmExternalKey;
import ru.yandex.market.mdm.http.MdmExternalKeys;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByExternalKeysRequest;
import ru.yandex.market.mdm.http.entity.GetMdmEntityByMdmIdsRequest;
import ru.yandex.market.mdm.http.entity.MdmEntityStorageServiceGrpc;
import ru.yandex.market.mdm.http.entity.SaveMdmEntityRequest;
import ru.yandex.market.mdm.http.search.MdmEntityIds;
import ru.yandex.market.springmvctots.annotations.TsIgnore;

@TsIgnore
@RestController
@RequestMapping("/int-test")
@SecuredRolesIgnore(reason = "Utility handle for integration test")
@ConditionalOnProperty(name = "mdm.tool", havingValue = "false", matchIfMissing = true)
@SuppressWarnings("checkstyle:MagicNumber")
public class TestController {
    private final QualityDocumentRepository documentRepository;
    private final MdmDatacampService mdmDatacampService;
    private final MdmBusinessStageSwitcher mdmBusinessStageSwitcher;
    private final MdmSupplierCachingService supplierCachingService;
    private final HelloGrpcTestingService helloGrpcTestingService;
    private final MdmEntityStorageServiceGrpc.MdmEntityStorageServiceBlockingStub mdmEntityStorageService;

    public TestController(QualityDocumentRepository documentRepository,
                          MdmDatacampService mdmDatacampService,
                          MdmBusinessStageSwitcher mdmBusinessStageSwitcher,
                          MdmSupplierCachingService supplierCachingService,
                          HelloGrpcTestingService helloGrpcTestingService,
                          MdmEntityStorageServiceGrpc.MdmEntityStorageServiceBlockingStub mdmEntityStorageService) {
        this.documentRepository = documentRepository;
        this.mdmDatacampService = mdmDatacampService;
        this.mdmBusinessStageSwitcher = mdmBusinessStageSwitcher;
        this.supplierCachingService = supplierCachingService;
        this.helloGrpcTestingService = helloGrpcTestingService;
        this.mdmEntityStorageService = mdmEntityStorageService;
    }

    @GetMapping(value = "/hard-remove-document")
    public String intTestRemoveDocument(@RequestParam(name = "regNumber") String regNumber) {
        List<DocumentOfferRelation> relations = documentRepository.findRelations(
            new DocumentFilter().setRegistrationNumberSearch(regNumber));
        if (!relations.isEmpty()) {
            documentRepository.deleteOfferRelations(relations);
        }
        List<QualityDocument> documents = documentRepository.findBy(
            new DocumentFilter().setRegistrationNumberSearch(regNumber));
        documents.forEach(documentRepository::delete);
        return "Done";
    }

    @GetMapping(value = "/send-datacamp-data")
    public String testSendDatacampData() {
        var offer = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(1)
                    .setOfferId("test-tovar")
                    .build())
                .build())
            .build();
        try {
            mdmDatacampService.sendOffersToDatacamp(List.of(offer));
        } catch (Exception e) {
            return e.getMessage();
        }
        return "Ok";
    }

    @GetMapping(value = "/switch-business")
    public String switchBusiness(@RequestParam(name = "newBusiness") int newBusiness,
                                 @RequestParam(name = "serviceIds") List<Integer> serviceIds) {
        if (newBusiness <= 0 || CollectionUtils.isEmpty(serviceIds)) {
            return "Invalid business id or service list. Aborted.";
        }

        List<BusinessSwitchInfo> switches = serviceIds.stream()
            .map(serviceId -> BusinessSwitchInfo.businessChange(serviceId, newBusiness, 0L,
                BusinessSwitchTransport.INTEGRATION_TEST))
            .collect(Collectors.toList());
        mdmBusinessStageSwitcher.applySupplierBusinessChanges(switches);
        return "Ok";
    }

    @GetMapping(value = "/switch-stage")
    public String switchStage(@RequestParam(name = "stageNum") int stageNum,
                              @RequestParam(name = "serviceIds") List<Integer> serviceIds) {
        if (stageNum < 2 || stageNum > 3 || CollectionUtils.isEmpty(serviceIds)) {
            return "Invalid stage or service list. Aborted.";
        }

        MdmBusinessStage stage = stageNum == 2 ? MdmBusinessStage.BUSINESS_DISABLED : MdmBusinessStage.BUSINESS_ENABLED;

        List<BusinessSwitchInfo> switches = serviceIds.stream()
            .map(serviceId -> BusinessSwitchInfo.stageSwitch(serviceId, stage, 0L,
                BusinessSwitchTransport.INTEGRATION_TEST))
            .collect(Collectors.toList());
        mdmBusinessStageSwitcher.applySupplierBusinessChanges(switches);
        return "Ok";
    }

    @GetMapping(value = "/get-stage")
    public String getStage(@RequestParam(name = "serviceIds") List<Integer> serviceIds) {
        if (CollectionUtils.isEmpty(serviceIds)) {
            return "Empty service list. Aborted.";
        }
        Map<MdmBusinessStage, String> stagesNames = Map.of(
            MdmBusinessStage.NO_DATA, "нет данных (проблема!)",
            MdmBusinessStage.NO_BUSINESS, "нет бизнеса (странно!)",
            MdmBusinessStage.BUSINESS_DISABLED, "стадия 2 (не подключён)",
            MdmBusinessStage.BUSINESS_ENABLED, "стадия 3 (подключён)"
        );
        StringBuilder result = new StringBuilder();
        for (int serviceId : serviceIds) {
            String stage = stagesNames.get(supplierCachingService.getBusinessEnableMode(serviceId));
            var group = supplierCachingService.getFlatRelationsFor(serviceId);
            result.append("{ ").append(serviceId).append(": ").append(stage);
            if (group.isPresent() && group.get().isBusinessGroup()) {
                long businessId = group.get().getBusinessId();
                result.append(", соотв. бизнес: ").append(businessId);
            }
            result.append(" }, ");
        }
        return result.toString();
    }

    @GetMapping(value = "/grpc-hello")
    public String grpcHello(@RequestParam(name = "mode") String mode) {
        if (Objects.equals(mode, "stream")) {
            return "STREAM: " + helloGrpcTestingService.getStreamingHelloResponse();
        } else {
            return "BLOCKING: " + helloGrpcTestingService.getBlockingHelloResponse();
        }
    }

    @GetMapping(value = "/mdm-entity-save")
    public String mdmEntitySave(
        @RequestParam(name = "mdmId") long mdmId,
        @RequestParam(name = "mskuId") long mskuId
    ) {
        if (mdmId <= 0 || mskuId <= 0) {
            return "MDM/MSKU ID should be positive";
        }

        MdmEntity.Builder entity = MdmEntity.newBuilder();
        entity.setMdmEntityTypeId(6); // gold msku
        entity.setMdmId(mdmId);
        entity.setMdmUpdateMeta(MdmBase.MdmUpdateMeta.getDefaultInstance());
        entity.putMdmAttributeValues(35428L, MdmAttributeValues.newBuilder() // MSKU ID indexed attribute
            .setMdmAttributeId(35428L)
            .addValues(MdmAttributeValue.newBuilder().setInt64(mskuId).build())
            .build());
        entity.putMdmAttributeValues(12345, MdmAttributeValues.newBuilder() // random pseudo attribute
            .setMdmAttributeId(12345)
            .addValues(MdmAttributeValue.newBuilder().setString(MdmBase.I18nStrings.newBuilder()
                .addI18NString(MdmBase.I18nString.newBuilder().setString(UUID.randomUUID().toString()).build())
                .buildPartial()).build())
            .build());
        entity.putMdmAttributeValues(54321, MdmAttributeValues.newBuilder() // random pseudo attribute
            .setMdmAttributeId(54321)
            .addValues(MdmAttributeValue.newBuilder().setNumeric("49.5").build())
            .build());
        var response = mdmEntityStorageService.save(SaveMdmEntityRequest.newBuilder()
            .addMdmEntities(entity)
            .build());
        return JsonFormat.printToString(response);
    }

    @GetMapping(value = "/mdm-entity-find-msku-id")
    public String mdmEntityFindByMskuId(@RequestParam(name = "mskuId") long mskuId) {
        var response = mdmEntityStorageService.getByExternalKeys(GetMdmEntityByExternalKeysRequest.newBuilder()
            .setMdmExternalKeys(MdmExternalKeys.newBuilder()
                .setMdmEntityTypeId(6) // gold msku
                .addMdmExternalKeys(MdmExternalKey.newBuilder()
                    .addMdmAttributeValues(MdmAttributeValues.newBuilder()
                        .setMdmAttributeId(35428L) // MSKU ID indexed attribute
                        .addValues(MdmAttributeValue.newBuilder().setInt64(mskuId).build())
                        .build())
                    .build())
                .build())
            .build());
        if (response.getMdmEntitiesCount() == 0) {
            return "Не найдено";
        }
        return JsonFormat.printToString(response.getMdmEntities(0));
    }

    @GetMapping(value = "/mdm-entity-find-mdm-id")
    public String mdmEntityFindByMdmId(@RequestParam(name = "mdmId") long mdmId) {
        var response = mdmEntityStorageService.getByMdmIds(GetMdmEntityByMdmIdsRequest.newBuilder()
            .setMdmIds(MdmEntityIds.newBuilder()
                .setMdmEntityTypeId(6) // gold msku
                .addMdmIds(mdmId)
                .build())
            .build());
        if (response.getMdmEntitiesCount() == 0) {
            return "Не найдено";
        }
        return JsonFormat.printToString(response.getMdmEntities(0));
    }
}
