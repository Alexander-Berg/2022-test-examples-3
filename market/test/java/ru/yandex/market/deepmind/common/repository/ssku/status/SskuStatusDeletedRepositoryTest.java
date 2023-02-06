package ru.yandex.market.deepmind.common.repository.ssku.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.availability.ssku.status.deleted.SskuStatusDeletedFilter;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;

public class SskuStatusDeletedRepositoryTest extends DeepmindBaseDbTestClass {
    @Autowired
    private SskuStatusDeletedRepository sskuStatusDeletedRepository;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    public void statusesAreBeingAdded() {
        //arrange
        var sskuKey1 = new ServiceOfferKey(1, "1");
        var sskuKey2 = new ServiceOfferKey(2, "2");

        //act
        sskuStatusDeletedRepository.addByKeys(sskuKey1, sskuKey2);

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findAllKeys())
            .containsExactlyInAnyOrderElementsOf(List.of(sskuKey1, sskuKey2));
    }

    @Test
    public void additionDoesNotCauseDuplicates() {
        //arrange
        var sskuKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(sskuKey);

        //act
        sskuStatusDeletedRepository.addByKeys(sskuKey);

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findAllKeys())
            .containsExactly(sskuKey);
    }

    @Test
    public void statusesAreBeingRemoved() {
        //arrange
        var keyToDelete = new ServiceOfferKey(1, "1");
        var keyToStay = new ServiceOfferKey(2, "2");
        sskuStatusDeletedRepository.addByKeys(keyToDelete, keyToStay);

        //act
        sskuStatusDeletedRepository.removeByKeys(keyToDelete);

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findAllKeys())
            .containsExactly(keyToStay);
    }

    @Test
    public void deletingNonExistentStatusesDoesNotLeadToErrors() {
        //arrange
        var nonExistentStatus = new ServiceOfferKey(1, "1");

        //assert
        Assertions.assertThatCode(() -> sskuStatusDeletedRepository.removeByKeys(nonExistentStatus))
            .doesNotThrowAnyException();
        Assertions.assertThat(sskuStatusDeletedRepository.findAllKeys())
            .isEmpty();
    }

    @Test
    public void statusesAreFound() {
        //arrange
        var sskuKey1 = new ServiceOfferKey(1, "1");
        var sskuKey2 = new ServiceOfferKey(2, "2");

        //act
        sskuStatusDeletedRepository.addByKeys(sskuKey1, sskuKey2);

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.find(sskuKey1, sskuKey2))
            .extracting(s -> new ServiceOfferKey(s.getSupplierId(), s.getShopSku()))
            .containsExactlyInAnyOrderElementsOf(List.of(sskuKey1, sskuKey2));
    }

    @Test
    public void sskuKeysAreFound() {
        //arrange
        var sskuKey1 = new ServiceOfferKey(1, "1");
        var sskuKey2 = new ServiceOfferKey(2, "2");

        //act
        sskuStatusDeletedRepository.addByKeys(sskuKey1, sskuKey2);

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findKeys(sskuKey1, sskuKey2))
            .containsExactlyInAnyOrderElementsOf(List.of(sskuKey1, sskuKey2));
    }

    @Test
    public void statusesAreBeingFilteredBySskuKeys() {
        //arrange
        var sskuKey1 = new ServiceOfferKey(1, "1");
        var sskuKey2 = new ServiceOfferKey(2, "2");
        sskuStatusDeletedRepository.addByKeys(sskuKey1, sskuKey2);

        //act
        var filter = new SskuStatusDeletedFilter().setShopSkuKeys(List.of(sskuKey1));

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.find(filter))
            .extracting(s -> new ServiceOfferKey(s.getSupplierId(), s.getShopSku()))
            .containsExactly(sskuKey1)
            .doesNotContain(sskuKey2);
    }

    @Test
    public void sskuKeysAreBeingFilteredBySskuKeys() {
        //arrange
        var sskuKey1 = new ServiceOfferKey(1, "1");
        var sskuKey2 = new ServiceOfferKey(2, "2");
        sskuStatusDeletedRepository.addByKeys(sskuKey1, sskuKey2);

        //act
        var filter = new SskuStatusDeletedFilter()
            .setShopSkuKeys(List.of(sskuKey1));

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findKeys(filter))
            .containsExactly(sskuKey1)
            .doesNotContain(sskuKey2);
    }

    @Test
    public void sskuKeysAreBeingFilteredByDeletedAt() {
        //arrange
        var oldKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(oldKey);
        reduceDeletedTsForNewStatusesByHour();

        var newKey = new ServiceOfferKey(2, "2");
        sskuStatusDeletedRepository.addByKeys(newKey);

        //act
        var filter = new SskuStatusDeletedFilter()
            .setDeletedAtBefore(Instant.now().minus(10, ChronoUnit.MINUTES));

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findKeys(filter))
            .containsExactly(oldKey)
            .doesNotContain(newKey);
    }

    public void reduceDeletedTsForNewStatusesByHour() {
        namedParameterJdbcTemplate.update(
            "update msku.ssku_status_deleted\n" +
                "set deleted_ts = :hour_ago\n" +
                "where deleted_ts > :hour_ago",
            new MapSqlParameterSource()
                .addValue("hour_ago", Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)))
        );
    }

    @Test
    public void sskuKeysAreBeingFilteredByDeletedFromYt() {
        //arrange
        var deletedKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(deletedKey);
        markAllStatusesDeletedFromYt();

        //act
        var newKey = new ServiceOfferKey(2, "2");
        sskuStatusDeletedRepository.addByKeys(newKey);

        //assert
        var uploadedToYtFilter = new SskuStatusDeletedFilter().setNotDeletedFromYt(true);
        Assertions.assertThat(sskuStatusDeletedRepository.findKeys(uploadedToYtFilter))
            .containsExactly(newKey)
            .doesNotContain(deletedKey);
    }

    public void markAllStatusesDeletedFromYt() {
        namedParameterJdbcTemplate.update(
            "update msku.ssku_status_deleted\n" +
                "set yt_deleted_ts = :now\n" +
                "where yt_deleted_ts is null",
            new MapSqlParameterSource()
                .addValue("now", Timestamp.from(Instant.now()))
        );
    }

    @Test
    public void keysAreReturnedNoMoreThanLimit() {
        //arrange
        var sskuKey1 = new ServiceOfferKey(1, "1");
        var sskuKey2 = new ServiceOfferKey(2, "2");
        var sskuKey3 = new ServiceOfferKey(3, "3");
        sskuStatusDeletedRepository.addByKeys(sskuKey1, sskuKey2, sskuKey3);

        //act
        var limitFilter = new SskuStatusDeletedFilter()
            .setLimit(2);

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findKeys(limitFilter))
            .hasSize(2);
    }

    @Test
    public void statusesAreBeingCounted() {
        //arrange
        var sskuKey1 = new ServiceOfferKey(1, "1");
        var sskuKey2 = new ServiceOfferKey(2, "2");
        var sskuKey3 = new ServiceOfferKey(3, "3");
        sskuStatusDeletedRepository.addByKeys(sskuKey1, sskuKey2, sskuKey3);

        //act
        var count = sskuStatusDeletedRepository.findCount(new SskuStatusDeletedFilter());

        //assert
        Assertions.assertThat(count).isEqualTo(3);
    }

    @Test
    public void deletedFromYtAreNotCountedWithFilter() {
        //arrange
        var deletedFromYtKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(deletedFromYtKey);
        markAllStatusesDeletedFromYt();

        var keyToCount = new ServiceOfferKey(3, "3");
        sskuStatusDeletedRepository.addByKeys(keyToCount);

        var filter = new SskuStatusDeletedFilter()
            .setNotDeletedFromYt(true);

        //act
        var count = sskuStatusDeletedRepository.findCount(filter);

        //assert
        Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    public void statusesAreBeingOrderedByFilter() {
        //arrange
        var key1 = new ServiceOfferKey(1, "1");
        var key2 = new ServiceOfferKey(2, "2");

        sskuStatusDeletedRepository.addByKeys(key1, key2);

        reduceDeletedTsByHour(key2);

        var filter = new SskuStatusDeletedFilter()
            .setOrderBy(SskuStatusDeletedFilter.OrderBy.DELETED_TS);

        //assert
        Assertions.assertThat(sskuStatusDeletedRepository.findKeys(filter))
            .containsExactlyElementsOf(List.of(key2, key1));
    }

    public void reduceDeletedTsByHour(ServiceOfferKey shopSkuKey) {
        namedParameterJdbcTemplate.update(
            "update msku.ssku_status_deleted\n" +
                "set deleted_ts = :hour_ago\n" +
                "where msku.ssku_status_deleted.supplier_id = :supplier_id" + " and " +
                "msku.ssku_status_deleted.shop_sku = :shop_sku",
            new MapSqlParameterSource()
                .addValue("hour_ago", Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .addValue("supplier_id", shopSkuKey.getSupplierId())
                .addValue("shop_sku", shopSkuKey.getShopSku())
        );
    }
}
