package ru.yandex.direct.i18n;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.i18n.dict.Dictionary;
import ru.yandex.direct.i18n.dict.Entry2Form;
import ru.yandex.direct.i18n.dict.Entry3Form;
import ru.yandex.direct.i18n.dict.PluralEntry2Form;
import ru.yandex.direct.i18n.dict.PluralEntry3Form;
import ru.yandex.direct.i18n.dict.SingularEntry;

public class DictionaryTest {
    @Test
    public void test2Form() throws IOException {
        Dictionary<Entry2Form> srcDictionary = new Dictionary<>();
        srcDictionary.put("squire", new SingularEntry("squire"));
        srcDictionary.put("warrior", new PluralEntry2Form("not warrior", "warriors"));

        byte[] json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(srcDictionary);

        Dictionary<Entry2Form> dictionary = Dictionary.fromInputStream(new ByteArrayInputStream(json));

        PluralEntry2Form warrior = (PluralEntry2Form) dictionary.get("warrior");
        Assert.assertEquals("not warrior", warrior.getOne());
        Assert.assertEquals("warriors", warrior.getMany());

        SingularEntry squire = (SingularEntry) dictionary.get("squire");
        Assert.assertEquals("squire", squire.getForm());
    }

    @Test
    public void test3Form() throws IOException {
        Dictionary<Entry3Form> srcDictionary = new Dictionary<>();
        srcDictionary.put("squire", new SingularEntry("squire"));
        srcDictionary.put("warrior", new PluralEntry3Form("воин", "воина", "воинов"));

        byte[] json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(srcDictionary);

        Dictionary<Entry3Form> dictionary = Dictionary.fromInputStream(new ByteArrayInputStream(json));

        PluralEntry3Form warrior = (PluralEntry3Form) dictionary.get("warrior");
        Assert.assertEquals("воин", warrior.getOne());
        Assert.assertEquals("воина", warrior.getSome());
        Assert.assertEquals("воинов", warrior.getMany());

        SingularEntry squire = (SingularEntry) dictionary.get("squire");
        Assert.assertEquals("squire", squire.getForm());
    }
}
