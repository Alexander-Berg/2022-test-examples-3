package ru.yandex.market.mbo.dump.falschspieler.model;

import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.util.ProtoUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author commince
 * @date 08.05.2018
 */
public class ParamOptionIndexTest {

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void searchOptionWithAdditionalAliases() throws Exception {
        List<MboParameters.Option> options = Arrays.asList(
                ProtoUtils.option(1L, 11L, "Вендор лучший один", "Alias"),
                ProtoUtils.option(2L, 12L, "Вендор плохой", "Alias Вендор"),
                ProtoUtils.option(3L, 13L, "Вендор лучший", "Alias Alias")
        );

        Map<Long, List<MboParameters.Word>> additionalAliases = new HashMap<>();
        additionalAliases.put(1L, ProtoUtils.words("Глобальный вендор-алиас один"));
        additionalAliases.put(2L, ProtoUtils.words("Глобальный вендор-алиас"));
        additionalAliases.put(3L, ProtoUtils.words("Глобальный вендор-алиас три"));

        ParamOptionIndex optionIndex = new ParamOptionIndex(options, additionalAliases);

        //Поиск по имени
        MboParameters.Option o = optionIndex.search("Вендор лучший");
        assertEquals(3L, o.getId());

        //Поиск по алиасу
        o = optionIndex.search("Alias Вендор");
        assertEquals(2L, o.getId());

        o = optionIndex.search("Alias");
        assertEquals(1L, o.getId());

        //Поиск по алиасу глобального вендора
        o = optionIndex.search("Глобальный вендор-алиас");
        assertEquals(2L, o.getId());

        o = optionIndex.search("Глобальный вендор-алиас три");
        assertEquals(3L, o.getId());

        //Ищем несуществующего вендора
        o = optionIndex.search("Absent");
        assertNull(o);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void searchSimpleOption() throws Exception {

        List<MboParameters.Option> options = Arrays.asList(
                ProtoUtils.option(1L, "Опция один", "Alias"),
                ProtoUtils.option(2L, "Опция два", "Alias 2"),
                ProtoUtils.option(3L, "Опция", "Alias Three")
        );
        ParamOptionIndex paramOptionIndex = new ParamOptionIndex(options);

        //Поиск по имени
        MboParameters.Option o = paramOptionIndex.search("Опция");
        assertEquals(3L, o.getId());

        o = paramOptionIndex.search("Опция один");
        assertEquals(1L, o.getId());

        //Поиск по алиасу
        o = paramOptionIndex.search("Alias 2");
        assertEquals(2L, o.getId());

        o = paramOptionIndex.search("Alias");
        assertEquals(1L, o.getId());

        //Ищем несуществующую опцию
        o = paramOptionIndex.search("Absent");
        assertNull(o);
    }
}
