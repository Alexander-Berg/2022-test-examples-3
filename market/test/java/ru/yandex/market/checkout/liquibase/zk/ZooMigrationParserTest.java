package ru.yandex.market.checkout.liquibase.zk;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class ZooMigrationParserTest {

    @Test
    public void parse() throws Exception {
        ZooMigrationParser zooMigrationParser = new ZooMigrationParser();
        List<ZooMigration> migrations = zooMigrationParser.parse(new ByteArrayResource(
                ("--changeset timursha:MARKETCHECKOUT-1\n" +
                        "create /checkout ''\n" +
                        "\n" +
                        "--changeset timursha:MARKETCHECKOUT-2\n" +
                        "create /checkout/orders ''\n").getBytes()

        ));
        Assertions.assertEquals(2, migrations.size());
        ZooMigration first = migrations.get(0);
        Assertions.assertEquals("timursha", first.getAuthor());
        Assertions.assertEquals("MARKETCHECKOUT-1", first.getId());
        assertThat(first.getScripts(), hasSize(1));
        Assertions.assertEquals("create /checkout ''", first.getScripts().get(0));
        //
        ZooMigration second = migrations.get(1);
        Assertions.assertEquals("timursha", second.getAuthor());
        Assertions.assertEquals("MARKETCHECKOUT-2", second.getId());
        assertThat(second.getScripts(), hasSize(1));
        Assertions.assertEquals("create /checkout/orders ''", second.getScripts().get(0));
    }

    @Test
    public void parse2() throws Exception {
        ZooMigrationParser zooMigrationParser = new ZooMigrationParser();
        List<ZooMigration> migrations = zooMigrationParser.parse(new ByteArrayResource(
                ("--changeset timursha:MARKETCHECKOUT-1\n" +
                        "create /checkout ''\n" +
                        "\n" +
                        "create /checkout/orders ''\n").getBytes()

        ));
        Assertions.assertEquals(1, migrations.size());
        ZooMigration first = migrations.get(0);
        Assertions.assertEquals("timursha", first.getAuthor());
        Assertions.assertEquals("MARKETCHECKOUT-1", first.getId());
        Assertions.assertEquals(2, first.getScripts().size());
        Assertions.assertEquals("create /checkout ''", first.getScripts().get(0));
        Assertions.assertEquals("create /checkout/orders ''", first.getScripts().get(1));
    }
}
