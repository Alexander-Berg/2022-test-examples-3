package ru.yandex.chemodan.app.smartcache.worker.tests;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.smartcache.worker.dataapi.LocalizedStringDictionary;
import ru.yandex.inside.utils.Language;

/**
 * @author osidorkin
 */
public class TestUtils {

    public static LocalizedStringDictionary createLocalizedStringDictionary(String enValue) {
        LocalizedStringDictionary partial = new LocalizedStringDictionary(
                Tuple2List.tuple2List(Cf.list(Tuple2.tuple(Language.ENGLISH, enValue))));
        return new LocalizedStringDictionary(partial.toList());
    }


}
