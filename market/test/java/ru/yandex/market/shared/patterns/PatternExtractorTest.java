package ru.yandex.market.shared.patterns;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.yandex.common.util.html.HtmlUtils;
import ru.yandex.market.robot.patterns.ServerDomProxy;
import ru.yandex.market.robot.shared.models.EntityElements;
import ru.yandex.market.robot.shared.models.Field;
import ru.yandex.market.robot.shared.patterns.ExtractedPattern;
import ru.yandex.market.robot.shared.patterns.PatternExtractor;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.*;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"/>
 * @date 06.12.11
 */
public class PatternExtractorTest extends Assert {
    private static PatternExtractor<Document, Element, Node> patternExtractor =
        new PatternExtractor<Document, Element, Node>(
            new ServerDomProxy()
        );

    private XPath xpathFactory = XPathFactory.newInstance().newXPath();
    private Document document;
    private ExtractedPattern previousPattern;

    @Test
    public void testCreateFieldPattern() throws Exception {
        String pattern;
        document = getDocument("aviacassa2.html");
        assertEquals(
            "Москва",
            evaluateString(fieldXPath("//*[@id=\"ak_content\"]/TABLE/TBODY/TR/TD/DIV[1]/UL[2]/LI/DIV/H3", "Text", "Москва"))
        );

        document = getDocument("jinni-com.html");
        pattern = extractPattern(
            "(/HTML/BODY/DIV[2]/DIV[3]/DIV[2]/DIV[3]/DIV/DIV[2]/DIV[2]/A)[1]",
            false, true
        );
        document = getDocument("jinni-com2.html");
        assertEquals(
            "Christopher Nolan 2",
            evaluateString(pattern)
        );

        document = getDocument("teknosa-com.html");
        assertEquals(
            "testImage",
            fieldString("(//DIV[@class='imageContainer'])[1]/A", "Image")
        );
        document = getDocument("cimri-com.html");
        assertEquals(
            evaluate("/HTML/BODY/DIV/DIV[2]/H1"),
            fieldElements("/HTML/BODY/DIV/DIV[2]/H1")
        );
        assertEquals(
            evaluate("/HTML/BODY/DIV/DIV[2]/DIV[5]/DIV[3]/DIV[15]"),
            fieldElements("/HTML/BODY/DIV/DIV[2]/DIV[5]/DIV[3]/DIV[15]")
        );
        assertEquals(
            "http://img.cimri.com/pictures/wm/632/632027_200-200.jpg",
            fieldString("/HTML/BODY/DIV/DIV[2]/DIV[5]/DIV[2]/A/IMG", "Image")
        );
        document = getDocument("anindakapida-com.html");

        assertEquals(
            "/ProductImages/117384/42lv5500.jpg",
            fieldString("//*[@id='ctl00_PortalContent_ctl00_UCBaseDetail_UCImg_ImgSelected']", "Image")
        );

        /* Не менять порядок следующего кода: Начало */
        assertTrue(
            fieldElements("/HTML/BODY/FORM/DIV[4]/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR/TD[3]/DIV[4]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD[2]/SPAN").equals(
                evaluate("/HTML/BODY/FORM/DIV[4]/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR/TD[3]/DIV[4]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD[2]/SPAN")
            )
        );

        document = getDocument("anindakapida2-com.html");

        assertEquals(
            evaluate("/HTML/BODY/FORM/DIV[4]/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR/TD[3]/DIV[4]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD[2]/SPAN"),
            evaluate(previousPattern.getPattern())
        );
        /* Конец */

        document = getDocument("bbk-ru.html");

        /* Не менять порядок следующего кода: Начало */
        assertEquals(
            evaluate("/HTML/BODY/TABLE/TBODY/TR[8]/TD/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR[2]/TD/TABLE/TBODY/TR[46]/TD[2]"),
            fieldElements("/HTML/BODY/TABLE/TBODY/TR[8]/TD/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR[2]/TD/TABLE/TBODY/TR[46]/TD[2]")
        );
        document = getDocument("bbk-ru2.html");

        assertEquals(
            evaluate("/HTML/BODY/TABLE/TBODY/TR[8]/TD/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR[2]/TD/TABLE/TBODY/TR[73]/TD[2]"),
            evaluate(previousPattern.getPattern())
        );

        /* Конец */

        /* Не менять порядок следующего кода: Начало*/
        document = getDocument("otzyv-ru.html");
        assertEquals(
            evaluate("/HTML/BODY/TABLE/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/FONT/B/FONT"),
            fieldElements("/HTML/BODY/TABLE/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/FONT/B/FONT")
        );

        document = getDocument("otzyv-ru2.html");
        assertEquals(
            evaluate("/HTML/BODY/TABLE/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/FONT/B/FONT"),
            evaluate(previousPattern.getPattern())
        );
        /* Конец */

        /* Не менять порядок следующих блоков: Начало */
        document = getDocument("my-media-gadgets.html");

        assertEquals(
            evaluate("/HTML/BODY/DIV/DIV/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD[2]/SPAN[position() = 47 or position() = 49]"),
            fieldElements("/HTML/BODY/DIV/DIV/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD[2]/SPAN[position() = 47 or position() = 49]")
        );

        document = getDocument("my-media-gadgets2.html");

        assertEquals(
            evaluate("/HTML/BODY/DIV/DIV/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD[2]/SPAN[position() = 45 or position() = 47]"),
            evaluate(previousPattern.getPattern())
        );
        /* Конец */

        document = getDocument("deveyuku-com.html");
        assertEquals(
            "63,77 TL",
            fieldString(
                "/HTML/BODY/DIV[2]/DIV/DIV[2]/DIV[2]/FORM/DIV[2]/DIV[2]", "text",
                "63,77 TL"
            ).trim()
        );

        document = getDocument("kino-otzyv-ru.html");

        assertEquals(
            "5+",
            fieldString(
                "/HTML/BODY/TABLE/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD[2]/FONT",
                "text",
                "5+"
            ).trim()
        );

        assertEquals(
            "18.03.08 12:36:36",
            fieldString(
                "/HTML/BODY/TABLE/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD[2]/TABLE[3]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD[2]/FONT",
                "text",
                "18.03.08 12:36:36"
            ).trim()
        );

        document = getDocument("hotel-ru2.html");
        assertEquals(
            "Февраль 2011",
            fieldString(
                "/HTML/BODY/TABLE/TBODY/TR[7]/TD[3]/TABLE/TBODY/TR[3]/TD/CENTER[1]",
                "text",
                "Февраль 2011"
            ).trim()
        );

        /*
         * Тестируем замену конкретной позиции на last()
         */
        document = getDocument("cimri-com-last.html");

        pattern = extractPattern("(/HTML/BODY/DIV/DIV[2]/DIV/DIV[2]/DIV/DIV/A/H3/SPAN)[position() != last()]", false);

        document = getDocument("cimri-com-last2.html");

        assertEquals(
            evaluate("(/HTML/BODY/DIV/DIV[2]/DIV/DIV[2]/DIV/DIV/A/H3/SPAN)[position() != last()]", document),
            evaluate(pattern, document)
        );

        /*
         * Тестируем генерацию шаблонов на класс, в котором есть id
         */
        document = getDocument("alisverisium-com.html");

        pattern = extractPattern("//*[@id=\"divprice74527\"]", false, true);

        document = getDocument("alisverisium-com2.html");

        assertEquals(
            evaluateString("//*[@id=\"divprice74793\"]"),
            evaluateString(pattern)
        );

        /*
         * Тестируем генерацию шаблонов для таблицы
         */
        document = getDocument("ticket2.html");

        String dateFromXPath = fieldXPath(
            "/HTML/BODY/DIV/DIV[2]/DIV/DIV[4]/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[1]/DIV[1]",
            "text",
            "31.12.2012"
        );

        String dateToXPath = fieldXPath(
            "/HTML/BODY/DIV/DIV[2]/DIV/DIV[4]/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[3]/DIV[1]",
            "text",
            "31.12.2012"
        );

        String placeXPath = fieldXPath(
            "/HTML/BODY/DIV/DIV[2]/DIV/DIV[4]/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD[4]/DIV[1]",
            "text",
            "Душанбе"
        );

        document = getDocument("ticket3.html");

        assertEquals(
            "22.08.2001",
            evaluateString(dateFromXPath).trim()
        );

        assertEquals(
            "22.05.2024",
            evaluateString(dateToXPath).trim()
        );

        assertEquals(
            "Анапа",
            evaluateString(placeXPath).trim()
        );

        document = getDocument("ticket4.html");

        assertEquals(
            "substring-before(substring-after(/HTML/BODY/DIV/P[5]/text()[1], \",\"), \",\")",
            fieldXPath(
                "/HTML/BODY/DIV/P[5]",
                "text",
                "20:35"
            ).trim()
        );
    }

    protected String extractPattern(String selectedXPath, boolean otherElementsAllowed) {
        return extractPattern(selectedXPath, otherElementsAllowed, false);
    }

    protected String extractPattern(String selectedXPath, boolean otherElementsAllowed, boolean isExtractionPattern) {
        ExtractedPattern pattern = patternExtractor.createPattern(
            evaluate(selectedXPath), Collections.<Element>emptySet(),
            document, otherElementsAllowed, isExtractionPattern
        );

        System.out.println(pattern.getPattern());

        return pattern.getPattern();
    }

    protected Set<Element> extractPattern(String selectedXPath, String excludedXPath, boolean otherElementsAllowed) {
        Set<Element> excludedElements;
        if (!excludedXPath.isEmpty()) {
            excludedElements = evaluate(excludedXPath);
        } else {
            excludedElements = Collections.emptySet();
        }
        ExtractedPattern pattern = patternExtractor.createPattern(
            evaluate(selectedXPath), excludedElements,
            document, otherElementsAllowed, false
        );

        System.out.println("Pattern:" + pattern.getPattern());

        return evaluate(pattern.getPattern());
    }

    protected ExtractedPattern createEntityPattern(final String firstFieldPattern, final String secondFieldPattern,
                                                   String excludeElementsPattern) {
        Set<Element> excludedElements = new HashSet<Element>();
        if (!excludeElementsPattern.isEmpty()) {
            excludedElements = evaluate(excludeElementsPattern);
        }
        ExtractedPattern pattern = patternExtractor.createEntityPattern(
            new ArrayList<Set<Element>>() {{
                add(evaluate(firstFieldPattern));
                add(evaluate(secondFieldPattern));
            }}, excludedElements, document, true
        );
        System.out.println("Entity pattern:" + pattern.getPattern());
        return pattern;
    }

    protected Set<Element> extractEntities(final String firstFieldPattern, final String secondFieldPattern,
                                           String excludeElementsPattern) {
        ExtractedPattern pattern = createEntityPattern(firstFieldPattern, secondFieldPattern, excludeElementsPattern);
        return evaluate(pattern.getPattern());
    }

    protected List<Element> extractEntitiesList(final String firstFieldPattern, final String secondFieldPattern,
                                                String excludeElementsPattern) {
        ExtractedPattern pattern = createEntityPattern(firstFieldPattern, secondFieldPattern, excludeElementsPattern);
        return evaluateList(pattern.getPattern());
    }

    protected Set<Element> fieldElements(String selectedXPath) {
        previousPattern = patternExtractor.createFieldPattern(
            new Field(), evaluateMap(selectedXPath), document
        );
        System.out.println("Pattern:" + previousPattern.getPattern());
        System.out.println("Value:" + evaluateString(previousPattern.getPattern()));

        return evaluate(previousPattern.getPattern());
    }

    protected String fieldString(String selectedXPath, String type) {
        return fieldString(selectedXPath, type, "");
    }

    protected String fieldXPath(String selectedXPath, String type, String value) {
        Field field = new Field();
        field.setType(type);
        ExtractedPattern pattern = patternExtractor.createFieldPattern(
            field, evaluateMap(selectedXPath, value), document
        );
        System.out.println("Pattern:" + pattern.getPattern());
        return pattern.getPattern();
    }

    protected String fieldString(String selectedXPath, String type, String value) {
        Field field = new Field();
        field.setType(type);
        ExtractedPattern pattern = patternExtractor.createFieldPattern(
            field, evaluateMap(selectedXPath, value), document
        );

        String result = evaluateString(pattern.getPattern());
        System.out.println("Pattern:" + pattern.getPattern());
        System.out.println("Value:" + result);

        return result;
    }

    @Test
    public void testCreatePattern() throws Exception {
        document = getDocument("bitmeyenkartus.html");
        assertEquals(
            evaluate("//DIV[2]/TABLE/TBODY/TR/TD/TABLE[contains(@class,\"_borderNone\") and contains(@class,\"wpe\")]/TBODY/TR/TD/A"),
            extractPattern(
                "//DIV[2]/TABLE/TBODY/TR/TD/TABLE[contains(@class,\"_borderNone\") and contains(@class,\"wpe\")]/TBODY/TR/TD/A[1]",
                "/HTML/BODY/DIV[2]/DIV[4]/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/DIV/DIV/DIV[3]/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR/TD/A",
                true
            )
        );

        document = getDocument("bosh-home-gr.html");
        assertEquals(
            evaluate("//DIV[contains(@class,\"menuCol\")]/UL/LI[position() != 1]"),
            extractPattern(
                "/HTML/BODY/FORM/DIV[3]/DIV/DIV[2]/DIV[6]/DIV[2]/DIV[4]/DIV/DIV/UL/LI[position() > 1]",
                "/HTML/BODY/FORM/DIV[3]/DIV/DIV[2]/DIV[6]/DIV[2]/DIV[4]/DIV/DIV/UL/LI[1]",
                true
            )
        );

        document = getDocument("otzyv-ru3.html");
        assertEquals(
            evaluate("/HTML/BODY/DIV/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/A"),
            extractPattern(
                "/HTML/BODY/DIV/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/TABLE[2]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[3]/TD/A",
                "",
                true
            )
        );

        document = getDocument("webdenbul-com.html");

        assertEquals(
            evaluate("/HTML/BODY/FORM/DIV[3]/DIV[2]/TABLE/TBODY/TR/TD[4]/DIV[3]/DIV[2]/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/A"),
            extractPattern(
                "/HTML/BODY/FORM/DIV[3]/DIV[2]/TABLE/TBODY/TR/TD[4]/DIV[3]/DIV[2]/TABLE/TBODY/TR/TD[2]/TABLE/TBODY/TR/TD/A", "", false
            )
        );
    }

    @Test
    public void testCreateEntityPattern() throws Exception {
        List<Element> entitiesList;
        ExtractedPattern pattern;
        List<Element> elements;
/*
        document = getDocument("aviacassa.html");
        pattern = createEntityPattern(
                "/HTML/BODY/DIV/TABLE/TBODY/TR[2]/TD/TABLE[2]/TBODY/TR[2]/TD/DIV[2]/TABLE/TBODY/TR/TD[4] | " +
                        "/HTML/BODY/DIV/TABLE/TBODY/TR[2]/TD/TABLE[2]/TBODY/TR[2]/TD/DIV[2]/TABLE/TBODY/TR/TD[3]",
                "/HTML/BODY/DIV/TABLE/TBODY/TR[2]/TD/TABLE[2]/TBODY/TR[2]/TD/SPAN/DIV/TABLE/TBODY/TR/TD[4] | " +
                        "/HTML/BODY/DIV/TABLE/TBODY/TR[2]/TD/TABLE[2]/TBODY/TR[2]/TD/SPAN/DIV/TABLE/TBODY/TR/TD[3]",
                ""
        );
        elements = evaluateList(pattern.getPattern());
        assertEquals(
                evaluate("//DIV[@style = 'padding:5px 15px;'][TABLE[1]/TBODY/TR[1]/TD[1]/SPAN]"),
                new HashSet<Element>(elements)
        );
*/
        /* Не менять порядок следующих блоков: Начало */
        document = getDocument("darty.html");
        pattern = createEntityPattern(
            "/HTML/BODY/DIV[3]/DIV/FORM/DIV[2]/DIV[8]/TABLE/TBODY/TR[2]/TD[2]/TABLE/TBODY/TR[2]/TD/DIV[3]/DIV[2]/DIV[2]/B |" +
                "/HTML/BODY/DIV[3]/DIV/FORM/DIV[2]/DIV[8]/TABLE/TBODY/TR[2]/TD[2]/TABLE/TBODY/TR[2]/TD/DIV[3]/DIV[2]/H3 |" +
                "/HTML/BODY/DIV[3]/DIV/FORM/DIV[2]/DIV[8]/TABLE/TBODY/TR[2]/TD[2]/TABLE/TBODY/TR[2]/TD/DIV[3]/DIV[3]",
            "/HTML/BODY/DIV[3]/DIV/FORM/DIV[2]/DIV[8]/TABLE/TBODY/TR[2]/TD[2]/TABLE/TBODY/TR[2]/TD/DIV[3]/DIV[5]/DIV[2]/B |" +
                "/HTML/BODY/DIV[3]/DIV/FORM/DIV[2]/DIV[8]/TABLE/TBODY/TR[2]/TD[2]/TABLE/TBODY/TR[2]/TD/DIV[3]/DIV[5]/H3 |" +
                "/HTML/BODY/DIV[3]/DIV/FORM/DIV[2]/DIV[8]/TABLE/TBODY/TR[2]/TD[2]/TABLE/TBODY/TR[2]/TD/DIV[3]/DIV[6]",
            ""
        );
        elements = evaluateList(pattern.getPattern());
        assertEquals(
            evaluate("//DIV[@class = 'productFiche_commentaire_rate_cell']"),
            new HashSet<Element>(elements)
        );
        assertEquals(
            evaluateString("DIV[@class = 'productFiche_commentaire_rate_pseudo']/B", elements.get(1)),
            extractContextualPattern(
                "DIV[@class = 'productFiche_commentaire_rate_pseudo']/B", elements.get(0), elements.get(1)
            )
        );
        assertEquals(
            evaluateString("following-sibling::DIV[1]", elements.get(1)),
            extractContextualPattern(
                "following-sibling::DIV[1]", elements.get(0), elements.get(1)
            )
        );
        /* Конец */

        /* Не менять порядок следующих блоков: Начало */
        document = getDocument("lufthansa.html");
        pattern = createEntityPattern(
            "/HTML/BODY/DIV/TABLE[6]/TBODY/TR/TD[2] | /HTML/BODY/DIV/TABLE[6]/TBODY/TR/TD[3]",
            "/HTML/BODY/DIV/TABLE[7]/TBODY/TR[3]/TD[2] | /HTML/BODY/DIV/TABLE[7]/TBODY/TR[3]/TD[3]",
            ""
        );
        elements = evaluateList(pattern.getPattern());
        assertEquals(
            evaluate("/HTML/BODY/DIV/TABLE[translate(@style,'; \"','') = \"table-layout:fixed\"]"),
            new HashSet<Element>(elements)
        );

        //Генерация шаблона сразу по нескольким сущностям
        assertEquals(
            evaluateString("TBODY/TR[contains(TD[2], 'выполняется')]/TD[3]", elements.get(1)),
            extractContextualPattern(
                "TBODY/TR[contains(TD[2], 'выполняется')]/TD[3]",
                elements, elements.get(1)
            )
        );
        /* Конец */

        /* Не менять порядок следующих блоков: Начало */
        //Тестируем генерацию шаблонов с rowspan
        document = getDocument("barofly.html");

        pattern = createEntityPattern(
            "/HTML/BODY/DIV/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR[2]/TD[2] |\n" +
                "/HTML/BODY/DIV/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR[2]/TD[3]\n",
            "/HTML/BODY/DIV/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR[3]/TD |\n" +
                "/HTML/BODY/DIV/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[3]/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[6]/TD/TABLE/TBODY/TR[3]/TD[2]",
            ""
        );

        elements = evaluateList(pattern.getPattern());
        assertEquals(
            evaluate("/HTML/BODY/DIV/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[position() != 2]/TD/TABLE/TBODY/TR[position() != 1]"),
            new HashSet<Element>(elements)
        );

        assertEquals(
            evaluateString("TD[position() = last() - 4]", elements.get(1)),
            extractContextualPattern(
                "TD[position() = last() - 4]",
                elements, elements.get(1)
            )
        );
        /* Конец */

        /* Не менять порядок следующих блоков: Начало */
        document = getDocument("lufthansa2.html");
        entitiesList = extractEntitiesList(
            "/HTML/BODY/DIV/DIV/TABLE[10]/TBODY/TR[2]/TD/TABLE/TBODY/TR[2]/TD[2]/FONT | " +
                "/HTML/BODY/DIV/DIV/TABLE[10]/TBODY/TR[2]/TD[2]/TABLE/TBODY/TR[2]/TD[2]/FONT",
            "/HTML/BODY/DIV/DIV/TABLE[10]/TBODY/TR[3]/TD/TABLE/TBODY/TR[2]/TD[2]/FONT | " +
                "/HTML/BODY/DIV/DIV/TABLE[10]/TBODY/TR[3]/TD[2]/TABLE/TBODY/TR[2]/TD[2]/FONT",
            ""
        );
        String patternString = createFieldPattern(
            entitiesList.get(0), "TD[2]/TABLE/TBODY/TR[2]/TD[3]/FONT/B"
        );
        assertEquals(
            evaluateString("TD[2]/TABLE/TBODY/TR[2]/TD[3]/FONT/B", entitiesList.get(1)),
            evaluateString(patternString, entitiesList.get(1))
        );
        /* Конец */

        /* Не менять порядок следующих блоков: Начало */
        document = getDocument("airticket1.html");
        pattern = createEntityPattern(
            "/HTML/BODY/DIV/CENTER/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[12]/TD/TABLE/TBODY/TR/TD[2]/B | " +
                "/HTML/BODY/DIV/CENTER/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[12]/TD/TABLE[2]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD",
            "/HTML/BODY/DIV/CENTER/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[13]/TD/TABLE/TBODY/TR/TD[2]/B | " +
                "/HTML/BODY/DIV/CENTER/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[13]/TD/TABLE[2]/TBODY/TR/TD/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD",
            ""
        );
        assertEquals(
            evaluate("//TABLE/TBODY/TR[TD[1]/TABLE/TBODY/TR/TD[1]/@style = 'color:rgb(0,60,150);border-top:2px solid #0046AF;']"),
            evaluate(pattern.getPattern())
        );
        document = getDocument("airticket2.html");
        /*
        assertEquals(
            evaluate("//TABLE/TBODY/TR[TD[1]/TABLE/TBODY/TR/TD[1]/@style = 'color:rgb(0,60,150);border-top:2px solid #0046AF;']"),
            evaluate(pattern.getPattern())
        );
        */
        /* Конец */

        /* Не менять порядок следующих блоков: Начало */
        document = getDocument("otzyvru-net.html");
        assertEquals(
            evaluate("//DIV[contains(@class,\"mainblock\")][position() != 1]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[position() != 1]"),
            extractEntities(
                "/HTML/BODY/DIV/DIV/DIV[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD/A/IMG",
                "/HTML/BODY/DIV/DIV/DIV[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[6]/TD/A/IMG",
                ""
            )
        );

        Set<Element> entities = extractEntities(
            "/HTML/BODY/DIV/DIV/DIV[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[1]/TD/A | /HTML/BODY/DIV/DIV/DIV[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[2]/TD/B",
            "/HTML/BODY/DIV/DIV/DIV[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[5]/TD/A | /HTML/BODY/DIV/DIV/DIV[2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR[6]/TD/B",
            ""
        );

        assertEquals(
            evaluate("(//DIV[contains(@class,\"mainblock\")][2]/TABLE/TBODY/TR/TD/TABLE/TBODY/TR)[position() mod 4 = 1]"),
            entities
        );

        entitiesList = new ArrayList<Element>(entities);

        Element entity = entitiesList.get(0);
        assertEquals(
            evaluateString("following-sibling::TR[1]/TD/B", entity),
            extractContextualPattern("following-sibling::TR[1]/TD/B", entity)
        );
        /* Конец */

        document = getDocument("ucuzu-com.html");

        entities = extractEntities(
            "/HTML/BODY/DIV/DIV[2]/DIV/DIV[2]/DIV[2]/DIV[3]/DIV/DIV/DIV/DIV/DIV[1]/DIV[3]/DIV/H4/A",
            "/HTML/BODY/DIV/DIV[2]/DIV/DIV[2]/DIV[2]/DIV[3]/DIV/DIV/DIV/DIV/DIV[2]/DIV[3]/DIV/H4/A",
            ""
        );

        assertEquals(
            evaluate("//DIV[contains(@class,\"promoted-product\") and contains(@class,\"one_product\")]"),
            entities
        );

        document = getDocument("cimri-com-opinion.html");

        entities = extractEntities(
            "/HTML/BODY/DIV/DIV[2]/DIV/DIV[5]/DIV[4]/DIV[3]/DIV[2]/DIV[2]/DIV | /HTML/BODY/DIV/DIV[2]/DIV/DIV[5]/DIV[4]/DIV[3]/DIV[2]/DIV[3]/P[2]",
            "/HTML/BODY/DIV/DIV[2]/DIV/DIV[5]/DIV[4]/DIV[3]/DIV[2]/DIV[5]/DIV | /HTML/BODY/DIV/DIV[2]/DIV/DIV[5]/DIV[4]/DIV[3]/DIV[2]/DIV[6]/P[2]",
            ""
        );

        assertEquals(
            evaluate("//DIV[@class = \"yorum_bant\"]"),
            entities
        );

        document = getDocument("ticket1.html");

        entities = extractEntities(
            "/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE[1]/TBODY/TR[3]/TD[2] | (/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE/TBODY/TR[4]/TD[2]/TABLE/TBODY/TR/TD/TABLE)[1]/TBODY/TR[1]/TD[2]",
            "/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE[1]/TBODY/TR[8]/TD[2] | (/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE/TBODY/TR[9]/TD[2]/TABLE/TBODY/TR/TD/TABLE)[1]/TBODY/TR[1]/TD[2]",
            ""
        );

        assertEquals(
            evaluate("(/HTML/BODY/TABLE/TBODY/TR/TD/DIV/DIV[6]/TABLE/TBODY/TR[not(position() = 1 or position() = 2)])[position() mod 5 = 1]"),
            entities
        );

        /*
         * Тестируем генерацию абсолютных шаблонов для значений,
         * при обучении извлечения нескольких сущностей
         */
        entities = extractEntities(
            "/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE[1]/TBODY/TR[3]/TD[2] | (/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE/TBODY/TR[4]/TD[2]/TABLE/TBODY/TR/TD/TABLE)[1]/TBODY/TR[1]/TD[2] | /HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV/TABLE/TBODY/TR[2]/TD/SPAN",
            "/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE[1]/TBODY/TR[8]/TD[2] | (/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE/TBODY/TR[9]/TD[2]/TABLE/TBODY/TR/TD/TABLE)[1]/TBODY/TR[1]/TD[2]",
            ""
        );

        assertEquals(
            evaluate("(/HTML/BODY/TABLE/TBODY/TR/TD/DIV/DIV[6]/TABLE/TBODY/TR[not(position() = 1 or position() = 2)])[position() mod 5 = 1]"),
            entities
        );

        entities = extractEntities(
            "/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE[1]/TBODY/TR[3]/TD[2] | (/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE/TBODY/TR[4]/TD[2]/TABLE/TBODY/TR/TD/TABLE)[1]/TBODY/TR[1]/TD[2] | /HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV/TABLE/TBODY/TR[2]/TD/SPAN",
            "/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE[1]/TBODY/TR[8]/TD[2] | (/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV[6]/TABLE/TBODY/TR[9]/TD[2]/TABLE/TBODY/TR/TD/TABLE)[1]/TBODY/TR[1]/TD[2] | /HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV/TABLE/TBODY/TR[2]/TD/SPAN",
            ""
        );

        assertEquals(
            evaluate("(/HTML/BODY/TABLE/TBODY/TR/TD/DIV/DIV[6]/TABLE/TBODY/TR[not(position() = 1 or position() = 2)])[position() mod 5 = 1]"),
            entities
        );

        assertEquals(
            evaluateString("/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV/TABLE/TBODY/TR[2]/TD/SPAN"),
            evaluateFieldString(
                entities.iterator().next(),
                "/HTML/BODY/TABLE/TBODY/TR[2]/TD/DIV/DIV/TABLE/TBODY/TR[2]/TD/SPAN"
            )
        );
    }

    String extractContextualPattern(String pattern, Element entity, Element evaluateElement) {
        return extractContextualPattern(pattern, Collections.singletonList(entity), evaluateElement);
    }

    String extractContextualPattern(String pattern, List<Element> entities, Element evaluateElement) {
        List<EntityElements<Element, Document>> examples = new ArrayList<EntityElements<Element, Document>>();
        for (Element element : entities) {
            Map<Element, String> fieldElements = evaluateMap(pattern, element);
            if (fieldElements.isEmpty()) {
                continue;
            }
            examples.add(new EntityElements<Element, Document>(element, document, fieldElements));
        }
        String extractedPattern = patternExtractor.createFieldPatternForEntity(new Field(), examples).getPattern();
        System.out.println("Pattern:" + extractedPattern);
        return evaluateString(extractedPattern, evaluateElement);
    }

    protected String extractContextualPattern(String pattern, Element entity) {
        return extractContextualPattern(pattern, entity, entity);
    }

    protected String evaluateString(String xpath, Object context) {
        try {
            return xpathFactory.evaluate(xpath, context);
        } catch (Exception ex) {
            throw new RuntimeException("XPath exception:" + xpath, ex);
        }
    }

    protected String createFieldPattern(Element entity, String xpath) {
        ExtractedPattern pattern = patternExtractor.createFieldPatternForEntity(
            new Field(),
            Collections.singletonList(
                new EntityElements<Element, Document>(entity, document, evaluateMap(xpath, entity))
            )
        );
        System.out.println("Pattern:" + pattern.getPattern());
        return pattern.getPattern();
    }

    protected String evaluateFieldString(Element entity, String xpath) {
        String pattern = createFieldPattern(entity, xpath);
        return evaluateString(pattern, entity);
    }

    protected String evaluateString(String xpath) {
        return evaluateString(xpath, document);
    }

    protected Set<Element> evaluate(String xpath) {
        return evaluate(xpath, document);
    }

    protected Map<Element, String> evaluateMap(String xpath) {
        return evaluateMap(xpath, "");
    }

    protected Map<Element, String> evaluateMap(String xpath, String value) {
        Map<Element, String> result = new HashMap<Element, String>();
        for (Element element : evaluate(xpath, document)) {
            result.put(element, value);
        }
        return result;
    }

    protected Map<Element, String> evaluateMap(String xpath, Object context) {
        Map<Element, String> result = new HashMap<Element, String>();

        for (Element element : evaluate(xpath, context)) {
            result.put(element, "");
        }

        return result;
    }

    protected Set<Element> evaluate(String xpath, Object context) {
        return new HashSet<Element>(evaluateList(xpath, context));
    }

    protected List<Element> evaluateList(String xpath) {
        return evaluateList(xpath, document);
    }

    protected List<Element> evaluateList(String xpath, Object context) {
        try {
            List<Element> result = new ArrayList<Element>();
            NodeList nodeSet = (NodeList) xpathFactory.evaluate(xpath, context, XPathConstants.NODESET);
            int size = nodeSet.getLength();
            for (int i = 0; i < size; i++) {
                Node node = nodeSet.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.add((Element) node);
                }
            }
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("XPath exception:" + xpath, ex);
        }
    }

    protected InputStream getInputStream(String path) {
        return this.getClass().getResourceAsStream("/" + path);
    }

    protected Document getDocument(String path) {
        System.out.println("Document:" + path);
        try {
            return HtmlUtils.parseStream(getInputStream(path));
        } catch (Exception ex) {
            throw new RuntimeException("Parse document exception:" + path, ex);
        }
    }
}
