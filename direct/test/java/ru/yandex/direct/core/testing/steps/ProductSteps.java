package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.repository.ProductRepository;
import ru.yandex.direct.core.entity.product.repository.ProductsCache;
import ru.yandex.direct.dbschema.ppcdict.tables.records.ProductsRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;
import ru.yandex.direct.utils.CollectionUtils;

import static ru.yandex.direct.dbschema.ppcdict.tables.Products.PRODUCTS;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@ParametersAreNonnullByDefault
public class ProductSteps {

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductsCache productsCache;

    public void addProductsIfNotExists(Collection<Product> products) {
        List<Product> existingProducts = productRepository.getAllProducts();
        List<Product> productsToAdd = filterList(products, product -> !containsProduct(existingProducts, product));

        if (!CollectionUtils.isEmpty(productsToAdd)) {
            InsertHelper<ProductsRecord> insertHelper =
                    new InsertHelper<>(dslContextProvider.ppcdict(), PRODUCTS);
            insertHelper.addAll(productRepository.productJooqMapper, products);
            insertHelper.executeIfRecordsAdded();
            productsCache.invalidate();
        }
    }

    public void removeProducts(Collection<Long> ids) {

        if (ids.isEmpty()) {
            return;
        }

        dslContextProvider.ppcdict().deleteFrom(PRODUCTS).where(PRODUCTS.PRODUCT_ID.in(ids)).execute();
    }

    private static boolean containsProduct(Collection<Product> products, Product product) {
        return StreamEx.of(products)
                .anyMatch(product1 -> isEqualProduct(product1, product));

    }

    private static boolean isEqualProduct(Product p1, Product p2) {
        return p1.getType() == p2.getType() &&
                p1.getCurrencyCode() == p2.getCurrencyCode() &&
                p1.getUnitName().equals(p2.getUnitName());
    }
}
