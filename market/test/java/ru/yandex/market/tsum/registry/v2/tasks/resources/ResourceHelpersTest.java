package ru.yandex.market.tsum.registry.v2.tasks.resources;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Ignore
public class ResourceHelpersTest {

    @Test
    public void getIssueComment() {
        String expected = "Найдены не привязанные ресурсы yt-account:\n" +
            "* three@some\n" +
            "* one\n" +
            "* two\n" +
            "\n" +
            "а так же привязанные, но более не существующие:\n" +
            "* three@some\n" +
            "* one\n" +
            "\n" +
            "Пожалуйста, актуализируйте список ресурсов сервисов в ((https://tsum.yandex-team.ru/registry реестре компонентов))\n";
        Set<String> unlinkedResources = new HashSet<>(Arrays.asList("one", "two", "three@some"));
        Set<String> deletedResources = new HashSet<>(Arrays.asList("one", "three@some"));
        String issue = ResourceHelpers.getIssueComment(unlinkedResources, deletedResources, "yt-account");
        assertEquals(issue, expected);
    }
}
