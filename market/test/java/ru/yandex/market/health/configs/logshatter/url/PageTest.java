package ru.yandex.market.health.configs.logshatter.url;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class PageTest {

    @Test
    public void parsePages() throws Exception {
        String pageLines = "GET_make_page_matcher_better\tGET:/oh/yes.xhtml\tdo_it_do_it\n\n\n\n\n\n\nsdfsdfsd\n\n";

        try (InputStream is = new ByteArrayInputStream(pageLines.getBytes())) {
            List<Page> pages = Page.parsePages(is);
            Assertions.assertEquals(1, pages.size());
            Page page = pages.get(0);
            Assertions.assertEquals("GET_make_page_matcher_better", page.getId());
            Assertions.assertEquals("get:/oh/yes.xhtml", page.getPattern());
            Assertions.assertEquals("do_it_do_it", page.getType());
        }
    }
}
