package ru.yandex.market.partner.auction.label;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.core.auction.err.AuctionValidationException;
import ru.yandex.market.partner.auction.AuctionBulkOfferBidsServantlet;
import ru.yandex.market.partner.auction.BidModificationLabelManager;
import ru.yandex.market.partner.auction.BulkUpdateRequest;
import ru.yandex.market.partner.auction.Labels;
import ru.yandex.market.partner.auction.request.AuctionBulkRequest;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.core.matchers.MapHasSize.mapHasSize;

/**
 * @author vbudnev
 */
public class LabelCommon {
    /**
     * Вспомогательный метод, задача которого - проверка результатов работы метода
     * <br>{@link BidModificationLabelManager#labelBidModificationByType}
     * <br>на основе ожидаемых значений счетчиков.
     */
    public static void testLabelModificationMethod(
            BulkUpdateRequest updateRequest,
            ExpectedLabelBidsStats expected,
            AuctionBulkRequest auctionRequest,
            AuctionBulkOfferBidsServantlet<PartnerDefaultRequestHandler.PartnerHttpServRequest> servantlet,
            BidModificationLabelManager bidModificationLabelManager
    ) throws AuctionValidationException {
        Labels labels = new Labels();
        final Map<String, String> warnings = new HashMap<>();

        List<BulkUpdateRequest> titleUpdateRequestsWithGroup = ImmutableList.of(
                updateRequest
        );

        labels.clear();
        warnings.clear();

        bidModificationLabelManager.labelBidModificationByType(
                auctionRequest,
                servantlet.validateAndMapToOfferId(
                        titleUpdateRequestsWithGroup,
                        auctionRequest,
                        warnings
                ),
                labels,
                warnings
        );

        if (expected.warningCount != null) {
            assertThat(
                    String.format("warnings size mismatch. updateReq=%s . Warnings: %s", updateRequest, warnings),
                    warnings,
                    mapHasSize(expected.warningCount)
            );
        }

        if (expected.updatedCount != null) {
            assertThat(
                    String.format("bidsToBeUpdate size mismatch. updateReq=%s", updateRequest),
                    labels.getBidsToBeValueUpdated(),
                    hasSize(expected.updatedCount)
            );
        }

        if (expected.recommendedCount != null) {
            assertThat(
                    String.format("bidsToBeRecommended size mismatch. updateReq=%s", updateRequest),
                    labels.getBidsToBeRecommended(),
                    hasSize(expected.recommendedCount)
            );
        }

        if (expected.groupOnlyChange != null) {
            assertThat(
                    String.format("onlyGroupChangeBids size mismatch. updateReq=%s", updateRequest),
                    labels.getOnlyGroupChangeBids(),
                    hasSize(expected.groupOnlyChange)
            );
        }

    }

    /**
     * Вспомогательный метод.
     * <br>Готовим пары запрос-лжидаемый_результат для удобства использования в {@link #testLabelModificationMethod )}
     */
    public static List<Pair<BulkUpdateRequest, ExpectedLabelBidsStats>> withExpectedStatus(List<BulkUpdateRequest> reqs, ExpectedLabelBidsStats status) {
        return reqs.stream()
                .map(x -> Pair.of(x, status))
                .collect(Collectors.toList());
    }
}
