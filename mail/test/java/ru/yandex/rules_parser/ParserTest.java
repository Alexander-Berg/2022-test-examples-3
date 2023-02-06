package ru.yandex.rules_parser;

import org.junit.Test;
import ru.yandex.expr_parser.QueryParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.List;

public class ParserTest {
    @Test
    public void test() throws Exception {

        QueryParser.main("!3 || !(4) && 2 && ( 1 || RegexMatch('some rwr') )");
//        final List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath("/home/luckybug/dev/rules/ban_temp.rul"), Charset.forName("koi8-r"));
//
//        new Parser().parse(lines);
    }
}
