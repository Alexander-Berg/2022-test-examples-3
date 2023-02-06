package ru.yandex.market.wrap.infor.service.inbound.converter;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.model.AltSkuDTO;
import ru.yandex.market.wrap.infor.client.model.SkuAndPackDTO;
import ru.yandex.market.wrap.infor.model.LifetimeIndicator;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.DescriptionMeta;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.ItemMeta;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.UpsertableItem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchSkuDTOConverterTest extends SoftAssertionSupport {

    private static final UnitId UNIT_ID = new UnitId("ssku1", 1L, "ssku1");

    @Mock
    private AltSkuDTOConverter altSkuDTOConverterMock;

    @Mock
    private DescriptionMetaConverter descriptionMetaConverterMock;

    @Mock
    private BatchSkuDTOPackEnricher packEnricher;

    @InjectMocks
    private BatchSkuDTOConverter converter;


    /**
     * Проверяем корректность конвертации в SkuDTO в соответствии с маппингом:
     * https://wiki.yandex-team.ru/delivery/fulfilment/wms/Infor/mapping-wrap-inforSCE/#putReferenceItems
     */
    @Test
    void conversion() {
        List<AltSkuDTO> altSkus = Arrays.asList(new AltSkuDTO(), new AltSkuDTO(), new AltSkuDTO());

        doReturn(altSkus)
            .when(altSkuDTOConverterMock)
            .convert(any(ItemMeta.class));

        DescriptionMeta descrMeta = new DescriptionMeta("descr", "notes1", "notes2");

        doReturn(descrMeta)
            .when(descriptionMetaConverterMock)
            .convert(any(Item.class));

        ItemMeta meta = new ItemMeta(
            new Item.ItemBuilder(null, 1, null)
                .setBoxCount(1)
                .setUnitId(UNIT_ID)
                .setHasLifeTime(true)
                .setRemainingLifetimes(createRemainingLifetimes())
                .setUpdatedDateTime(new DateTime("2019-10-11T22:00:00.000"))
                    .setInboundServices(createInboundServices())
                .build(),
            UNIT_ID,
            "FMT_ID"
        );


        UpsertableItem item = new UpsertableItem(meta, null);

        SkuAndPackDTO result = converter.convert(item);

        softly.assertThat(result.getSku()).isEqualTo(meta.getWarehouseSku());
        softly.assertThat(result.getManufacturersku()).isEqualTo(UNIT_ID.getArticle());
        softly.assertThat(result.getStorerkey()).isEqualTo(UNIT_ID.getVendorId().toString());
        softly.assertThat(result.getDescr()).isEqualTo(descrMeta.getDescr());
        softly.assertThat(result.getNotes1()).isEqualTo(descrMeta.getNotes1());
        softly.assertThat(result.getNotes2()).isEqualTo(descrMeta.getNotes2());
        softly.assertThat(result.getAltSkus()).isEqualTo(altSkus);
        softly.assertThat(result.getShelflifeindicator()).isEqualTo(LifetimeIndicator.TRACK_LIFETIME.getValue());
        softly.assertThat(result.getSusr4()).isEqualTo("10");
        softly.assertThat(result.getSusr5()).isEqualTo("20");
        softly.assertThat(result.getShelflifeonreceivingPercentage()).isEqualTo(15);
        softly.assertThat(result.getShelflifePercentage()).isEqualTo(25);
        softly.assertThat(result.getShelflifeEditDate()).isEqualTo("2019-10-11T22:00+03:00");
        softly.assertThat(result.isNeedMeasurement()).isTrue();

        verify(altSkuDTOConverterMock).convert(meta);
        verify(descriptionMetaConverterMock).convert(meta.getItem());
    }

    private RemainingLifetimes createRemainingLifetimes() {
        return new RemainingLifetimes(
            createShelfLives(10, 15),
            createShelfLives(20, 25)
        );
    }

    private ShelfLives createShelfLives(Integer days, Integer percentage) {
        return new ShelfLives(
            new ShelfLife(days),
            new ShelfLife(percentage)
        );
    }

    private List<Service> createInboundServices() {
        return ImmutableList.of(
                new Service(
                        ServiceType.CHECK,
                        ServiceType.CHECK.getValue(),
                        "",
                        false),
                new Service(
                        null,
                        null,
                        "",
                        false),
                new Service(
                        ServiceType.MEASURE_ITEM,
                        ServiceType.MEASURE_ITEM.getValue(),
                        "",
                        false)
        );
    }
}
