package ru.yandex.market.tsum.analytics;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 17.01.18
 */
public class CodeMetricsServiceTest {
    @Test
    public void extractsCommands() {
        List<Comment> comments = Arrays.asList(
            new Comment().setBody("Ola /busy").setCreatedAt(new Date()),
            new Comment().setBody("норм ваще круто").setCreatedAt(new Date()),
            new Comment().setBody("q норм ваще круто").setCreatedAt(new Date()),
            new Comment().setBody(":fire: /ok").setCreatedAt(new Date())
        );

        List<GithubMetricsCalculator.Command> commands = GithubMetricsCalculator.getCommands(comments);
        Assert.assertEquals(4, commands.size());
        Assert.assertEquals("/busy", commands.get(0).getValue());
        Assert.assertEquals("норм ваще", commands.get(1).getValue());
        Assert.assertEquals(":fire:", commands.get(2).getValue());
        Assert.assertEquals("/ok", commands.get(3).getValue());
    }
}
