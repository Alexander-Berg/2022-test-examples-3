package ru.yandex.chemodan.app.docviewer.utils.html;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.utils.XmlUtils2;
import ru.yandex.misc.io.StringReaderSource;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

public class HtmlFilterTest {

    @Test
    public void test() {
        final String sourceHtml = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n"
                + "\n"
                + "<HTML>\n"
                + "<HEAD>\n"
                + "    \n"
                + "    <META HTTP-EQUIV=\"CONTENT-TYPE\" CONTENT=\"text/html; charset=utf-8\">\n"
                + "    <TITLE></TITLE>\n"
                + "    <META NAME=\"GENERATOR\" CONTENT=\"LibreOffice 3.3  (Unix)\">\n"
                + "    <META NAME=\"AUTHOR\" CONTENT=\"qwerty\">\n"
                + "    <META NAME=\"CREATED\" CONTENT=\"20061025;8004900\">\n"
                + "    <META NAME=\"CHANGEDBY\" CONTENT=\"Анатолий Рохманов\">\n"
                + "    <META NAME=\"CHANGED\" CONTENT=\"20091001;12563900\">\n"
                + "    <META NAME=\"AppVersion\" CONTENT=\"12.0000\">\n"
                + "    <META NAME=\"Company\" CONTENT=\"ООО &quot;ТК ВОГ-Керамика&quot;\">\n"
                + "    <META NAME=\"DocSecurity\" CONTENT=\"0\">\n"
                + "    <META NAME=\"HyperlinksChanged\" CONTENT=\"false\">\n"
                + "    <META NAME=\"LinksUpToDate\" CONTENT=\"false\">\n"
                + "    <META NAME=\"ScaleCrop\" CONTENT=\"false\">\n"
                + "    <META NAME=\"ShareDoc\" CONTENT=\"false\">\n"
                + "    \n"
                + "    <STYLE>\n"
                + "        <!-- \n"
                + "        BODY,DIV,TABLE,THEAD,TBODY,TFOOT,TR,TH,TD,P { font-family:\"Arial Cyr\"; font-size:x-small }\n"
                + "         -->\n"
                + "    </STYLE>\n"
                + "    \n"
                + "</HEAD>\n"
                + "\n"
                + "<BODY TEXT=\"#000000\">\n"
                + "<HR>\n"
                + "    <P><CENTER>\n"
                + "        <H1>Обзор</H1>\n"
                + "        <A HREF=\"#table0\">Cersanit Мебель 01.05.09г.</A><BR>\n"
                + "        <A HREF=\"#table1\">Керамин сантехника 17.09.09г.</A><BR>\n"
                + "        <A HREF=\"#table2\">Cersanit 2сорт 23.03.09г. </A><BR>\n"
                + "        <A HREF=\"#table3\">JIKA 01.04.09г.</A><BR>\n"
                + "        <A HREF=\"#table4\">Керамин плитка 23.07.09г.</A><BR>\n"
                + "        <A HREF=\"#table5\">Интеркерама  Москва 20.08.09</A><BR>\n"
                + "        <A HREF=\"#table6\">ЗЕВС Соль перец + чипсы</A><BR>\n"
                + "        <A HREF=\"#table7\">ЗЕВС глазур-ый гранит</A><BR>\n"
                + "        <A HREF=\"#table8\">Cersanit 14.09.09 </A><BR>\n"
                + "        \n"
                + "    </CENTER></P>\n"
                + "<HR>\n"
                + "<A NAME=\"table0\"><H1>Лист 1: <EM>Cersanit Мебель 01.05.09г.</EM></H1></A>\n"
                + "<TABLE FRAME=VOID CELLSPACING=0 COLS=6 RULES=NONE BORDER=0>\n"
                + "    <COLGROUP><COL WIDTH=93><COL WIDTH=91><COL WIDTH=178><COL WIDTH=727><COL WIDTH=175><COL WIDTH=155></COLGROUP>\n"
                + "    <TBODY>\n"
                + "        <TR>\n"
                + "            <TD WIDTH=93 HEIGHT=17 ALIGN=LEFT VALIGN=BOTTOM SDNUM=\"1049;1049;Standard\"><FONT COLOR=\"#000000\"><BR></FONT></TD>\n"
                + "            <TD WIDTH=91 ALIGN=LEFT VALIGN=BOTTOM SDNUM=\"1049;1049;Standard\"><FONT SIZE=1 COLOR=\"#000000\"><BR></FONT></TD>\n"
                + "            <TD WIDTH=178 ALIGN=LEFT VALIGN=BOTTOM SDNUM=\"1049;1049;Standard\"><FONT SIZE=1 COLOR=\"#000000\"><BR></FONT></TD>\n"
                + "            <TD ROWSPAN=6 WIDTH=727 ALIGN=LEFT VALIGN=BOTTOM SDNUM=\"1049;1049;Standard\"><FONT SIZE=1 COLOR=\"#000000\"><BR><IMG SRC=\"215_html_m3aa3e8d.jpg\" WIDTH=305 HEIGHT=100 HSPACE=210 VSPACE=3>\n"
                + "            </FONT></TD>\n"
                + "            <TD WIDTH=175 ALIGN=LEFT VALIGN=BOTTOM SDNUM=\"1049;1049;Standard\"><FONT SIZE=1 COLOR=\"#000000\"><BR></FONT></TD>\n"
                + "            <TD WIDTH=155 ALIGN=LEFT VALIGN=BOTTOM SDNUM=\"1049;1049;Standard\"><BR></TD>\n"
                + "        </TR>";

        Document doc = XmlUtils2.parseHtmlToDom4j(new StringReaderSource(sourceHtml));
        new HtmlFilter(doc).process();
        OutputFormat format = new OutputFormat();
        format.setIndent(false);
        format.setSuppressDeclaration(true);
        format.setXHTML(true);
        String result = Dom4jUtils.writeToString(doc.getRootElement(), format);

        Assert.assertContains(result,
                "<META content=\"text/html; charset=utf-8\" http-equiv=\"CONTENT-TYPE\"/>");
        Assert.assertContains(result,
                "BODY,DIV,TABLE,THEAD,TBODY,TFOOT,TR,TH,TD,P{font-family:\"Arial Cyr\";font-size:x-small;}");
        Assert.assertContains(result, "<A href=\"#table8\">Cersanit 14.09.09 </A><BR/>");
        Assert.assertContains(result,
                "<TABLE border=\"0\" cellspacing=\"0\" cols=\"6\" frame=\"VOID\" rules=\"NONE\">");
        Assert.assertContains(result,
                "<IMG height=\"100\" src=\"215_html_m3aa3e8d.jpg\" width=\"305\"/>");
    }

    @Test
    public void testMsxmlOpenoffice() {
        Document doc = XmlUtils2.parseHtmlToDom4j(new UrlInputStreamSource(
                TestResources.HTML_MSXML_OPENOFFICE));

        HtmlFilter htmlFilter = new HtmlFilter(doc);
        htmlFilter.process();

        OutputFormat format = new OutputFormat();
        format.setIndent(false);
        format.setSuppressDeclaration(true);
        format.setXHTML(true);
        String result = Dom4jUtils.writeToString(doc.getRootElement(), format);

        Assert.assertContains(result, "<TD align=\"LEFT\" colspan=\"3\">№ 53482338</TD>");
    }

    @Test
    public void testCommentedStyle() {
        Document doc = XmlUtils2.parseHtmlToDom4j(new UrlInputStreamSource(
                TestResources.HTML_STYLE_COMMENTED));
        HtmlFilter htmlFilter = new HtmlFilter(doc);
        htmlFilter.process();

        OutputFormat format = new OutputFormat();
        format.setIndent(false);
        format.setSuppressDeclaration(true);
        format.setXHTML(true);
        String result = Dom4jUtils.writeToString(doc.getRootElement(), format);

        Assert.assertTrue(result.contains(".justclass1"));
        Assert.assertTrue(result.contains("text-decoration:underline-1;"));
        Assert.assertTrue(result.contains(".justclass2"));
        Assert.assertTrue(result.contains("text-decoration:underline-2;"));

        Assert.assertFalse(result.contains("somescript"));
        Assert.assertFalse(result.contains("on-click"));
        Assert.assertFalse(result.contains("do-not-show-1"));
        Assert.assertFalse(result.contains("do-not-show-2"));
    }

    @Test
    public void testSelectorPatter() {
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("body").matches());
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("h1").matches());
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("h1, h2, h3, h4").matches());
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("div").matches());
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("div.border").matches());
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("a:hover").matches());
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("a:link").matches());
        Assert.assertTrue(HtmlFilter.PATTERN_SELECTOR.matcher("a:focus").matches());

        Assert.assertFalse(HtmlFilter.PATTERN_SELECTOR.matcher("h3, h4 & h5").matches());
    }
}
