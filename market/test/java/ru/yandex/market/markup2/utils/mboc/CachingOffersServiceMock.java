package ru.yandex.market.markup2.utils.mboc;

import ru.yandex.market.mboc.http.SupplierOffer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.market.markup2.utils.mboc.MboCategoryServiceMock.SUPPLIER_NAME_FUNCTION;

/**
 * @author york
 * @since 20.05.2020
 */
public class CachingOffersServiceMock extends CachingOffersService {

    public CachingOffersServiceMock(MboCategoryService mboCategoryService) {
        super(mboCategoryService, Collections.emptyMap());
    }

    @Override
    public List<SupplierOffer.Offer> getOffersByStatus(String status) {
        return mboCategoryService.getOfferPriorities().stream()
            .map(
                op -> SupplierOffer.Offer.newBuilder()
                    .setProcessingTicket(op.getTrackerTicket())
                    .setSupplierName(SUPPLIER_NAME_FUNCTION.apply(op.getSupplierId()))
                    .setSupplierId(op.getSupplierId())
                    .setMarketCategoryId(op.getCategoryId())
                    .setInternalOfferId(op.getOfferId())
                    .setProcessingTicketId(op.getProcessingTicketId())
                    .build())
            .collect(Collectors.toList());
    }
}
