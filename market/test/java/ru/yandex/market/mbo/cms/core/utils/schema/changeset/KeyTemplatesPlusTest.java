package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.KeyTemplate;

public class KeyTemplatesPlusTest {

    @Test
    public void testNullPlusNull() {
        Assert.assertNull(KeyTemplateOperations.keyTemplatesPlus(null, null));
    }

    @Test
    public void testNullPlusKeyTemplates() {
        Assert.assertEquals(
                1,
                KeyTemplateOperations.keyTemplatesPlus(null, Collections.singletonList(voidKeyTemplate())).size()
        );
    }

    @Test
    public void testKeyTemplatesPlusNull() {
        Assert.assertEquals(
                1,
                KeyTemplateOperations.keyTemplatesPlus(Collections.singletonList(voidKeyTemplate()), null).size()
        );
    }

    @Test
    public void testKeyTemplatesPlusEmpty() {
        Assert.assertEquals(
                1,
                KeyTemplateOperations.keyTemplatesPlus(
                        Collections.singletonList(
                                voidKeyTemplate()
                        ),
                        Collections.emptyList()
                ).size()
        );
    }

    @Test
    public void testEmptyPlusKeyTemplates() {
        Assert.assertEquals(
                1,
                KeyTemplateOperations.keyTemplatesPlus(
                        Collections.emptyList(),
                        Collections.singletonList(
                                voidKeyTemplate()
                        )
                ).size()
        );
    }

    @Test
    public void testPlusWithSameKeys() {
        List<String> key = new ArrayList<>();
        key.add("f1");
        key.add("f2");
        key.add("f3");

        KeyTemplate kt1 = new KeyTemplate(key, false, false, "g1");
        KeyTemplate kt2 = new KeyTemplate(key, true, true, "g2");
        List<KeyTemplate> ktResult = KeyTemplateOperations.keyTemplatesPlus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key, ktResult.get(0), true, true, "g2");

        kt1 = new KeyTemplate(key, null, null, null);
        kt2 = new KeyTemplate(key, false, false, "g1");
        ktResult = KeyTemplateOperations.keyTemplatesPlus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key, ktResult.get(0), false, false, "g1");

        kt1 = new KeyTemplate(key, false, true, "g1");
        kt2 = new KeyTemplate(key, null, null, null);
        ktResult = KeyTemplateOperations.keyTemplatesPlus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key, ktResult.get(0), false, true, "g1");

        kt1 = new KeyTemplate(key, null, null, null);
        kt2 = new KeyTemplate(key, null, null, null);
        ktResult = KeyTemplateOperations.keyTemplatesPlus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key, ktResult.get(0), null, null, null);
    }

    @Test
    public void testPlusWithDifferentKeys() {
        List<String> key1 = new ArrayList<>();
        key1.add("f1");
        key1.add("f2");
        key1.add("f3");

        List<String> key2 = new ArrayList<>();
        key2.add("f2");
        key2.add("f3");
        key2.add("f4");

        KeyTemplate kt1 = new KeyTemplate(key1, false, false, "g1");
        KeyTemplate kt2 = new KeyTemplate(key2, true, true, "g2");
        List<KeyTemplate> ktResult = KeyTemplateOperations.keyTemplatesPlus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(2, ktResult.size());
        assertKeyTemplate(key1, ktResult.get(0), false, false, "g1");
        assertKeyTemplate(key2, ktResult.get(1), true, true, "g2");
    }

    @Test
    public void testPlusWithDifferentKeysAndOrder() {
        List<String> key1 = new ArrayList<>();
        key1.add("f1");
        key1.add("f2");
        key1.add("f3");

        List<String> key2 = new ArrayList<>();
        key2.add("f2");
        key2.add("f3");
        key2.add("f4");

        KeyTemplate kt11 = new KeyTemplate(key1, false, false, "g1");
        KeyTemplate kt12 = new KeyTemplate(key2, false, false, "g1");
        KeyTemplate kt2 = new KeyTemplate(key2, true, true, "g2");
        List<KeyTemplate> ktl1 = new ArrayList<>();
        ktl1.add(kt11);
        ktl1.add(kt12);
        List<KeyTemplate> ktResult = KeyTemplateOperations.keyTemplatesPlus(
                ktl1, Collections.singletonList(kt2)
        );

        Assert.assertEquals(2, ktResult.size());
        assertKeyTemplate(key1, ktResult.get(0), false, false, "g1");
        assertKeyTemplate(key2, ktResult.get(1), true, true, "g2");

        kt11 = new KeyTemplate(key2, false, false, "g2");
        kt12 = new KeyTemplate(key1, false, false, "g1");
        kt2 = new KeyTemplate(key2, null, null, null);
        ktl1 = new ArrayList<>();
        ktl1.add(kt11);
        ktl1.add(kt12);
        ktResult = KeyTemplateOperations.keyTemplatesPlus(
                ktl1, Collections.singletonList(kt2)
        );

        Assert.assertEquals(2, ktResult.size());
        assertKeyTemplate(key2, ktResult.get(0), false, false, "g2");
        assertKeyTemplate(key1, ktResult.get(1), false, false, "g1");

        KeyTemplate kt1 = new KeyTemplate(key1, false, false, "g1");
        KeyTemplate kt21 = new KeyTemplate(key1, null, true, null);
        KeyTemplate kt22 = new KeyTemplate(key2, false, null, "g2");
        List<KeyTemplate> ktl2 = new ArrayList<>();
        ktl2.add(kt21);
        ktl2.add(kt22);
        ktResult = KeyTemplateOperations.keyTemplatesPlus(
                Collections.singletonList(kt1), ktl2
        );

        Assert.assertEquals(2, ktResult.size());
        assertKeyTemplate(key1, ktResult.get(0), false, true, "g1");
        assertKeyTemplate(key2, ktResult.get(1), false, null, "g2");

        kt1 = new KeyTemplate(key1, true, true, "g1");
        kt21 = new KeyTemplate(key2, null, true, null);
        kt22 = new KeyTemplate(key1, false, null, "g2");
        ktl2 = new ArrayList<>();
        ktl2.add(kt21);
        ktl2.add(kt22);
        ktResult = KeyTemplateOperations.keyTemplatesPlus(
                Collections.singletonList(kt1), ktl2
        );

        Assert.assertEquals(2, ktResult.size());
        assertKeyTemplate(key2, ktResult.get(0), null, true, null);
        assertKeyTemplate(key1, ktResult.get(1), false, true, "g2");
    }

    private KeyTemplate voidKeyTemplate() {
        return new KeyTemplate(Collections.emptyList(), null, null, null);
    }

    private void assertKeyTemplate(
            List<String> key, KeyTemplate kt, Boolean uniq, Boolean required, String requiredGroup
    ) {
        Assert.assertEquals(key, kt.getTemplate());
        Assert.assertEquals(uniq, kt.getUniq());
        Assert.assertEquals(uniq != null, kt.isUniqPresented());
        Assert.assertEquals(required, kt.getRequired());
        Assert.assertEquals(required != null, kt.isRequiredPresented());
        Assert.assertEquals(requiredGroup, kt.getRequiredGroup());

    }
}
