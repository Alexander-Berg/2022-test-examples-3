package ru.yandex.market.mbo.db.modelstorage.listeners;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.catalogue.model.UpdateAttributesEventParams;
import ru.yandex.market.mbo.db.modelstorage.GoodIdSaveService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author eugenezag
 */
@RunWith(MockitoJUnitRunner.class)
public class GoodListChangeListenerTest {

    @Mock
    private GoodIdSaveService goodIdSaveService;

    GoodListChangeListener listener;

    @Before
    public void before() throws Exception {
        listener = new GoodListChangeListener();
        listener.setGoodIdSaveService(goodIdSaveService);
    }

    @Test
    public void updateGoodIdListTest() {
        ParameterValues pvBefore = new ParameterValues(1,
            "good_id",
            Param.Type.STRING,
            new Word(1, "11111111111111111111111111111111"));

        ParameterValues pvAfter = new ParameterValues(pvBefore);
        pvAfter.getSingle().setStringValue(Arrays.asList(new Word(1, "22222222222222222222222222222222")));

        CommonModel modelBefore = new CommonModel();
        modelBefore.putParameterValues(pvBefore);

        CommonModel modelAfter = new CommonModel();
        modelBefore.putParameterValues(pvAfter);

        Map<String, List<String>> before = new HashMap<>();
        before.put("good_id", Arrays.asList("11111111111111111111111111111111"));

        Map<String, List<String>> after = new HashMap<>();
        after.put("good_id", Arrays.asList("22222222222222222222222222222222"));

        listener.handleEvent(
            new UpdateAttributesEventParams(new ModelChanges(modelBefore, modelAfter), -1, null,
                before, after));
    }
}
