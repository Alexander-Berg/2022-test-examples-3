package ru.yandex.market.logshatter.parser.checkout;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import static org.junit.Assert.*;

/*
[29/Dec/2016:06:28:09 +0300]	1482982089	127.0.0.1	GET	/ping	200	8	1482982089502/f2cdc3489ef7de340a17b10369052fdf	STORAGE	0
[29/Dec/2016:06:28:16 +0300]	1482982096	82.199.118.59	GET	/orders/by-shop/172281	200	84	1482982096613/dbbe48f7c190722e0aef7de41839fb2c	STORAGE	81
[29/Dec/2016:06:28:16 +0300]	1482982096	82.199.118.59	GET	/orders/172281/payments.json/	200	84	1482982096613/dbbe48f7c190722e0aef7de41839fb2c	STORAGE	81
 */

public class CheckoutQueryProfilerParserTest {
    private LogParserChecker logParserChecker;

    @Before
    public void setUp() throws Exception {
        logParserChecker = new LogParserChecker(new CheckoutQueryProfilerParser());
    }

    @Test
    public void testParse() throws Exception {
        logParserChecker.check(
            "[29/Dec/2016:06:28:09 +0300]\t1482982089\t127.0.0.1\tGET\t/ping\t200\t8\t1482982089502/f2cdc3489ef7de340a17b10369052fdf\tSTORAGE\t0",
            1482982089,
                "127.0.0.1",
                logParserChecker.getHost(),
                "GET",
                "/ping",
                200,
                8,
                "1482982089502/f2cdc3489ef7de340a17b10369052fdf",
                "STORAGE",
                0
        );
    }

    @Test
    public void testParse2() throws Exception {
        logParserChecker.check(
                "[29/Dec/2016:06:28:16 +0300]\t1482982096\t82.199.118.59\tGET\t/orders/by-shop/172281\t200\t84\t1482982096613/dbbe48f7c190722e0aef7de41839fb2c\tSTORAGE\t81",
                1482982096,
                "82.199.118.59",
                logParserChecker.getHost(),
                "GET",
                "/orders/by-shop",
                200,
                84,
                "1482982096613/dbbe48f7c190722e0aef7de41839fb2c",
                "STORAGE",
                81
        );
    }

    @Test
    public void testParse3() throws Exception {
        logParserChecker.check(
                "[29/Dec/2016:06:28:16 +0300]\t1482982096\t82.199.118.59\tGET\t/orders/172281/payments.json/\t200\t84\t1482982096613/dbbe48f7c190722e0aef7de41839fb2c\tSTORAGE\t81",
                1482982096,
                "82.199.118.59",
                logParserChecker.getHost(),
                "GET",
                "/orders/payments",
                200,
                84,
                "1482982096613/dbbe48f7c190722e0aef7de41839fb2c",
                "STORAGE",
                81
        );
    }

}