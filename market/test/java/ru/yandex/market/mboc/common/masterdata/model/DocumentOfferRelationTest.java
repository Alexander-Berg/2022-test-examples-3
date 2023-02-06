package ru.yandex.market.mboc.common.masterdata.model;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author jkt on 13.12.18.
 */
public class DocumentOfferRelationTest {

    @Test
    public void whenCreatingDocumentOfferRelationShouldInitTimestamp() {
        ShopSkuKey skuKey = new ShopSkuKey(1, "");
        QualityDocument document = new QualityDocument().setId(1);

        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(skuKey);
        masterData.addQualityDocument(document);

        LocalDateTime timeBeforeCreation = DateTimeUtils.dateTimeNow();

        DocumentOfferRelation relation = DocumentOfferRelation.from(skuKey, document);
        List<DocumentOfferRelation> relationsFromMasterData = DocumentOfferRelation.fromMasterData(masterData);

        assertSoftly(softly -> {
            softly.assertThat(relation.getModifiedTimestamp()).isAfterOrEqualTo(timeBeforeCreation);
            relationsFromMasterData.forEach(rel ->
                softly.assertThat(rel.getModifiedTimestamp()).isAfterOrEqualTo(timeBeforeCreation)
            );
        });
    }
}
