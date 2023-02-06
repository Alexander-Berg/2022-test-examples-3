package ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.MapUtils;
import ru.yandex.market.ir.autogeneration.common.util.ModelBuilder;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class DcpPicturesFromModelCollectorTest {

    private static final long HID = 91491;
    private static final long PSKU_ID1 = 101L;
    private static final long PSKU_ID2 = 102L;

    private static final String MBO_AVATAR_URL_1 =
        "//avatars.mds.yandex.net/get-mpic/1961245/img_id8835968184976353325.jpeg/orig";
    private static final String MBO_AVATAR_URL_2 =
        "//avatars.mds.yandex.net/get-mpic/1886039/img_id7814404310959047598.jpeg/orig";
    private static final String MBO_AVATAR_URL_3 =
        "//avatars.mds.yandex.net/get-mpic/1256333/img_id7814404317474747474.jpeg/orig";

    private static final GcSkuTicket TICKET_WITH_PSKU_1 = makeTicket(1L, PSKU_ID1);
    private static final GcSkuTicket TICKET_WITH_PSKU_2 = makeTicket(2L, PSKU_ID2);
    private static final GcSkuTicket TICKET_WITHOUT_PSKU = makeTicket(3L, null);

    private static final ModelStorage.Picture PIC1 =
        ModelStorage.Picture.newBuilder()
            .setUrlOrig(MBO_AVATAR_URL_1)
            .build();
    private static final ModelStorage.Picture PIC2 =
        ModelStorage.Picture.newBuilder()
            .setUrlOrig(MBO_AVATAR_URL_2)
            .build();
    private static final ModelStorage.Picture PIC3 =
        ModelStorage.Picture.newBuilder()
            .setUrlOrig(MBO_AVATAR_URL_3)
            .build();


    private ModelStorageHelper modelStorageHelper;
    private ModelStorageServiceMock modelStorageServiceMock;
    private DcpPicturesFromModelCollector collector;

    @Before
    public void setUp() {
        modelStorageServiceMock = Mockito.spy(new ModelStorageServiceMock());
        modelStorageHelper = Mockito.spy(new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock));
        modelStorageServiceMock.putModels(
            ModelBuilder.newBuilder(PSKU_ID1, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .picture(PIC1)
                .build(),
            ModelBuilder.newBuilder(PSKU_ID2, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .picture(PIC2)
                .picture(PIC3)
                .build()
        );
        collector = new DcpPicturesFromModelCollector(modelStorageHelper);
    }

    @Test
    public void testSimple() {
        ImmutableMap<String, Long> urlsMap = ImmutableMap.of(
            MBO_AVATAR_URL_1, TICKET_WITH_PSKU_1.getId(),
            MBO_AVATAR_URL_2, TICKET_WITH_PSKU_2.getId(),
            MBO_AVATAR_URL_3, TICKET_WITH_PSKU_2.getId()
        );
        List<DcpPartnerPicture> dcpPartnerPictures =
            collector.collectDcpPicturesFromPskus(urlsMap, Arrays.asList(TICKET_WITH_PSKU_1, TICKET_WITH_PSKU_2));
        Map<String, Long> urlToTicketIdMap =
            MapUtils.toMap(dcpPartnerPictures, DcpPartnerPicture::getIdxAvatarUrl, DcpPartnerPicture::getGcSkuTicketId);
        ImmutableMap<String, Long> expected = ImmutableMap.of(
            MBO_AVATAR_URL_1, TICKET_WITH_PSKU_1.getId(),
            MBO_AVATAR_URL_2, TICKET_WITH_PSKU_2.getId(),
            MBO_AVATAR_URL_3, TICKET_WITH_PSKU_2.getId()
        );
        Assertions.assertThat(dcpPartnerPictures).hasSize(expected.size());
        Assertions.assertThat(urlToTicketIdMap).containsAllEntriesOf(expected);
        Assertions.assertThat(dcpPartnerPictures).extracting(DcpPartnerPicture::getForceCwValidationOk)
            .containsOnly(true);
    }

    @Test
    public void testWhenNoMappedPsku() {
        ImmutableMap<String, Long> urlsMap = ImmutableMap.of(
            MBO_AVATAR_URL_1, TICKET_WITHOUT_PSKU.getId()
        );
        List<DcpPartnerPicture> dcpPartnerPictures =
            collector.collectDcpPicturesFromPskus(urlsMap, Arrays.asList(TICKET_WITH_PSKU_1, TICKET_WITH_PSKU_2));
        Assertions.assertThat(dcpPartnerPictures).isEmpty();
    }

    @Test
    public void testWhenDuplicatePicturesThenReturnOnlyUniqueOnes() {
        modelStorageServiceMock.putModels(
            ModelBuilder.newBuilder(PSKU_ID1, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .picture(PIC1)
                .picture(PIC1)
                .build(),
            ModelBuilder.newBuilder(PSKU_ID2, HID)
                .currentType(ModelStorage.ModelType.PARTNER_SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .picture(PIC2)
                .picture(PIC2)
                .picture(PIC3)
                .build()
        );

        ImmutableMap<String, Long> urlsMap = ImmutableMap.of(
            MBO_AVATAR_URL_1, TICKET_WITH_PSKU_1.getId(),
            MBO_AVATAR_URL_2, TICKET_WITH_PSKU_2.getId(),
            MBO_AVATAR_URL_3, TICKET_WITH_PSKU_2.getId()
        );
        List<DcpPartnerPicture> dcpPartnerPictures =
            collector.collectDcpPicturesFromPskus(urlsMap, Arrays.asList(TICKET_WITH_PSKU_1, TICKET_WITH_PSKU_2));
        Assertions.assertThat(dcpPartnerPictures).hasSize(3);
        Assertions.assertThat(dcpPartnerPictures.stream()
            .map(DcpPartnerPicture::getMboPicture)
            .collect(Collectors.toList())).containsOnly(PIC1, PIC2, PIC3);
    }


    private static GcSkuTicket makeTicket(long id, Long existingPskuId) {
        GcSkuTicket skuTicket = new GcSkuTicket();
        skuTicket.setId(id);
        skuTicket.setExistingMboPskuId(existingPskuId);
        return skuTicket;
    }

}
