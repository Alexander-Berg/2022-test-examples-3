package ru.yandex.market.mboc.app.proto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MdmCommon;
import ru.yandex.market.mdm.http.MdmDocument;
import ru.yandex.market.mdm.http.MdmDocument.AddDocumentsResponse.DocumentResponse;
import ru.yandex.market.mdm.http.MdmDocument.AddDocumentsResponse.Status;
import ru.yandex.market.mdm.http.SupplierDocumentService;

/**
 * @author moskovkin@yandex-team.ru
 * @since 01.07.19
 */
public class SupplierDocumentServiceMock implements SupplierDocumentService {
    private final MasterDataServiceMock masterDataServiceMock;

    private Map<String, MdmDocument.Document> mdmDocuments = new HashMap<>();
    private Map<MdmCommon.ShopSkuKey, List<MdmDocument.DocumentOfferRelation>> mdmDocumentRelations = new HashMap<>();

    public SupplierDocumentServiceMock(MasterDataServiceMock masterDataServiceMock) {
        this.masterDataServiceMock = masterDataServiceMock;
    }

    public Map<String, MdmDocument.Document> getMdmDocuments() {
        return Collections.unmodifiableMap(mdmDocuments);
    }

    public Map<MdmCommon.ShopSkuKey, List<MdmDocument.DocumentOfferRelation>> getMdmDocumentRelations() {
        return Collections.unmodifiableMap(mdmDocumentRelations);
    }

    public List<MdmDocument.DocumentOfferRelation> getConvertedToInternalRelations(SupplierConverterService converter) {
        return mdmDocumentRelations.values().stream()
            .flatMap(Collection::stream)
            .map(rel -> {
                ShopSkuKey externalKey = new ShopSkuKey(rel.getSupplierId(), rel.getShopSku());
                ShopSkuKey internalKey = converter.convertRealToInternal(externalKey);
                return rel.toBuilder().setSupplierId(internalKey.getSupplierId()).setShopSku(internalKey.getShopSku())
                    .build();
            }).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public MdmDocument.FindDocumentsResponse findSupplierDocuments(
        MdmDocument.FindSupplierDocumentsRequest findSupplierDocumentsRequest
    ) {
        MdmCommon.ShopSkuKey key = MdmCommon.ShopSkuKey.newBuilder()
            .setSupplierId(findSupplierDocumentsRequest.getSupplierId())
            .setShopSku(findSupplierDocumentsRequest.getSearchQuery())
            .build();

        MdmDocument.FindDocumentsResponse response = MdmDocument.FindDocumentsResponse.newBuilder()
            .addAllDocument(getDocumentsByKey(key))
            .build();

        return response;
    }

    private List<MdmDocument.Document> getDocumentsByKey(MdmCommon.ShopSkuKey key) {
        List<MdmDocument.DocumentOfferRelation> relations = mdmDocumentRelations.getOrDefault(key, new ArrayList<>());
        return relations.stream().map(r -> mdmDocuments.get(r.getRegistrationNumber())).collect(Collectors.toList());
    }

    @Override
    public MdmDocument.FindDocumentsByShopSkuResponse findSupplierDocumentsByShopSku(
        MdmDocument.FindDocumentsByShopSkuRequest request
    ) {
        MdmDocument.FindDocumentsByShopSkuResponse.Builder response = MdmDocument.FindDocumentsByShopSkuResponse
            .newBuilder();

        for (MdmCommon.ShopSkuKey shopSkuKey : new HashSet<>(request.getShopSkuKeysList())) {
            MdmDocument.ShopSkuDocuments documents = MdmDocument.ShopSkuDocuments.newBuilder()
                .setShopSkuKey(shopSkuKey)
                .addAllDocument(getDocumentsByKey(shopSkuKey))
                .build();
            response.addShopSkuDocuments(documents);
        }
        return response.build();
    }

    @Override
    public MdmDocument.AddDocumentsResponse addSupplierDocuments(
        MdmDocument.AddSupplierDocumentsRequest request
    ) {
        for (MdmDocument.AddSupplierDocumentsRequest.DocumentAddition documentAddition : request.getDocumentList()) {
            mdmDocuments.put(documentAddition.getDocument().getRegistrationNumber(), documentAddition.getDocument());
        }
        var response = MdmDocument.AddDocumentsResponse.newBuilder()
            .setStatus(MdmDocument.AddDocumentsResponse.Status.OK);
        request.getDocumentList().forEach(doc -> {
            response.addDocumentResponse(DocumentResponse.newBuilder()
                .setStatus(Status.OK)
                .setDocument(doc.getDocument())
                .build());
        });
        return response.build();
    }

    @Override
    public MdmDocument.AddDocumentRelationsResponse addDocumentRelations(
        MdmDocument.AddDocumentRelationsRequest request
    ) {
        MdmDocument.AddDocumentRelationsResponse.Builder response =
            MdmDocument.AddDocumentRelationsResponse.newBuilder();

        for (MdmDocument.DocumentOfferRelation relation : request.getDocumentOfferRelationList()) {
            MdmCommon.ShopSkuKey key = MdmCommon.ShopSkuKey.newBuilder()
                .setSupplierId(relation.getSupplierId())
                .setShopSku(relation.getShopSku())
                .build();

            if (!masterDataServiceMock.getSskuMasterData().containsKey(key)) {
                response.addDocumentRelations(
                    MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition.newBuilder()
                        .setError(MbocCommon.Message.newBuilder()
                            .setMessageCode("mboc.error.md-add-document-relations-failed-to-add-document-relation")
                            .build())
                        .build()
                );
                continue;
            }

            if (!mdmDocuments.containsKey(relation.getRegistrationNumber())) {
                response.addDocumentRelations(
                    MdmDocument.AddDocumentRelationsResponse.DocumentRelationAddition.newBuilder()
                        .setError(MbocCommon.Message.newBuilder()
                            .setMessageCode("mboc.error.md-add-document-relations-failed-to-add-document-relation")
                            .build())
                        .build()
                );
                continue;
            }

            mdmDocumentRelations.computeIfAbsent(key, k -> new ArrayList<>()).add(relation);
        }

        response.setStatus(response.getDocumentRelationsList().isEmpty()
            ? MdmDocument.AddDocumentRelationsResponse.Status.OK
            : MdmDocument.AddDocumentRelationsResponse.Status.ERROR
        );

        return response.build();
    }

    @Override
    public MdmDocument.FindSupplierDocumentRelationsResponse findSupplierDocumentRelations(
        MdmDocument.FindSupplierDocumentRelationsRequest findSupplierDocumentRelationsRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MdmDocument.RemoveDocumentOfferRelationsResponse removeDocumentOfferRelations(
        MdmDocument.RemoveDocumentOfferRelationsRequest removeDocumentOfferRelationsRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MdmDocument.FindDocumentsResponse findSupplierDocumentByRegistrationNumber(
        MdmDocument.FindDocumentByRegistrationNumberRequest findDocumentByRegistrationNumberRequest
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MonitoringResult ping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MonitoringResult monitoring() {
        throw new UnsupportedOperationException();
    }
}
