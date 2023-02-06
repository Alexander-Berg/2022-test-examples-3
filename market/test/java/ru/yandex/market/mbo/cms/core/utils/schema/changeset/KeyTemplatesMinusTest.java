package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.KeyTemplate;

public class KeyTemplatesMinusTest {

    @Test
    public void testNullMinusNull() {
        Assert.assertNull(KeyTemplateOperations.keyTemplatesMinus(null, null));
    }

    @Test
    public void testNullMinusKeyTemplates() {
        Assert.assertNull(
                KeyTemplateOperations.keyTemplatesMinus(
                        null,
                        Collections.singletonList(voidKeyTemplate())
                )
        );
    }

    @Test
    public void testKeyTemplatesMinusNull() {
        Assert.assertEquals(
                1,
                KeyTemplateOperations.keyTemplatesMinus(
                        Collections.singletonList(voidKeyTemplate()),
                        null
                ).size()
        );
    }

    @Test
    public void testKeyTemplatesMinusEmpty() {
        Assert.assertEquals(
                1,
                KeyTemplateOperations.keyTemplatesMinus(
                        Collections.singletonList(voidKeyTemplate()),
                        Collections.emptyList()
                ).size()
        );
    }

    @Test
    public void testEmptyMinusKeyTemplates() {
        Assert.assertEquals(
                0,
                KeyTemplateOperations.keyTemplatesMinus(
                        Collections.emptyList(),
                        Collections.singletonList(
                                voidKeyTemplate()
                        )
                ).size()
        );
    }

    @Test
    public void testMinusWithSameKeys() {
        List<String> key = new ArrayList<>();
        key.add("f1");
        key.add("f2");
        key.add("f3");

        KeyTemplate kt1 = new KeyTemplate(key, false, false, "g1");
        KeyTemplate kt2 = new KeyTemplate(key, null, null, null);
        List<KeyTemplate> ktResult = KeyTemplateOperations.keyTemplatesMinus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(0, ktResult.size());

        kt1 = new KeyTemplate(key, true, false, "g1");
        kt2 = new KeyTemplate(key, false, null, null);
        ktResult = KeyTemplateOperations.keyTemplatesMinus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key, ktResult.get(0), null, false, "g1");

        kt1 = new KeyTemplate(key, true, false, "g1");
        kt2 = new KeyTemplate(key, null, false, null);
        ktResult = KeyTemplateOperations.keyTemplatesMinus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key, ktResult.get(0), true, null, "g1");

        kt1 = new KeyTemplate(key, true, false, "g1");
        kt2 = new KeyTemplate(key, null, null, "");
        ktResult = KeyTemplateOperations.keyTemplatesMinus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key, ktResult.get(0), true, false, null);
    }

    @Test
    public void testMinusWithDifferentKeys() {
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
        List<KeyTemplate> ktResult = KeyTemplateOperations.keyTemplatesMinus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key1, ktResult.get(0), false, false, "g1");

        kt1 = new KeyTemplate(key1, false, false, "g1");
        kt2 = new KeyTemplate(key2, null, null, null);
        ktResult = KeyTemplateOperations.keyTemplatesMinus(
                Collections.singletonList(kt1), Collections.singletonList(kt2)
        );

        Assert.assertEquals(1, ktResult.size());
        assertKeyTemplate(key1, ktResult.get(0), false, false, "g1");
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
