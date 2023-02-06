package ru.yandex.market.mbi.util.db.jdbc;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

public class TStringTblTest {

    @Test
    public void stringArrayToPostgreSQLTextArray() {
        Collection<String> collection = new ArrayList<>();
        collection.add("Hello");
        collection.add(",");
        collection.add(" ");
        collection.add("\"World'");
        collection.add("!");
        TStringTbl tStringTbl = new TStringTbl(collection);
        Assert.assertEquals(tStringTbl.toString(), "{Hello,\\,,\\ ,\\\"World'',!}");
    }
}
