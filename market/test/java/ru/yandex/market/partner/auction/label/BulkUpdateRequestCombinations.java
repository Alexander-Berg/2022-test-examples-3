package ru.yandex.market.partner.auction.label;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.partner.auction.BidReq;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.HybridGoal;

import static ru.yandex.market.partner.auction.AuctionBulkCommon.BIDREQ_111;
import static ru.yandex.market.partner.auction.BulkUpdateRequest.Builder.builder;

/**
 * @author vbudnev
 */
public class BulkUpdateRequestCombinations {


    /**
     * Вспомогательный метод
     * Создает список {@link BulkUpdateRequest} на основе заданного паттерна комбинаций и полей.
     * Использует однин и тот же набор offerName,value,link,goal для каждого созданного объекта -отличе лишь в том какие компоненты ставки установлены(bid/cbid/fee)
     * <p>
     * Пример:
     * "c-f-bf" -> создает список из {@link BulkUpdateRequest} содержащих в запросе : только bid-компоненту, только fee компоненту, fee+cbid компоненту
     * <p>
     * В случае неизвестного сочетания - кидает IllegalArgumentException
     */
    public static List<BulkUpdateRequest> combinations(String pattern, String offerName, BidReq value, AuctionBidComponentsLink link, HybridGoal goal) {

        List<BulkUpdateRequest> res = new ArrayList<>();
        BulkUpdateRequest.Builder builder = builder().withOfferName(offerName).withGoal(goal);

        for (int i = 0; i < pattern.length(); ++i) {
            char component = pattern.charAt(i);
            switch (component) {
                case 'c':
                    builder.withCbid(value);
                    break;
                case 'f':
                    builder.withFee(value);
                    break;
                case 'b':
                    builder.withBid(value);
                    break;
                case '-':
                    //do nothing on known separator
                    break;
                default:
                    throw new IllegalArgumentException("Cant generate permutation for :" + component);
            }
            res.add(builder.build());
        }
        return res;
    }

    public static List<BulkUpdateRequest> combinations(String pattern, String offerName, BidReq value, AuctionBidComponentsLink link) {
        return combinations(pattern, offerName, value, link, null);
    }

    public static List<BulkUpdateRequest> combinations(String pattern, String offerName, BidReq value) {
        return combinations(pattern, offerName, value, null);
    }

    /**
     * Комбинации в случае если значние оффера и ставки неважны
     */
    public static List<BulkUpdateRequest> combinationsA(String pattern, String offerName, BidReq value, AuctionBidComponentsLink link, HybridGoal goal) {
        //если значение неважно
        if (value == null) {
            value = BIDREQ_111;
        }

        if (offerName == null) {
            offerName = "offer_irrelevant_name";
        }

        return combinations(pattern, offerName, value, link, goal);
    }

    public static List<BulkUpdateRequest> combinationsA(String pattern, String offerName, AuctionBidComponentsLink link) {
        return combinationsA(pattern, offerName, null, link, null);
    }

    public static List<BulkUpdateRequest> combinationsA(String pattern, String offerName) {
        return combinationsA(pattern, offerName, null, null, null);
    }

    public static List<BulkUpdateRequest> combinationsA(String pattern, AuctionBidComponentsLink link) {
        return combinationsA(pattern, null, null, link, null);
    }

    public static List<BulkUpdateRequest> combinationsA(String pattern) {
        return combinationsA(pattern, null, null, null, null);
    }

}
