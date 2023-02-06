package ru.yandex.market.markup2.utils.mboc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author s-ermakov
 */
public class MboCategoryServiceMock implements MboCategoryService {
    public static final Function<Long, String> SUPPLIER_NAME_FUNCTION = (id) -> "Supplier" + id;
    private Map<Long, MbocOfferStatus> lastData = new HashMap<>();
    private List<List<MbocOfferStatus>> history = new ArrayList<>();
    private Map<String, Map<Long, SupplierOffer.Offer>> offerMap = new HashMap<>();
    private Map<Integer, MboCategory.GetShortSupplierInfosResponse.ShortSupplierInfo> supplierShortInfos = new HashMap<>();
    private Multimap<MboCategory.GetOffersPrioritiesRequest, String> ticketsForRequest = HashMultimap.create();
    private Multimap<String, OfferPriorityInfo> offerPrioritiesByTicket = HashMultimap.create();
    private List<TicketPriorityInfo> ticketPriorityInfos = new ArrayList<>();
    private Map<Integer, TicketPriorityInfo> ticketPriorityInfoMap = new HashMap<>();

    private List<SupplierOffer.ContentTaskResult> mappingResults = new ArrayList<>();
    private List<SupplierOffer.MappingModerationTaskResult> moderationResults =
            new ArrayList<>();
    
    private boolean mockThrowingException;
    
    @Override
    public void updateStatuses(Collection<MbocOfferStatus> offerUpdates) {
        if (offerUpdates.isEmpty()) {
            return;
        }

        for (MbocOfferStatus offerUpdate : offerUpdates) {
            lastData.put(offerUpdate.getOfferId(), offerUpdate);
        }
        history.add(new ArrayList<>(offerUpdates));
    }

    public List<TicketPriorityInfo> getTicketPriorities() {
        return getTicketPriorities(MboCategory.GetTicketPrioritiesRequest.getDefaultInstance());
    }

    public MbocOfferStatus getLastState(long offerId) {
        return lastData.get(offerId);
    }

    public List<List<MbocOfferStatus>> getAndClearHistory() {
        List<List<MbocOfferStatus>> result = new ArrayList<>(history);
        history.clear();
        return result;
    }

    public List<OfferPriorityInfo> getOfferPriorities() {
        return new ArrayList<>(offerPrioritiesByTicket.values());
    }

    @Override
    public List<TicketPriorityInfo> getTicketPriorities(MboCategory.GetTicketPrioritiesRequest request) {
        return ticketPriorityInfos;
    }

    @Override
    public MboCategory.SaveMappingModerationResponse saveMappingsModeration(
            MboCategory.SaveMappingsModerationRequest request) {
        if (mockThrowingException) {
            mockThrowingException = false;
            throw new RuntimeException("error");
        }

        moderationResults.addAll(request.getResultsList());
        return MboCategory.SaveMappingModerationResponse.newBuilder()
                .setResult(SupplierOffer.OperationResult.newBuilder()
                        .setStatus(SupplierOffer.OperationStatus.SUCCESS)
                        .build())
                .build();
    }

    @Override
    public MboCategory.SaveTaskMappingsResponse saveTaskMappings(
            MboCategory.SaveTaskMappingsRequest request) {
        if (mockThrowingException) {
            mockThrowingException = false;
            throw new RuntimeException("error");
        }

        mappingResults.addAll(request.getMappingList());
        return MboCategory.SaveTaskMappingsResponse.newBuilder()
                .setResult(SupplierOffer.OperationResult.newBuilder()
                        .setStatus(SupplierOffer.OperationStatus.SUCCESS)
                        .build())
                .build();
    }

    @Override
    public MboCategory.UpdateSupplierOfferCategoryResponse updateSupplierOfferCategory(
            MboCategory.UpdateSupplierOfferCategoryRequest request) {
        return MboCategory.UpdateSupplierOfferCategoryResponse.newBuilder()
                .setResult(SupplierOffer.OperationResult.newBuilder()
                        .setStatus(SupplierOffer.OperationStatus.SUCCESS)
                        .build()).build();
    }

    @Override
    public void loadOffersByStatusStoppable(String status,
                                            Optional<Integer> limit,
                                            boolean hasDeadlineOrOldProcessingStatus,
                                            Function<SupplierOffer.Offer, Boolean> consumer) {
        int lim = limit.orElse(Integer.MAX_VALUE);
        for (SupplierOffer.Offer offer : offerMap.getOrDefault(status, new HashMap<>()).values()) {
            if (!consumer.apply(offer) || --lim <= 0) {
                break;
            }
        }
    }

    @Override
    public MboCategory.GetShortSupplierInfosResponse getShortSupplierInfos(
            MboCategory.GetShortSupplierInfosRequest request) {
        MboCategory.GetShortSupplierInfosResponse.Builder response = MboCategory.GetShortSupplierInfosResponse.newBuilder();
        for (int id : request.getSupplierIdList()) {
            if (supplierShortInfos.containsKey(id)) {
                response.addShortSupplierInfo(supplierShortInfos.get(id));
            }
        }
        return response.build();
    }

    public void addSupplier(int id, String name, SupplierOffer.SupplierType supplierType) {
        supplierShortInfos.put(id, MboCategory.GetShortSupplierInfosResponse.ShortSupplierInfo.newBuilder()
                .setSupplierId(id)
                .setSupplierName(name)
                .setSupplierType(supplierType)
                .build());
    }

    public void clearOffers() {
        offerMap.clear();
        offerPrioritiesByTicket.clear();
        ticketPriorityInfoMap.clear();
        ticketPriorityInfos.clear();
    }

    public void putOfferPriorities(List<OfferPriorityInfo> priorityInfos) {
        putOfferPriorities(priorityInfos, null);
    }

    public void putOfferPriorities(List<OfferPriorityInfo> priorityInfos,
                                   MboCategory.GetOffersPrioritiesRequest matchingRequest) {
        priorityInfos.forEach(info -> {
            offerPrioritiesByTicket.put(info.getTrackerTicket(), info);
            if (matchingRequest != null) {
                ticketsForRequest.put(matchingRequest, info.getTrackerTicket());
            }

            TicketPriorityInfo ticketInfo =  ticketPriorityInfoMap.get(info.getProcessingTicketId());
            if (ticketInfo == null) {
                ticketInfo = buildTicket(info);
                ticketPriorityInfos.add(ticketInfo);
                ticketPriorityInfoMap.put(info.getProcessingTicketId(), ticketInfo);
            } else {
                ticketInfo.addOfferInfo(info.getCategoryId(), info.getOfferId());
            }
        });
    }

    private TicketPriorityInfo buildTicket(OfferPriorityInfo p) {
        TicketPriorityInfo priorityInfo = new TicketPriorityInfo();
        priorityInfo.setId(p.getProcessingTicketId());
        priorityInfo.setTrackerTicket(p.getTrackerTicket());
        priorityInfo.setTicketDeadline(p.getTicketDeadline());
        priorityInfo.setSupplierId(p.getSupplierId());
        priorityInfo.addOfferInfo(p.getCategoryId(), p.getOfferId());
        return priorityInfo;
    }

    public void removeOffers(String trackerTicket) {
        offerPrioritiesByTicket.removeAll(trackerTicket);
        ticketPriorityInfos.removeIf(t -> t.getTrackerTicket().equals(trackerTicket));
    }


    public List<SupplierOffer.ContentTaskResult> getMappingResults() {
        return mappingResults;
    }

    public List<SupplierOffer.MappingModerationTaskResult> getModerationResults() {
        return moderationResults;
    }

    public void mockThrowingException() {
        mockThrowingException = true;
    }

}
