package ru.yandex.ir.modelsclusterizer.utils;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author shurk
 */
public class DescriptionUtilsTest {
    @Test
    public void leaveOnlyTags() {
        assertEquals("|Tag1: bububu             ",
            DescriptionUtils.leaveOnlyTags("|Tag1: bububu|Tag2: bababa", getTagsSet("Tag1")));
        assertEquals("             |Tag2: bababa",
            DescriptionUtils.leaveOnlyTags("|Tag1: bububu|Tag2: bababa", getTagsSet("Tag2")));

        assertEquals("|Tag1: bu||bubu               ",
            DescriptionUtils.leaveOnlyTags("|Tag1: bu||bubu|Tag2: ba||baba", getTagsSet("Tag1")));
        assertEquals("               |Tag2: ba||baba",
            DescriptionUtils.leaveOnlyTags("|Tag1: bu||bubu|Tag2: ba||baba", getTagsSet("Tag2")));

        assertEquals("             |Tag2: bububu             ",
            DescriptionUtils.leaveOnlyTags("|Tag1: bububu|Tag2: bububu|Tag3: bobobo", getTagsSet("Tag2")));
        assertEquals("|Tag1: bububu             |Tag3: bobobo",
            DescriptionUtils.leaveOnlyTags("|Tag1: bububu|Tag2: bububu|Tag3: bobobo", getTagsSet("Tag1", "Tag3")));

        assertEquals("              |Tag 2: bububu              ",
            DescriptionUtils.leaveOnlyTags("|Tag 1: bububu|Tag 2: bububu|Tag 3: bobobo", getTagsSet("Tag 2")));
        assertEquals("|Tag 1: bububu              |Tag 3: bobobo",
            DescriptionUtils.leaveOnlyTags("|Tag 1: bububu|Tag 2: bububu|Tag 3: bobobo", getTagsSet("Tag 1", "Tag 3")));
    }

    private static Set<String> getTagsSet(String... tags) {
        return new ObjectOpenHashSet<>(tags);
    }
}