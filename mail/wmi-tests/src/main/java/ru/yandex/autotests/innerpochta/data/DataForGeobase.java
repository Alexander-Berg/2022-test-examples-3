package ru.yandex.autotests.innerpochta.data;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.05.14
 * Time: 17:31
 */
public class DataForGeobase {

    //языки поддерживаемые геобазой
    public static List<String> langs() {
        return newArrayList("ru", "uk", "be", "kk", "en", "tr", "tt");
        //DARIA-45616 язык cs перестали поддерживать
    }

    public static List<String> ips() {
        return newArrayList(
                //Пермь
                "195.161.200.208",
                //Джакарта
                "222.165.202.208",
                //Москва
                "77.88.2.36"
        );
    }
}
