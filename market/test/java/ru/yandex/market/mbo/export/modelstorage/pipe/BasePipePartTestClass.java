package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Assert;
import org.junit.Before;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public abstract class BasePipePartTestClass {
    protected ModelPipeContextTestGenerator generator;

    @Before
    public void setUp() {
        AtomicLong idx = new AtomicLong(10L);
        generator = new ModelPipeContextTestGenerator(() -> idx.getAndAdd(1), false);
    }

    protected ModelStorage.Model createModification(ModelStorage.Model parent, boolean published) {
        return generator.createModificationFor(parent, published);
    }

    protected ModelStorage.Model createModel(boolean published) {
        return generator.createGuru(published);
    }

    protected ModelStorage.Model createPartnerModel(boolean published) {
        return generator.createModel(CommonModel.Source.PARTNER, published);
    }

    protected ModelStorage.Model createSkuFor(ModelStorage.Model model, boolean published) {
        return generator.createSkuFor(model, published);
    }

    protected ModelStorage.Model createPartnerSkuFor(ModelStorage.Model model, boolean published) {
        return generator.createSkuFor(CommonModel.Source.PARTNER_SKU, model, published);
    }

    protected ModelStorage.Model createModel(boolean published, boolean broken, boolean strictChecksRequired) {
        return generator.createGuru(published, broken, strictChecksRequired);
    }

    protected void checkAll(ModelPipeContext context, Predicate<ModelStorage.ModelOrBuilder> predicate,
                            String check) {
        if (context.getModel() != null) {
            assertCorrect(context.getModel(), predicate, check);
        }
        for (ModelStorage.Model.Builder mdf : context.getModifications()) {
            assertCorrect(mdf, predicate, check);
        }
        for (ModelStorage.Model.Builder sku : context.getSkus()) {
            assertCorrect(sku, predicate, check);
        }
    }

    protected void assertCorrect(ModelStorage.ModelOrBuilder mdl,
                                 Predicate<ModelStorage.ModelOrBuilder> predicate,
                                 String check) {
        Assert.assertTrue("model with id " + mdl.getId() + " not " + check, predicate.test(mdl));
    }
}
