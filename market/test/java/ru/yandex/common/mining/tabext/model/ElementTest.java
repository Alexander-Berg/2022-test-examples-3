//package ru.yandex.common.mining.tabext.model;
//
//import junit.framework.TestCase;
//import ru.yandex.common.mining.tabext.mining.Strategy;
//import ru.yandex.common.mining.tabext.mining.strategy.SimpleStrategy;
//import ru.yandex.common.mining.tabext.parser.TagSoupParser;
//import ru.yandex.common.mining.tabext.util.ShingleUtil;
//
///**
// * Date: 26.12.2006
// * Time: 2:13:38
// *
// * @author nmalevanny@yandex-team.ru
// */
//public class ElementTest extends TestCase {
//	private static final WordBuilder builder = new DefaultWordBuilder();
//
//    public void testCreateSimple() {
//        assertNotNull(createTestTable());
//    }
//
//    public void testSimpleStrategies() {
//        final Element root = createTestTable();
//        Strategy s1 = new SimpleStrategy(1);
//        SimpleStrategy s2 = new SimpleStrategy(2);
//        assertEquals(2, s1.selectBlocks(root.getChildren()).size());
//        assertEquals(1, s2.selectBlocks(root.getChildren()).size());
//    }
//
//    public void testBuildWord() {
//        final Element element = TagSoupParser.parseString("test<p><p><p><table width='1' class=''/>");
//        assertEquals("body",
//                ShingleUtil.joinWithDot(builder.buildWord(element, 1)));
//        assertEquals("body.(.<TEXT>.p.p.p.table.)",
//                ShingleUtil.joinWithDot(builder.buildWord(element, 2)));
//        assertEquals("body.(.<TEXT>.p.p.p.table.(.class.width.).)",
//                ShingleUtil.joinWithDot(builder.buildWord(element, 3)));
//    }
//
//    public void testGetFirstChild() {
//        final Element element = TagSoupParser.parseString("test<p><p><p><table width='1' class=''/>");
//        assertNotNull(element.getDescendantOnFirstChildWithName("<TEXT>"));
//        assertNull(element.getDescendantOnFirstChildWithName("table"));
//    }
//
//    private Element createTestTable() {
//        Element table = Element.createRoot("table");
//        table.setAttribute("width","100%");
//        table.setAttribute("border","1");
//        {
//            Element row = table.addChild("tr");
//            row.setAttribute("class","person");
//            row.addTextLeaf("td","testName");
//            final Element td2 = row.addChild("td");
//            td2.setAttribute("width","100");
//            td2.setText("testAge");
//        }
//        {
//            Element row = table.addChild("tr");
//            row.setAttribute("class","person");
//            row.addTextLeaf("td","testName2");
//            final Element td2 = row.addChild("td");
//            td2.setAttribute("width","100");
//            td2.setText("testAge2");
//        }
//        return table;
//    }
//
//}
