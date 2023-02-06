package ru.yandex.market.mboc.tms.service.uee;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.SmMatchTarget;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.services.idxapi.pics.dto.ImageSignatureCollection;
import ru.yandex.market.mboc.common.services.idxapi.pics.dto.ImageSignatureLayer;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

import static org.assertj.core.api.Assertions.assertThat;

public class UeeYtServiceImplTest {
    private static final long CATEGORY_ID = 91491;
    private static final int VENDOR_ID = 100500;
    private static final long MSKU_ID1 = 100500101;
    private static final long MSKU_ID2 = 100500102;
    private static final String BASE_PATH = "//tmp/uee";
    public static final String FACTORS = "0 1";

    private TestYt testYt;
    private UeeYtService ueeYtService;

    @Before
    public void setUp() {
        testYt = new TestYt();
        StorageKeyValueServiceMock storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        storageKeyValueServiceMock.putValue("smartmatcher.version.current", "current");
        storageKeyValueServiceMock.putValue("smartmatcher.confidence.threshold.current", 0.0);
        ueeYtService = new UeeYtServiceImpl(UnstableInit.simple(testYt), BASE_PATH, storageKeyValueServiceMock);
    }

    @Test
    public void writeOffers() {
        List<Offer> offers = List.of(
            offer(1),
            offer(2)
        );
        ImageSignatureCollection imageSignatureCollection = new ImageSignatureCollection();
        imageSignatureCollection.putImageSignatures("pic1",
            Map.of(ImageSignatureLayer.SIGNATURE_MARKET_V10, FACTORS)
        );
        imageSignatureCollection.putImageSignatures("pic2",
            Map.of(ImageSignatureLayer.SIGNATURE_MARKET_V10, FACTORS)
        );
        String path = ueeYtService.writeOffers(offers, imageSignatureCollection, "current").toString();

        assertThat(testYt.cypress().exists(YPath.simple(path))).isTrue();

        testYt.tables().read(YPath.simple(path), YTableEntryTypes.YSON, entries -> {
            assertThat(entries.get(UeeInputTableDefinition.Columns.OFFER_ID.getColumnName()))
                .isNotEmpty().map(YTreeNode::longValue).get()
                .isIn(List.of(1L, 2L));
            assertThat(entries.get(UeeInputTableDefinition.Columns.CATEGORY_ID.getColumnName()))
                .isNotEmpty().map(YTreeNode::longValue).get()
                .isEqualTo(CATEGORY_ID);
            assertThat(entries.get(UeeInputTableDefinition.Columns.VENDOR_ID.getColumnName()))
                .isNotEmpty().map(YTreeNode::intValue).get()
                .isEqualTo(VENDOR_ID);
            assertThat(entries.get(UeeInputTableDefinition.Columns.TITLE.getColumnName()))
                .isNotEmpty().map(YTreeNode::stringValue).get().asString()
                .startsWith("Offer: ");
            assertThat(entries.get(UeeInputTableDefinition.Columns.DESCRIPTION.getColumnName()))
                .isNotEmpty().map(YTreeNode::stringValue).get().asString()
                .isIn(List.of("description1", "description2"));
            assertThat(entries.get(UeeInputTableDefinition.Columns.CURRENT_THRESHOLD.getColumnName()))
                .isNotEmpty().map(YTreeNode::doubleValue).get()
                .isEqualTo(0.0);
            assertThat(entries.get(UeeInputTableDefinition.Columns.SIGNATURES.getColumnName()))
                .isNotEmpty();
            YTreeNode yTreeNode = entries.get(UeeInputTableDefinition.Columns.SIGNATURES.getColumnName()).get();
            YTreeNode sign = yTreeNode.asMap().get(ImageSignatureLayer.SIGNATURE_MARKET_V10.getColumnName());
            assertThat(sign).isNotNull();
            assertThat(sign.asList()).hasSize(1)
                .singleElement().extracting(YTreeNode::stringValue).isEqualTo(FACTORS);
        });
    }

    @Test
    public void readOffers() {
        YPath outPath = YPath.simple(BASE_PATH).child("out");
        testYt.cypress().create(new CreateNode(outPath, ObjectType.Table));
        List<SMResult> smResults = List.of(
            SMResult.builder().confidence(1.).documentId(MSKU_ID1).matchTarget(SmMatchTarget.SKU_LIKE).build(),
            SMResult.builder().confidence(0.).documentId(MSKU_ID2).matchTarget(SmMatchTarget.SKU_LIKE).build()
        );

        prepareOutputData(outPath, smResults);

        Map<Long, SMResult> smResultMap = ueeYtService.readOffers(outPath.toString());
        assertThat(smResultMap)
            .hasSize(1)
            .containsOnlyKeys(1L)
        ;
        SMResult smResult = smResultMap.get(1L);

        assertThat(smResult.getDocumentId()).isEqualTo(MSKU_ID1);
        assertThat(smResult.getConfidence()).isEqualTo(1.);
    }

    private void prepareOutputData(YPath outPath, List<SMResult> smResults) {
        testYt.tables().write(outPath, YTableEntryTypes.YSON, List.of(
            YTree.mapBuilder()
                .key(UeeYtService.INPUT_OFFER_ID).value(1)
                .key(UeeYtService.OUTPUT_SM_RESULT).value(
                YTree.listBuilder()
                    .forEach(smResults, (yTreeBuilder, smResult) ->
                        yTreeBuilder.beginMap()
                            .key(SMResult.SM_CONFIDENCE_NODE).value(smResult.getConfidence())
                            .key(SMResult.SM_DOCUMENT_ID_NODE).value(smResult.getDocumentId())
                            .key(SMResult.SM_TYPE_NODE).value(smResult.getMatchTarget().name())
                            .endMap()
                    )
                    .buildList()
            )
                .buildMap(),
            YTree.mapBuilder()
                .key(UeeYtService.INPUT_OFFER_ID).value(2)
                .key(UeeYtService.OUTPUT_SM_RESULT).value(YTree.listBuilder().buildList())
                .buildMap()
        ));
    }

    private static Offer offer(int id) {
        return new Offer()
            .setId(id)
            .setTitle("Offer: " + id)
            .setCategoryIdForTests(UeeYtServiceImplTest.CATEGORY_ID, Offer.BindingKind.APPROVED)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.builder().sourcePicUrls("pic" + id).description("description" + id).build())
            .setVendorId(UeeYtServiceImplTest.VENDOR_ID);
    }

}
