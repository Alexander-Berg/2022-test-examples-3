package ru.yandex.yt.yqltestable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 06.09.2021
 */
public class YqlTestableTest {
    @Test
    public void testHelper() {
        String expectedFull = "\n" +
            "-- procedures to keep yql code testable\n" +
            "-- they are replaced in tests with mocks\n" +
            "\n" +
            "-- allows to mock table calls\n" +
            "DEFINE SUBQUERY $_table($tablename) as\n" +
            "    SELECT * FROM $tablename;\n" +
            "END DEFINE;\n" +
            "\n" +
            "$_dir = ($path) -> { return $path; };\n" +
            "\n" +
            "\n" +
            "$yql = (\n" +
            "    select *\n" +
            "    from $_table(\"//my_simple/table\")\n" +
            "    join range($_dir(\"//my_custom/dir\")) --_IMPORT_ -- HACK for faster tests\n" +
            ");\n" +
            "\n" +
            "\n" +
            "select * from $yql;";
        Assertions.assertEquals(expectedFull,
            YqlTestable.withAll(YqlTestable.readFile("/sample_yql.sql"), "yql"));
    }

    @Test
    public void testHelperHeader() {
        String expectedFull = "\n" +
            "-- procedures to keep yql code testable\n" +
            "-- they are replaced in tests with mocks\n" +
            "\n" +
            "-- allows to mock table calls\n" +
            "DEFINE SUBQUERY $_table($tablename) as\n" +
            "    SELECT * FROM $tablename;\n" +
            "END DEFINE;\n" +
            "\n" +
            "$_dir = ($path) -> { return $path; };\n" +
            "\n" +
            "\n" +
            "select * from nothing";
        Assertions.assertEquals(expectedFull,
            YqlTestable.withHeader("select * from nothing"));
    }

    @Test
    public void testHelperSelect() {
        String expectedFull = "$yql = (select * from nothing);\n" +
            "\n" +
            "select * from $yql;";
        Assertions.assertEquals(expectedFull,
            YqlTestable.withSelect("$yql = (select * from nothing);", "yql"));
    }
}
