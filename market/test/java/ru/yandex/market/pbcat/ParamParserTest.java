package ru.yandex.market.pbcat;

import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Тесты проверяют корректность парсинга входных аргументов.
 *
 * @author s-ermakov
 */
public class ParamParserTest {

    private static final String INPUT_FILE = "fakeFile.txt";

    ArgsParser argsParser;

    @Before
    public void setUp() throws Exception {
        argsParser = new ArgsParser();
    }

    @Test(expected = ParseException.class)
    public void emptyParams() throws Exception {
        String[] args = new String[0];

        ArgsParser.Options options = argsParser.parseCommandLineArguments(args);
    }

    @Test
    public void firstArgIsPath() throws Exception {
        String[] args = new String[] {
                INPUT_FILE
        };

        ArgsParser.Options options = argsParser.parseCommandLineArguments(args);

        Assert.assertEquals(INPUT_FILE, options.getInputFilePath());
    }

    @Test
    public void inputFileParam() throws Exception {
        String[] args = new String[] {
                "-I", INPUT_FILE
        };

        ArgsParser.Options options = argsParser.parseCommandLineArguments(args);

        Assert.assertEquals(INPUT_FILE, options.getInputFilePath());
    }
}
