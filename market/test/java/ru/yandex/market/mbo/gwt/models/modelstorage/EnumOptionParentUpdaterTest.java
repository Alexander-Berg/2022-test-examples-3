package ru.yandex.market.mbo.gwt.models.modelstorage;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.db.params.EnumOptionParentUpdater;

/**
 * @author verekonn
 * @date 09.08.21
 */
public class EnumOptionParentUpdaterTest {

    @SuppressWarnings("MagicNumber")
    @Test
    public void testIdealTree() {
        EnumOptionParentUpdater updater = new EnumOptionParentUpdater();
        List<EnumOptionParentUpdater.EnumOption> optionsFix =
            updater.getFixedEnums(getTestCategories(),
            getTestParameters(),
            getTestEnumOptions());
        Assert.assertEquals(0, optionsFix.size());
    }

    @SuppressWarnings("MagicNumber")
    @Test
    public void testFail1() {
        EnumOptionParentUpdater updater = new EnumOptionParentUpdater();
        List<EnumOptionParentUpdater.EnumOption> broken =
            getTestEnumOptions();
        broken.remove(4);
        broken.add(4, new EnumOptionParentUpdater.EnumOption(5L, 1L, 5L));
        List<EnumOptionParentUpdater.EnumOption> optionsFix =
            updater.getFixedEnums(getTestCategories(),
                getTestParameters(),
                broken);
        Assert.assertEquals(1, optionsFix.size());
        Assert.assertEquals(2L, (long) optionsFix.get(0).getNewParentId());
    }

    @SuppressWarnings("MagicNumber")
    @Test
    public void testFail2() {
        EnumOptionParentUpdater updater = new EnumOptionParentUpdater();
        List<EnumOptionParentUpdater.EnumOption> broken =
            getTestEnumOptions();
        broken.remove(4);
        broken.add(4, new EnumOptionParentUpdater.EnumOption(5L, 1L, 5L));
        broken.remove(7);
        broken.add(7, new EnumOptionParentUpdater.EnumOption(8L, 1L, 8L));
        List<EnumOptionParentUpdater.EnumOption> optionsFix =
            updater.getFixedEnums(getTestCategories(),
                getTestParameters(),
                broken);
        Assert.assertEquals(2, optionsFix.size());
        Assert.assertEquals(2L, (long) optionsFix.get(0).getNewParentId());
        Assert.assertEquals(4L, (long) optionsFix.get(1).getNewParentId());
    }

    //              1
    //      2               3
    //   4     5         6     7
    //   8
    @SuppressWarnings("MagicNumber")
    private List<EnumOptionParentUpdater.Category> getTestCategories() {
        List<EnumOptionParentUpdater.Category> categories = new ArrayList<>();
        categories.add(new EnumOptionParentUpdater.Category(1L, null));
        categories.add(new EnumOptionParentUpdater.Category(2L, 1L));
        categories.add(new EnumOptionParentUpdater.Category(3L, 1L));
        categories.add(new EnumOptionParentUpdater.Category(4L, 2L));
        categories.add(new EnumOptionParentUpdater.Category(5L, 2L));
        categories.add(new EnumOptionParentUpdater.Category(6L, 3L));
        categories.add(new EnumOptionParentUpdater.Category(7L, 3L));
        categories.add(new EnumOptionParentUpdater.Category(8L, 4L));
        return categories;
    }

    @SuppressWarnings("MagicNumber")
    private List<EnumOptionParentUpdater.Parameter> getTestParameters() {
        List<EnumOptionParentUpdater.Parameter> parameters = new ArrayList<>();
        parameters.add(new EnumOptionParentUpdater.Parameter(1L, 1L, null));
        parameters.add(new EnumOptionParentUpdater.Parameter(2L, 2L, 1L));
        parameters.add(new EnumOptionParentUpdater.Parameter(3L, 3L, 1L));
        parameters.add(new EnumOptionParentUpdater.Parameter(4L, 4L, 1L));
        parameters.add(new EnumOptionParentUpdater.Parameter(5L, 5L, 1L));
        parameters.add(new EnumOptionParentUpdater.Parameter(6L, 6L, 1L));
        parameters.add(new EnumOptionParentUpdater.Parameter(7L, 7L, 1L));
        parameters.add(new EnumOptionParentUpdater.Parameter(8L, 8L, 1L));
        return parameters;
    }

    @SuppressWarnings("MagicNumber")
    private List<EnumOptionParentUpdater.EnumOption> getTestEnumOptions() {
        List<EnumOptionParentUpdater.EnumOption> enums = new ArrayList<>();
        enums.add(new EnumOptionParentUpdater.EnumOption(1L, null, 1L));
        enums.add(new EnumOptionParentUpdater.EnumOption(2L, 1L, 2L));
        enums.add(new EnumOptionParentUpdater.EnumOption(3L, 1L, 3L));
        enums.add(new EnumOptionParentUpdater.EnumOption(4L, 2L, 4L));
        enums.add(new EnumOptionParentUpdater.EnumOption(5L, 2L, 5L));
        enums.add(new EnumOptionParentUpdater.EnumOption(6L, 3L, 6L));
        enums.add(new EnumOptionParentUpdater.EnumOption(7L, 3L, 7L));
        enums.add(new EnumOptionParentUpdater.EnumOption(8L, 4L, 8L));
        return enums;
    }
}
