package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import org.reflections.Reflections;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by entarrion on 22.09.16.
 */
public class Dictionaries {

    private static List<Class<? extends DictionaryRecord>> DICTIONARY_RECORDS = new ArrayList<>();
    private static Map<Class<? extends DictionaryRecord>, DictType> dictTypes = new HashMap<>();

    public static List<Class<? extends DictionaryRecord>> allDictionariesClasses() {
        if (DICTIONARY_RECORDS.isEmpty()) {
            loadDictionaryClasses();
        }
        return DICTIONARY_RECORDS;
    }

    private static void loadDictionaryClasses() {
        Reflections reflections = new Reflections("ru.yandex.autotests.market.stat.dictionaries_yt.beans.records");
        Set<Class<? extends DictionaryRecord>> allClasses = reflections.getSubTypesOf(DictionaryRecord.class);
        allClasses.stream().filter(DictionaryRecord.class::isAssignableFrom)
            .filter(it -> !DictionaryRecord.class.equals(it))
            .forEach(clazz -> DICTIONARY_RECORDS.add(clazz));
    }

    private static void loadDictTypes() {
        if (DICTIONARY_RECORDS.isEmpty()) {
            loadDictionaryClasses();
        }
        Attacher.attach("Found classes!", DICTIONARY_RECORDS);
        DICTIONARY_RECORDS.forEach(classs -> dictTypes.put(classs, new DictType<>(classs)));
        Attacher.attach("Found dictionaries!", dictTypes);
    }

    private static List<DictType> getAll() {
        if (dictTypes.isEmpty()) {
            loadDictTypes();
        }
        return new ArrayList<>(dictTypes.values());
    }

    public static List<DictType> ytDicts() {
        return getAll();
    }

}
