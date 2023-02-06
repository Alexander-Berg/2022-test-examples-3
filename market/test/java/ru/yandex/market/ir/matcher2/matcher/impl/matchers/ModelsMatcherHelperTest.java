package ru.yandex.market.ir.matcher2.matcher.impl.matchers;

import org.junit.Test;

import ru.yandex.market.ir.matcher2.matcher.MatchType;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Dimension;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Model;
import ru.yandex.market.ir.matcher2.matcher.alternate.be.Modification;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.matchers.ModelsMatcherHelper;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Infix;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Level;
import ru.yandex.market.ir.http.Matcher;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author inenakhov
 */
public class ModelsMatcherHelperTest {
    private Model publishedModel = new Model(1, true, createDummyDimension(), false, false, "published");
    private Model unpublishedModel = new Model(1, false, createDummyDimension(), false, false, "unpublished");
    private Modification publishedModification = new Modification(1, 1, true,
                                                          createDummyDimension(), true, true, "published_modification");
    private Modification unpublishedModification = new Modification(1, 1, false,
                                                            createDummyDimension(), true, true, "unpublished_modification");
    private Level vendorLevel = new Level(1, Infix.GOOD_ID_INFIX, Infix.EMPTY_ARRAY, "vendor");

    @Test
    public void getPublishedOnMarket() throws Exception {
        Model model = new Model(1, true, createDummyDimension(), false, false, "");
        Modification modification = new Modification(1, 1, true,
                                                     createDummyDimension(), true, true, "");
        Level modelLevel = createModelLevel(model);
        Level modificationLevel = createModificationLevel(modification);
        ArrayList<Level> hierarchy = new ArrayList<>();
        hierarchy.add(vendorLevel);
        hierarchy.add(modelLevel);
        assertEquals(false, ModelsMatcherHelper.getPublishedOnMarket(hierarchy));

        hierarchy.add(modificationLevel);
        assertEquals(true, ModelsMatcherHelper.getPublishedOnMarket(hierarchy));
    }

    @Test
    public void getMatchType() throws Exception {
        ArrayList<Level> hierarchy = new ArrayList<>();

        hierarchy.add(vendorLevel);
        hierarchy.add(createModelLevel(publishedModel));
        assertEquals(MatchType.MODEL_OK_MATCH, ModelsMatcherHelper.getMatchType(hierarchy));

        hierarchy.remove(1);
        hierarchy.add(createModelLevel(unpublishedModel));
        assertEquals(MatchType.MODEL_TASK_MATCH, ModelsMatcherHelper.getMatchType(hierarchy));

        hierarchy.add(createModificationLevel(publishedModification));
        assertEquals(MatchType.MODIFICATION_MATCH, ModelsMatcherHelper.getMatchType(hierarchy));

        hierarchy.remove(2);
        hierarchy.add(createModificationLevel(unpublishedModification));
        assertEquals(MatchType.MODIFICATION_TASK_MATCH, ModelsMatcherHelper.getMatchType(hierarchy));

        hierarchy.remove(2);
        hierarchy.add(createLevelWithBlockWords());
        assertEquals(MatchType.BLOCK_WORD_MATCH, ModelsMatcherHelper.getMatchType(hierarchy));
    }

    @Test
    public void getMatchTarget() throws Exception {
        ArrayList<Level> hierarchy = new ArrayList<>();
        assertEquals(Matcher.MatchTarget.NOTHING, ModelsMatcherHelper.getMatchTarget(hierarchy));

        hierarchy.add(vendorLevel);
        assertEquals(Matcher.MatchTarget.VENDOR, ModelsMatcherHelper.getMatchTarget(hierarchy));

        hierarchy.add(createModelLevel(publishedModel));
        assertEquals(Matcher.MatchTarget.PUBLISHED_MODEL, ModelsMatcherHelper.getMatchTarget(hierarchy));

        hierarchy.remove(1);
        hierarchy.add(createModelLevel(unpublishedModel));
        assertEquals(Matcher.MatchTarget.UNPUBLISHED_MODEL, ModelsMatcherHelper.getMatchTarget(hierarchy));

        hierarchy.add(createModificationLevel(publishedModification));
        assertEquals(Matcher.MatchTarget.PUBLISHED_MODIFICATION, ModelsMatcherHelper.getMatchTarget(hierarchy));

        hierarchy.remove(2);
        hierarchy.add(createModificationLevel(unpublishedModification));
        assertEquals(Matcher.MatchTarget.UNPUBLISHED_MODIFICATION, ModelsMatcherHelper.getMatchTarget(hierarchy));
    }

    private Dimension createDummyDimension() {
        return new Dimension(1, 1, 1, 1);
    }

    private Level<Model> createModelLevel(Model model) {
        Infix<Model> modelInfix = new Infix<>(null, null, model);
        return new Level<Model>(model.getId(), modelInfix, Infix.EMPTY_ARRAY, model.getName());
    }

    private Level<Modification> createModificationLevel(Modification modification) {
        Infix<Modification> modelInfix = new Infix<>(null, null, modification);
        return new Level<Modification>(modification.getId(), modelInfix, Infix.EMPTY_ARRAY, modification.getName());
    }

    private Level createVendorLevel(int vendorId) {
        return new Level(1, Infix.GOOD_ID_INFIX, Infix.EMPTY_ARRAY, "");
    }

    private Level createLevelWithBlockWords() {
        return new Level(1, Infix.GOOD_ID_INFIX, new Infix[]{Infix.DUMMY_INFIX}, "");
    }
}
