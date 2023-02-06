package ru.yandex.market.aliasmaker.cache.models;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.aliasmaker.cache.AliasMakerUtils;
import ru.yandex.market.mbo.http.ModelStorage;

public class SearchModelsTest {
    private static final int CATEGORY_ID = 1000;
    private static final long VENDOR_ID = 1;
    private static final long VENDOR_ID_1 = 2;
    private static long idSeq = 1L;
    private static long modifiedTsSeq = 1L;

    private CategoryModelsCache guruCache;
    private CategoryModelsCache skuCache;

    @Before
    public void init() {
        guruCache = new CategoryModelsCacheImpl(ModelStorage.ModelType.GURU, CATEGORY_ID);
        skuCache = new CategoryModelsCacheImpl(ModelStorage.ModelType.SKU, CATEGORY_ID);
    }

    @Test
    public void testSimpleNoSKU() {
        ModelBuilder guru = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture_1", "bla")
                .addPicture("XL-Picture", "bla1");
        addToCache(guru);
        assertResult(guru, "bla1");
    }

    @Test
    public void testSimpleWithSku() {
        ModelBuilder guru = ModelBuilder.guru(VENDOR_ID)
                .addPicture("Big-Picture", "bla");
        ModelBuilder sku = ModelBuilder.sku(VENDOR_ID)
                .addPicture(null, "vla")
                .addRelation(guru);
        addToCache(guru, sku);
        assertResult(guru, "vla");
    }

    @Test(expected = NullPointerException.class)
    public void testSkuIsAbsent() {
        ModelBuilder guru = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture", "bla");
        ModelBuilder sku = ModelBuilder.sku(VENDOR_ID_1)
                .addPicture(null, "vla")
                .addRelation(guru);
        addToCache(guru, sku);
        assertResult(guru, null);
    }

    @Test
    public void testNoSkuPicture() {
        ModelBuilder guru = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture", "bla");
        ModelBuilder sku = ModelBuilder.sku(VENDOR_ID)
                .addRelation(guru);
        addToCache(guru, sku);
        assertResult(guru, "bla");
    }

    @Test
    public void testTwoSku() {
        ModelBuilder guru = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture", "bla");
        ModelBuilder sku = ModelBuilder.sku(VENDOR_ID)
                .addRelation(guru);
        ModelBuilder sku1 = ModelBuilder.sku(VENDOR_ID)
                .addPicture(null, "vla")
                .addRelation(guru);
        addToCache(guru, sku, sku1);
        assertResult(guru, "vla");
    }

    @Test
    public void testGroupNoSku() {
        ModelBuilder guru = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture_2", "bla");
        ModelBuilder modif = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture_1", "vla")
                .addRelation(guru);
        ModelBuilder modif2 = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture", "gla")
                .addRelation(guru);
        addToCache(guru, modif, modif2);

        assertResult(guru, "gla");
        assertResult(modif, "vla");
        assertResult(modif2, "gla");
    }

    @Test
    public void testGroupWithSkus() {
        ModelBuilder guru = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture_2", "bla");
        ModelBuilder modif = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture_1", "vla")
                .addRelation(guru);
        ModelBuilder sku = ModelBuilder.sku(VENDOR_ID)
                .addPicture(null, "ska")
                .addRelation(modif);
        ModelBuilder modif2 = ModelBuilder.guru(VENDOR_ID)
                .addPicture("XL-Picture", "gla")
                .addRelation(guru);
        ModelBuilder sku2 = ModelBuilder.sku(VENDOR_ID)
                .addPicture(null, "ska2")
                .addRelation(modif2);
        addToCache(guru, modif, modif2, sku, sku2);

        assertResult(guru, "ska2"); //second modification is created later
    }

    private void assertResult(ModelBuilder modelBuilder, String result) {
        Multimap<Long, ModelStorage.Model> byParentId = ArrayListMultimap.create();
        if (modelBuilder.isGuru()) {
            guruCache.getModelsStream().sorted(Comparator.comparing(ModelStorage.Model::getModifiedTs).reversed())
                    .forEach(m -> {
                        if (m.getParentId() > 0) {
                            byParentId.put(m.getParentId(), m);
                        }
                    });
        }
        ModelStorage.Model model = Stream.concat(guruCache.getModelsStream(), skuCache.getModelsStream())
                .filter(m -> m.getId() == modelBuilder.mdlBuilder.getId())
                .findFirst()
                .get();
        Optional<String> res = AliasMakerUtils.findModelPicture(
                guruCache, skuCache, model, byParentId);
        Assertions.assertThat(res).isEqualTo(Optional.ofNullable(result));
    }

    private void addToCache(ModelBuilder... builders) {
        for (ModelBuilder builder : builders) {
            CategoryModelsCache cache = builder.isSku() ? skuCache : guruCache;
            ModelStorage.Model model = builder.mdlBuilder.build();
            cache.putModel(model);
        }
    }

    private static class ModelBuilder {
        ModelStorage.Model.Builder mdlBuilder;

        static ModelBuilder sku(long vendorId) {
            return forType(ModelStorage.ModelType.SKU, vendorId);
        }

        static ModelBuilder guru(long vendorId) {
            return forType(ModelStorage.ModelType.GURU, vendorId);
        }

        static ModelBuilder forType(ModelStorage.ModelType type, long vendorId) {
            ModelBuilder result = new ModelBuilder();
            result.mdlBuilder = ModelStorage.Model.newBuilder()
                    .setId(idSeq++)
                    .setVendorId(vendorId)
                    .setModifiedTs(modifiedTsSeq++)
                    .setCurrentType(type.name());
            return result;
        }

        ModelBuilder addRelation(ModelBuilder relatedBuilder) {
            if (isSku() || relatedBuilder.isSku()) {
                ModelStorage.RelationType current = isSku() ? ModelStorage.RelationType.SKU_PARENT_MODEL
                        : ModelStorage.RelationType.SKU_MODEL;
                ModelStorage.RelationType target = isSku() ? ModelStorage.RelationType.SKU_MODEL
                        : ModelStorage.RelationType.SKU_PARENT_MODEL;
                mdlBuilder.addRelations(ModelStorage.Relation.newBuilder()
                        .setType(current)
                        .setId(relatedBuilder.mdlBuilder.getId())
                        .build());
                relatedBuilder.mdlBuilder.addRelations(ModelStorage.Relation.newBuilder()
                        .setType(target)
                        .setId(mdlBuilder.getId())
                        .build());
            } else {
                mdlBuilder.setParentId(relatedBuilder.mdlBuilder.getId());
            }
            return this;
        }

        ModelBuilder addPicture(@Nullable String xslName, String url) {
            ModelStorage.Picture.Builder picture = ModelStorage.Picture.newBuilder()
                    .setUrl(url);
            if (xslName != null) {
                picture.setXslName(xslName);
            }
            mdlBuilder.addPictures(picture);
            return this;
        }

        boolean isGuru() {
            return !isSku();
        }

        boolean isSku() {
            return mdlBuilder.getCurrentType().equals(ModelStorage.ModelType.SKU.name());
        }
    }
}
