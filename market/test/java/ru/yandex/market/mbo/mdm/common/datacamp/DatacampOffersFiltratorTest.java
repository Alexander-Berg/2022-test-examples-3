package ru.yandex.market.mbo.mdm.common.datacamp;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class DatacampOffersFiltratorTest extends MdmBaseDbTestClass {

    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    private DatacampOffersFiltrator filtrator;
    private StorageKeyValueServiceMock skv;

    @Before
    public void setup() {
        skv = new StorageKeyValueServiceMock();
        filtrator = new DatacampOffersFiltrator(null, null, skv, null, sskuExistenceRepository);
    }

    @Test
    public void testNoColorFilterUsesDefaultBlueOnly() {
        // given
        var sskus = List.of(
            ssku(100, "A", MasterDataSourceType.SUPPLIER, "aaa",
                flatSsku(1001, "A"),
                dsbsSskuWithDbsType(1002, "A") // отбросится
            ),
            ssku(200, "B", MasterDataSourceType.SUPPLIER, "bbb",
                dsbsSskuWithDbsType(2001, "B"), // отбросится
                flatSsku(2002, "B"),
                flatSsku(2003, "B")
            ),
            ssku(300, "C", MasterDataSourceType.SUPPLIER, "ccc",
                flatSsku(3001, "C"),
                flatSsku(3002, "C")
            )
        );

        // when
        var filteredSskus = filtrator.applyColorFilter(sskus);

        // then
        Assertions.assertThat(filteredSskus).containsExactlyInAnyOrder(
            ssku(100, "A", MasterDataSourceType.SUPPLIER, "aaa",
                flatSsku(1001, "A")
            ),
            ssku(200, "B", MasterDataSourceType.SUPPLIER, "bbb",
                flatSsku(2002, "B"),
                flatSsku(2003, "B")
            ),
            ssku(300, "C", MasterDataSourceType.SUPPLIER, "ccc",
                flatSsku(3001, "C"),
                flatSsku(3002, "C")
            )
        );
    }

    @Test
    public void testEmptiedBusinessNotDiscarded() {
        // given
        var sskus = List.of(
            ssku(100, "A", MasterDataSourceType.DBS, "aaa",
                dsbsSskuWithDbsType(1001, "A"),
                dsbsSskuWithDbsType(1002, "A")
            ),
            ssku(200, "B", MasterDataSourceType.DBS, "bbb",
                dsbsSskuWithDbsType(2001, "B"),
                dsbsSskuWithDbsType(2002, "B"),
                dsbsSskuWithDbsType(2003, "B")
            ),
            ssku(300, "C", MasterDataSourceType.SUPPLIER, "ccc",
                dsbsSskuWithDbsType(3001, "C"),
                dsbsSskuWithDbsType(3002, "C")
            )
        );

        // when
        var filteredSskus = filtrator.applyColorFilter(sskus);

        // then
        Assertions.assertThat(filteredSskus).containsExactlyInAnyOrder(
            ssku(100, "A", MasterDataSourceType.DBS, "aaa"),
            ssku(200, "B", MasterDataSourceType.DBS, "bbb"),
            ssku(300, "C", MasterDataSourceType.SUPPLIER, "ccc")
        );
    }

    @Test
    public void testAllColorsFilterAllowsAnyOffers() {
        // given
        var sskus = List.of(
            ssku(100, "A", MasterDataSourceType.SUPPLIER, "aaa",
                flatSsku(1001, "A"),
                dsbsSskuWithDbsType(1002, "A") // не отбросится
            ),
            ssku(200, "B", MasterDataSourceType.SUPPLIER, "bbb",
                dsbsSskuWithDbsType(2001, "B"), // не отбросится
                flatSsku(2002, "B"),
                flatSsku(2003, "B")
            ),
            ssku(300, "C", MasterDataSourceType.SUPPLIER, "ccc",
                flatSsku(3001, "C"),
                flatSsku(3002, "C")
            )
        );

        // when
        skv.putValue(MdmProperties.SSKU_COLOR_FILTER, new SskuColorFilter(DbsImportMode.BLUE_AND_DBS, 0, 0));
        var filteredSskus = filtrator.applyColorFilter(sskus);

        // then
        Assertions.assertThat(filteredSskus).containsExactlyInAnyOrderElementsOf(sskus);
    }

    @Test
    public void testRangeFilter() {
        // given
        var sskus = List.of(
            ssku(100, "A", MasterDataSourceType.SUPPLIER, "aaa",
                flatSsku(1001, "A"),
                dsbsSskuWithDbsType(1002, "A") // отбросится
            ),
            ssku(200, "B", MasterDataSourceType.SUPPLIER, "bbb",
                dsbsSskuWithDbsType(2001, "B"), // попадёт в рендж (по бизнесовому значению 200)
                flatSsku(2002, "B"),
                flatSsku(2003, "B")
            ),
            ssku(300, "C", MasterDataSourceType.SUPPLIER, "ccc",
                flatSsku(3001, "C"),
                flatSsku(3002, "C")
            )
        );

        // when
        skv.putValue(MdmProperties.SSKU_COLOR_FILTER, new SskuColorFilter(
            DbsImportMode.DBS_IN_BUSINESS_RANGE, 101, 201)); // 200 попадает в диапазон
        var filteredSskus = filtrator.applyColorFilter(sskus);

        // then
        Assertions.assertThat(filteredSskus).containsExactlyInAnyOrder(
            ssku(100, "A", MasterDataSourceType.SUPPLIER, "aaa",
                flatSsku(1001, "A")
            ),
            ssku(200, "B", MasterDataSourceType.SUPPLIER, "bbb",
                dsbsSskuWithDbsType(2001, "B"), // остался
                flatSsku(2002, "B"),
                flatSsku(2003, "B")
            ),
            ssku(300, "C", MasterDataSourceType.SUPPLIER, "ccc",
                flatSsku(3001, "C"),
                flatSsku(3002, "C")
            )
        );
    }

    @Test
    public void testExistenceOverridesColorRule() {
        // given
        var sskus = List.of(
            ssku(100, "A", MasterDataSourceType.DBS, "aaa",
                dsbsSskuWithDbsType(1001, "A"), // non existent
                dsbsSskuWithDbsType(1002, "A") // non existent
            ),
            ssku(200, "B", MasterDataSourceType.DBS, "bbb",
                dsbsSskuWithDbsType(2001, "B"), // non existent
                dsbsSskuWithDbsType(2002, "B") // existent
            ),
            ssku(300, "C", MasterDataSourceType.DBS, "ccc",
                dsbsSskuWithDbsType(3001, "C"), // existent
                dsbsSskuWithDbsType(3002, "C") // existent
            ),
            ssku(400, "D", MasterDataSourceType.SUPPLIER, "ddd",
                flatSsku(4001, "D"),
                dsbsSskuWithDbsType(4002, "D") // non existent
            ),
            ssku(500, "E", MasterDataSourceType.SUPPLIER, "eee",
                flatSsku(5001, "E"),
                dsbsSskuWithDbsType(5002, "E") // existent
            )
        );

        // when
        skv.putValue(MdmProperties.SSKU_COLOR_FILTER, new SskuColorFilter(
            DbsImportMode.DBS_IN_BUSINESS_RANGE, 1, 2)); // диапазон таков, что в него ничего не попадает.

        // Однако часть ДБСов нам уже известна по табличке service_exists:
        sskuExistenceRepository.markExistence(List.of(
            new ShopSkuKey(2002, "B"),
            new ShopSkuKey(3001, "C"),
            new ShopSkuKey(3002, "C"),
            new ShopSkuKey(5002, "E")
        ), true);

        var filteredSskus = filtrator.applyColorFilter(sskus);

        // then
        Assertions.assertThat(filteredSskus).containsExactlyInAnyOrder(
            ssku(100, "A", MasterDataSourceType.DBS, "aaa"),
            ssku(200, "B", MasterDataSourceType.DBS, "bbb",
                dsbsSskuWithDbsType(2001, "B"), // сервис не существовал, но благодаря "брату" мы его разрешили
                dsbsSskuWithDbsType(2002, "B") // брат-спаситель, который как раз и был в service-existence
            ),
            ssku(300, "C", MasterDataSourceType.DBS, "ccc", // тут оба сервиса были в existence, тоже ок
                dsbsSskuWithDbsType(3001, "C"),
                dsbsSskuWithDbsType(3002, "C")
            ),
            ssku(400, "D", MasterDataSourceType.SUPPLIER, "ddd", // Не прошли по фильтру, и сервиса не было в existence
                flatSsku(4001, "D") // как итог ДБС-сервис отброшен.
            ),
            ssku(500, "E", MasterDataSourceType.SUPPLIER, "eee",
                flatSsku(5001, "E"),
                dsbsSskuWithDbsType(5002, "E") // Сервис был нам известен и сохранился.
            )
        );
    }

    private SilverServiceSsku flatSsku(int supplierId,
                                       String shopSku,
                                       MasterDataSourceType type,
                                       String sourceId,
                                       SskuSilverParamValue... values
    ) {
        SilverServiceSsku ssku = new SilverServiceSsku(new SilverSskuKey(supplierId, shopSku, type, sourceId));
        SskuSilverParamValue someData = new SskuSilverParamValue();
        someData.setMdmParamId(Long.hashCode(supplierId));
        someData.setNumeric(BigDecimal.valueOf(supplierId * 2L));
        ssku.addParamValue(someData);

        someData = new SskuSilverParamValue();
        someData.setMdmParamId(shopSku.hashCode());
        someData.setString(shopSku);
        ssku.addParamValue(someData);
        ssku.addParamValues(List.of(values));
        return ssku;
    }

    private SilverServiceSsku flatSsku(int supplierId, String shopSku, SskuSilverParamValue... values) {
        return flatSsku(supplierId, shopSku, MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), values);
    }

    private SilverServiceSsku dsbsSskuWithDbsType(int supplierId, String shopSku, SskuSilverParamValue... values) {
        SskuSilverParamValue flag = new SskuSilverParamValue();
        flag.setMdmParamId(KnownMdmParams.IS_DBS);
        flag.setBool(true);
        return (SilverServiceSsku) flatSsku(supplierId, shopSku,
            MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(), values).addParamValue(flag);
    }

    private SilverCommonSsku ssku(int businessId,
                                  String shopSku,
                                  MasterDataSourceType type,
                                  String sourceId,
                                  SilverServiceSsku... services) {
        SilverCommonSsku ssku = new SilverCommonSsku(new SilverSskuKey(businessId, shopSku, type, sourceId));
        ssku.putServiceSskus(List.of(services));
        SskuSilverParamValue someData = new SskuSilverParamValue();
        someData.setMdmParamId(Long.hashCode(businessId));
        someData.setNumeric(BigDecimal.valueOf(businessId * 2L));
        ssku.addBaseValue(someData);

        someData = new SskuSilverParamValue();
        someData.setMdmParamId(shopSku.hashCode());
        someData.setString(shopSku);
        ssku.addBaseValue(someData);
        return ssku;
    }
}
