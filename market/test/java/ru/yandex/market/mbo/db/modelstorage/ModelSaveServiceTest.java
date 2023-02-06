package ru.yandex.market.mbo.db.modelstorage;


import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("checkstyle:MagicNumber")
public class ModelSaveServiceTest {
    ModelSaveService modelSaveService = new ModelSaveService();

    @Test
    public void buildParamValueCombination() {
        List<ParameterValue> l1 = ImmutableList.of(new ParameterValue(2, "first", Param.Type.ENUM),
            new ParameterValue(3,
                "first", Param.Type.ENUM));
        List<ParameterValue> l2 = ImmutableList.of(new ParameterValue(4, "second", Param.Type.ENUM),
            new ParameterValue(1,
                "second", Param.Type.ENUM),
            new ParameterValue(6, "second", Param.Type.ENUM));
        List<ParameterValue> l3 = ImmutableList.of(new ParameterValue(8, "third", Param.Type.ENUM),
            new ParameterValue(9,
                "third", Param.Type.ENUM));
        List<List<ParameterValue>> values = ImmutableList.of(l1, l2, l3);
        List<List<ParameterValue>> result = new ArrayList<>();
        modelSaveService.buildParamValueCombination(values, result);

        for (List<ParameterValue> v : result) {
            System.out.println();
            for (ParameterValue s : v) {
                System.out.print(s.getParamId());
                System.out.print(" ");
            }
        }
        /* корректный ввывод
2 4 8
2 4 9
2 1 8
2 1 9
2 6 8
2 6 9
3 4 8
3 4 9
3 1 8
3 1 9
3 6 8
3 6 9
        * */
    }
}
