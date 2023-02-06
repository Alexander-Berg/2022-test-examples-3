package ru.yandex.market.wms.radiator.repository.sku;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;
import ru.yandex.market.wms.radiator.dto.SkuAndMiniPackDTO;
import ru.yandex.market.wms.radiator.dto.SkuMiniPackIdentitiesDto;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuAutoGetStocksTest;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuRefIdentities;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuRefMultibox;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/stocks.xml", connection = "wh1Connection"),
})
class SkuRepositoryTest_forEachRemaining extends IntegrationTestBackend {

    @Autowired
    private SkuRepository skuRepository;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void forEachRemaining2() {
        forEachRemaining(WH_1_ID);
    }


    private void forEachRemaining(String warehouseId) {
        dispatcher.withWarehouseId(
                warehouseId, () -> {
                    List<SkuMiniPackIdentitiesDto> result = new ArrayList<>();
                    skuRepository.forEachRemaining(result::add);

                    assertThat(result.size(), is(3));
                    verify(result.get(0), mSkuAutoGetStocksTest(), "Y");
                    verify(result.get(1), mSkuRefMultibox(), "N");
                    verify(result.get(2), mSkuRefIdentities(), "Y");
                }
        );
    }

    private static void verify(SkuMiniPackIdentitiesDto dto, ItemReference itemReference, String shelfLife) {
        var item = itemReference.getItem();
        assertThat(dto.getManufacturersku(), is(equalTo(item.getUnitId().getArticle())));           // manufacturersku
        assertThat(dto.getStorerkey(), is(equalTo(item.getUnitId().getVendorId().toString())));     // storerkey

        assertThat(dto.getStdgrosswgt().doubleValue(), is(equalTo(item.getKorobyte().getWeightGross().doubleValue()))); // stdgrosswgt
        assertThat(dto.getStdnetwgt().doubleValue(), is(equalTo(item.getKorobyte().getWeightNet().doubleValue())));     // stdnetwgt
        assertThat(dto.getTare().doubleValue(), is(equalTo(item.getKorobyte().getWeightTare().doubleValue())));         // tare

        assertThat(dto.getShelflifeindicator(), is(equalTo(shelfLife)));    // shelflifeindicator
        assertThat(dto.getToexpiredays(), is(equalTo(item.getLifeTime() == null ? 0 : item.getLifeTime())));    // toexpiredays

        assertThat(dto.getDescr(), is(equalTo(item.getName())));    // descr

        assertThat(dto.getSusr1(), is(equalTo(item.getBoxCount() == null ? null : item.getBoxCount().toString())));

        // susr4
        assertThat(dto.getSusr4(), is(
                Optional.ofNullable(item.getRemainingLifetimes())
                        .flatMap(remainingLifetimes -> Optional.ofNullable(remainingLifetimes.getInbound()))
                        .flatMap(shelfLives -> Optional.ofNullable(shelfLives.getDays()))
                        .flatMap(shelfLive -> Optional.ofNullable(shelfLive.getValue()))
                        .map(Object::toString)
                        .orElse(null)
        ));
        // shelflifeOnReceivingPercentage
        assertThat(dto.getShelflifeonreceivingPercentage(), is(
                Optional.ofNullable(item.getRemainingLifetimes())
                        .flatMap(remainingLifetimes -> Optional.ofNullable(remainingLifetimes.getInbound()))
                        .flatMap(shelfLives -> Optional.ofNullable(shelfLives.getPercentage()))
                        .flatMap(shelfLive -> Optional.ofNullable(shelfLive.getValue()))
                        .orElse(null)
        ));

        // susr5
        assertThat(dto.getSusr5(), is(
                Optional.ofNullable(item.getRemainingLifetimes())
                        .flatMap(remainingLifetimes -> Optional.ofNullable(remainingLifetimes.getOutbound()))
                        .flatMap(shelfLives -> Optional.ofNullable(shelfLives.getDays()))
                        .flatMap(shelfLive -> Optional.ofNullable(shelfLive.getValue()))
                        .map(Object::toString)
                        .orElse(null)
        ));
        // shelflifePercentage
        assertThat(dto.getShelflifePercentage(), is(
                Optional.ofNullable(item.getRemainingLifetimes())
                        .flatMap(remainingLifetimes -> Optional.ofNullable(remainingLifetimes.getOutbound()))
                        .flatMap(shelfLives -> Optional.ofNullable(shelfLives.getPercentage()))
                        .flatMap(shelfLive -> Optional.ofNullable(shelfLive.getValue()))
                        .orElse(null)
        ));

        // shelflife_editdate
        assertThat(
                dto.getShelflifeEditDate() == null ? null : dto.getShelflifeEditDate(),
                is(equalTo(item.getUpdated() == null ? null : item.getUpdated().getOffsetDateTime())));
    }
}
