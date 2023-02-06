package ru.yandex.market.supercontroller.mbologs.cli;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author amaslak
 */
public class CommandLineParserTest {

    private static final String YT = "YT";
    private static final String TEST_PATH = "//tmp/mbo_offers_20121011_1000";
    private static final String TEST_TABLE = "mbo_offers_20121011_1000";
    private static final String BASE_SESSION = "20121010_1010";
    private static final String TABLE_SESSION = "20361010_1010";

    @Test
    public void testDestinationArg() throws Exception {
        CommandLine cli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--baseSession", BASE_SESSION,
            "--sessions", ":20121010_1011:20121010_1010",
            "--irregular"
        });
        Assert.assertTrue(cli.hasOption(CommandLineParser.DESTINATION));

        String value = cli.getOptionValue(CommandLineParser.DESTINATION);
        Assert.assertEquals(YT, value);

        CommandLineParser parser = new CommandLineParser(cli);
        Assert.assertEquals(DestinationType.YT, parser.getDestination());
    }

    @Test(expected = MissingOptionException.class)
    public void testDestinationMissingArg() throws Exception {
        CommandLineParser.parseArgs(new String[] {
            "--baseSession", BASE_SESSION,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--sessions", ":20121010_1010:20121010_1010",
            "--irregular"
        });
    }

    @Test
    public void testBaseSessionArg() throws Exception {
        CommandLine cli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--baseSession", BASE_SESSION,
            "--sessions", ":20121010_1010:20121010_1010",
            "--irregular", ":20121010_1010:20121010_1010",
        });
        Assert.assertTrue(cli.hasOption(CommandLineParser.BASE_SESSION));

        String value = cli.getOptionValue(CommandLineParser.BASE_SESSION);
        Assert.assertEquals(BASE_SESSION, value);

        CommandLineParser parser = new CommandLineParser(cli);
        Assert.assertEquals(BASE_SESSION, parser.getBaseSession());
    }

    @Test
    public void testTableSessionArg() throws Exception {
        CommandLine cli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--tableSession", TABLE_SESSION,
            "--sessions", ":20121010_1010:20121010_1010",
            "--irregular", ":20121010_1010:20121010_1010",
        });
        Assert.assertTrue(cli.hasOption(CommandLineParser.TABLE_SESSION));

        String value = cli.getOptionValue(CommandLineParser.TABLE_SESSION);
        Assert.assertEquals(TABLE_SESSION, value);

        CommandLineParser parser = new CommandLineParser(cli);
        Assert.assertEquals(TABLE_SESSION, parser.getTableSession());
    }

    @Test
    public void testTablePathArg() throws Exception {
        CommandLine cli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--baseSession", BASE_SESSION,
            "--sessions", ":20121010_1010:20121010_1010",
            "--irregular", ":20121010_1010:20121010_1010",
        });
        Assert.assertTrue(cli.hasOption(CommandLineParser.TABLE_PATH));

        String value = cli.getOptionValue(CommandLineParser.TABLE_PATH);
        Assert.assertEquals(TEST_PATH, value);

        CommandLineParser parser = new CommandLineParser(cli);
        Assert.assertEquals(TEST_PATH, parser.getTablePath());
    }

    @Test
    public void testTableNameArg() throws Exception {
        CommandLine cli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--tableName", TEST_TABLE,
            "--baseSession", BASE_SESSION,
            "--sessions", ":20121010_1010:20121010_1010",
            "--irregular", ":20121010_1010:20121010_1010",
        });
        Assert.assertTrue(cli.hasOption(CommandLineParser.TABLE_NAME));

        String value = cli.getOptionValue(CommandLineParser.TABLE_NAME);
        Assert.assertEquals(TEST_TABLE, value);

        CommandLineParser parser = new CommandLineParser(cli);
        Assert.assertEquals(TEST_TABLE, parser.getTableName());
    }

    @Test(expected = MissingOptionException.class)
    public void testTablePathMissingArg() throws Exception {
        CommandLineParser.parseArgs(new String[] {
            "--baseSession", BASE_SESSION,
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--sessions", ":20121010_1010:20121010_1010",
            "--irregular"
        });
    }

    @Test
    public void testSessionsArg() throws Exception {
        CommandLine cli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--baseSession", BASE_SESSION,
            "--sessions", ":20121010_1010:20121010_1012",
        });

        Assert.assertTrue(cli.hasOption(CommandLineParser.SESSIONS));

        String[] sessions = cli.getOptionValues(CommandLineParser.SESSIONS);
        String[] expected = new String[] {"", BASE_SESSION, "20121010_1012"};
        Assert.assertArrayEquals(expected, sessions);

        CommandLineParser parser = new CommandLineParser(cli);
        Assert.assertEquals(ImmutableSet.of(BASE_SESSION, "20121010_1012"), parser.getSessions());
    }

    @Test
    public void testIrregularArg() throws Exception {
        CommandLine irregularCli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH,
            "--baseSession", BASE_SESSION,
            "--irregular"
        });
        Assert.assertTrue(irregularCli.hasOption(CommandLineParser.IRREGULAR));

        CommandLineParser parser = new CommandLineParser(irregularCli);
        Assert.assertTrue(parser.isIrregular());

        CommandLine cli = CommandLineParser.parseArgs(new String[] {
            "--destination", YT,
            "--baseSession", BASE_SESSION,
            "--dashboardTableName", TEST_TABLE,
            "--tablePath", TEST_PATH
        });
        Assert.assertFalse(cli.hasOption(CommandLineParser.IRREGULAR));

        CommandLineParser irregularParser = new CommandLineParser(cli);
        Assert.assertFalse(irregularParser.isIrregular());
    }
}
