package ru.yandex.autotests.innerpochta.utils;

import org.apache.commons.beanutils.Converter;

import org.yaml.snakeyaml.Yaml;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 22.05.13
 * Time: 18:31
 */
public class AccountsConverter implements Converter {

    @Override
    public Object convert(Class aClass, Object o) {
        if (!(o instanceof String)) {
            return new HashMap<String, List<Map<String, String>>>();
        }
        String path = (String) o;
        return new Yaml().load(this.getClass().getClassLoader().getResourceAsStream(path));
    }
}
