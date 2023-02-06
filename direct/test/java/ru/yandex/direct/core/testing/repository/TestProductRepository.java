package ru.yandex.direct.core.testing.repository;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.DSLContext;
import org.jooq.TableField;

import ru.yandex.direct.core.entity.product.repository.ProductsCache;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsCalcType;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsCurrency;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsType;
import ru.yandex.direct.dbschema.ppcdict.enums.ProductsUnit;
import ru.yandex.direct.dbschema.ppcdict.tables.records.ProductsRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static ru.yandex.direct.dbschema.ppcdict.tables.Products.PRODUCTS;

@ParametersAreNonnullByDefault
public class TestProductRepository {
    private static final List<? extends TableField<ProductsRecord, ? extends Serializable>> SELECT_FIELDS = List.of(
            PRODUCTS.PRODUCT_ID, PRODUCTS.PRODUCT_NAME, PRODUCTS.TYPE, PRODUCTS.CURRENCY, PRODUCTS.UNIT_NAME,
            PRODUCTS.ENGINE_ID, PRODUCTS.UNIT, PRODUCTS.CALC_TYPE);

    private final DslContextProvider dslContextProvider;
    private final ProductsCache productsCache;

    public TestProductRepository(DslContextProvider dslContextProvider, ProductsCache productsCache) {
        this.dslContextProvider = dslContextProvider;
        this.productsCache = productsCache;
    }

    public void addProduct(long productId, String productName, ProductsType productType, ProductsCurrency currency,
                            String unitName, long engineId, ProductsUnit unit, ProductsCalcType calcType) {
        DSLContext dslContext = dslContextProvider.ppcdict();

        dslContext.insertInto(PRODUCTS)
                .set(PRODUCTS.PRODUCT_ID, productId)
                .set(PRODUCTS.PRODUCT_NAME, productName)
                .set(PRODUCTS.THEME_ID, 0L)
                .set(PRODUCTS.TYPE, productType)
                .set(PRODUCTS.PRICE, BigDecimal.ONE)
                .set(PRODUCTS.CURRENCY, currency)
                .set(PRODUCTS.NDS, 0L)
                .set(PRODUCTS.UNIT_NAME, unitName)
                .set(PRODUCTS.ENGINE_ID, engineId)
                .set(PRODUCTS.RATE, 1L)
                .set(PRODUCTS.DAILY_SHOWS, (Long) null)
                .set(PRODUCTS.PACKET_SIZE, (Long) null)
                .set(PRODUCTS.PUBLIC_NAME_KEY, "")
                .set(PRODUCTS.PUBLIC_DESCRIPTION_KEY, "")
                .set(PRODUCTS.UNIT, unit)
                .set(PRODUCTS.UNIT_SCALE, 1L)
                .set(PRODUCTS.CALC_TYPE, calcType)
                .onDuplicateKeyIgnore()
                .execute();

        productsCache.invalidate();

        // может быть, несколько тестов пытаются заполнить один и тот же продукт, так что в insert-запросе
        // onDuplicateKeyIgnore, а потом проверяем, что дублирующая строка такая же, как требуется
        ProductsRecord dbRecord = dslContext.select(SELECT_FIELDS).from(PRODUCTS)
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .fetchOneInto(PRODUCTS);

        checkState(Objects.equals(dbRecord.getProductid(), productId));
        checkState(Objects.equals(dbRecord.getProductName(), productName));
        checkState(Objects.equals(dbRecord.getType(), productType));
        checkState(Objects.equals(dbRecord.getCurrency(), currency));
        checkState(Objects.equals(dbRecord.getUnitname(), unitName));
        checkState(Objects.equals(dbRecord.getEngineid(), engineId));
        checkState(Objects.equals(dbRecord.getUnit(), unit));
        checkState(Objects.equals(dbRecord.getCalcType(), calcType));
    }

}
