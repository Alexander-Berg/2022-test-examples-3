package ru.yandex.market.marketpromo.core.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

import com.yandex.ydb.table.description.TableColumn;
import com.yandex.ydb.table.description.TableDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.tables.promo.PromoYdbTable;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties;
import ru.yandex.market.marketpromo.model.MechanicsType;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.ydb.integration.model.Field;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;

/**
 * @author kl1san
 */
public class PromoDaoTest extends ServiceTestBase {

    @Autowired
    private PromoDao promoDao;
    @Autowired
    private PromoYdbTable promoYdbTable;

    private Promo promo;

    @BeforeEach
    void setUp() {
        promo = Promos.promo(
                id(IdentityUtils.hashId("some promo")),
                Promos.directDiscount(
                        minimalDiscountPercentSize(13)
                ),
                Promos.category(123L, 10),
                Promos.category(124L, 15),
                Promos.category(125L, 20)
        );
    }

    @Test
    void shouldDescribeTable() {
        TableDescription promo = promoDao.describeTables().get(promoYdbTable.tableName());
        Set<String> columnNames = promo.getColumns().stream().map(TableColumn::getName).collect(Collectors.toSet());
        assertThat(columnNames, containsInAnyOrder(promoYdbTable.fields().stream()
                .map(Field::name)
                .toArray()));
    }

    @Test
    void shouldCreatePromo() {
        promoDao.replace(promo);
        Promo result = promoDao.findExistedByPromoId(promo.getPromoId()).orElseThrow();

        assertThat(result.getMechanicsType(), is(MechanicsType.DIRECT_DISCOUNT));
        assertThat(result.getCategoriesWithDiscounts(), hasSize(3));
        assertThat(result.getCategoriesWithDiscounts(), hasItems(allOf(
                hasProperty("categoryId", comparesEqualTo(123L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(10)))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(124L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(15)))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(125L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(20)))
        )));
        DirectDiscountProperties directDiscountProperties =
                result.getMechanicsPropertiesAs(DirectDiscountProperties.class).orElseThrow();
        assertThat(directDiscountProperties.getMinimalDiscountPercentSize(), comparesEqualTo(BigDecimal.valueOf(13)));
    }

    @Test
    void shouldUpdatePromo() {
        promoDao.replace(promo);
        promoDao.replace(Promos.promo(
                id(IdentityUtils.hashId("some promo")),
                Promos.directDiscount(
                        minimalDiscountPercentSize(13)
                ),
                Promos.category(123L, 10),
                Promos.category(124L, 15),
                Promos.category(126L, 26)
        ));
        Promo result = promoDao.findExistedByPromoId(promo.getPromoId()).orElseThrow();

        assertThat(result.getMechanicsType(), is(MechanicsType.DIRECT_DISCOUNT));
        assertThat(result.getCategoriesWithDiscounts(), hasSize(3));
        assertThat(result.getCategoriesWithDiscounts(), hasItems(allOf(
                hasProperty("categoryId", comparesEqualTo(123L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(10)))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(124L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(15)))
        ), allOf(
                hasProperty("categoryId", comparesEqualTo(126L)),
                hasProperty("discount", comparesEqualTo(BigDecimal.valueOf(26)))
        )));
        DirectDiscountProperties directDiscountProperties = (DirectDiscountProperties) result.getMechanicsProperties();
        assertThat(directDiscountProperties.getMinimalDiscountPercentSize(), comparesEqualTo(BigDecimal.valueOf(13)));
    }

    @Test
    void shouldUpdatePromoUpdatedTimes() {
        final LocalDateTime localDateTime = clock.dateTime();
        promoDao.replace(this.promo);
        clock.setFixed(Instant.now().plus(10, ChronoUnit.DAYS), clock.getZone());
        promoDao.updatePromo(this.promo.toPromoKey());
        final Promo promo = promoDao.get(this.promo.toPromoKey());
        assertThat(promo, notNullValue());
        final LocalDateTime updatedAt = promo.getUpdatedAt();

        assertTrue(updatedAt.isAfter(localDateTime.plus(9, ChronoUnit.DAYS)));
    }

    @Test
    void shouldSetHasErrors() {
        promoDao.replace(this.promo);
        promoDao.setPromoHasErrors(this.promo.toPromoKey(), true);
        final Promo promo = promoDao.get(this.promo.toPromoKey());
        assertThat(promo, notNullValue());
        assertThat(promo.getHasErrors(), notNullValue());
        assertTrue(promo.getHasErrors());
    }

    @Test
    void shouldNotSetHasErrors() {
        promoDao.replace(this.promo);
        promoDao.setPromoHasErrors(this.promo.toPromoKey(), false);
        final Promo promo = promoDao.get(this.promo.toPromoKey());
        assertThat(promo, notNullValue());
        assertThat(promo.getHasErrors(), notNullValue());
        assertFalse(promo.getHasErrors());
    }

    @Test
    void shouldSetHasChanges() {
        promoDao.replace(this.promo);
        promoDao.updatePromoHasChanges(this.promo.getId(), true);
        final Promo promo = promoDao.get(this.promo.toPromoKey());
        assertThat(promo, notNullValue());
        assertThat(promo.getHasNotPublishedChanges(), notNullValue());
        assertTrue(promo.getHasNotPublishedChanges());
    }
}
