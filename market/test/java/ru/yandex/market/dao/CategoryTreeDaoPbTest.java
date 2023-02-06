package ru.yandex.market.dao;

import com.google.common.collect.Sets;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.market.CategoryTree;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.yandex.ir.util.TestUtil;

import static org.junit.Assert.assertEquals;

/**
 * Сверяет ответы обеих реализаций {@link CategoryTreeDao}: ныне не существующей CategoryTreeDaoXml и {@link CategoryTreeDaoPb}.
 * Использует файлик с ожидаемыми ответами {@link CategoryTreeDaoPbTest#controlPath}.
 * Использует файлик с pb выгрузкой {@link CategoryTreeDaoPbTest#recentPath}.
 */
public class CategoryTreeDaoPbTest {
    private static final String rootPath = TestUtil.getSrcTestResourcesPath();
    private static final String controlPath = rootPath + "/control/";
    private static final String recentPath = rootPath + "/recent/";

    private static final Function<CategoryTree.CategoryTreeNode, Integer> hidExtractor = CategoryTree.CategoryTreeNode::getHyperId;
    private static final Function<Map.Entry<CategoryTree.CategoryTreeNode, Integer>, Integer> hidExtractorE = hidExtractor.compose(Map.Entry::getKey);
    // отделяет hid от значений в файлах test/java/control
    private static final String delimiter = ";";
    private static Map<CategoryTree.CategoryTreeNode, Integer> pbMap;
    // правильные ответы, читаются из файла
    private static Map<String, Map<String, String>> field2Hid2valueExpectedMap;
    private static List<NamedFn<String>> fieldsNFs = Arrays.asList(
        new NamedFn<>("getValue", e -> escape(e.getValue().toString())),
        new NamedFn<>("getKey.getOutputCategoryId", t -> escape(t.getKey().getOutputCategoryId())),
        new NamedFn<>("getKey.getTovarId", t -> escape(t.getKey().getTovarId())),
        new NamedFn<>("getKey.getTolokerInstructions", t -> escape(t.getKey().getTolokerInstructions())),
        new NamedFn<>("getKey.getVisualCategoryId",
            t -> escape(t.getKey().getVisualCategoryId()),
            x -> x.getKey().isClusterize()
        ),
        new NamedFn<>("getKey.getUniqueName", t -> escape(t.getKey().getUniqueName())),
        new NamedFn<>("getKey.getOutputType", t -> escape(t.getKey().getOutputType())),
        new NamedFn<>("getKey.getName", t -> escape(t.getKey().getName())),
        new NamedFn<>("getKey.getInCategory", t -> escape(t.getKey().getInCategory())),
        new NamedFn<>("getKey.getParentId", t -> escape(t.getKey().getParentId())),
        new NamedFn<>("getKey.getGuruLightCategoryId",
            t -> escape(t.getKey().getGuruLightCategoryId()),
            x -> !x.getKey().isClusterize() // кажется, можно наложить более сильное условие, но хватает и такого
        ),
        new NamedFn<>("getKey.getMatcherId", t -> escape(t.getKey().getMatcherId())),
        new NamedFn<>("getKey.getOutOfCategory", t -> escape(t.getKey().getOutOfCategory())),
        new NamedFn<>("getKey.getHeight", t -> escape(t.getKey().getHeight())),
        new NamedFn<>("getKey.getHyperId", t -> escape(t.getKey().getHyperId())),
        new NamedFn<>("getKey.getParent", t -> escape(t.getKey().getParent())),
        new NamedFn<>("getKey.isVisible", t -> escape(t.getKey().isVisible())),
        new NamedFn<>("getKey.isClusterize", t -> escape(t.getKey().isClusterize())),
        new NamedFn<>("getKey.isFixedCluster", t -> escape(t.getKey().isFixedCluster())),
        new NamedFn<>("getKey.isPublished", t -> escape(t.getKey().isPublished())),
        new NamedFn<>("getKey.getAliases", t -> {
            List<String> aliases = t.getKey().getAliases();
            if (aliases == null || aliases.isEmpty()) {
                aliases = Collections.emptyList();
            }
            List<String> copy = new ArrayList<>(aliases);
            return escape(copy);
        }),
        new NamedFn<>("getKey.getLinkedCategories", t -> escape(t.getKey().getLinkedCategories())),
        new NamedFn<>("getKey.getChildren", t -> escape(t.getKey().getChildren()))
    );

    private static String escape(Object o) {
        return (o == null ? "null" : o).toString().replaceAll("[\n\r]", "_");
    }

    private static Map<String, String> readReferenceFile(String v) {
        try (Scanner scanner = new Scanner(new FileReader(controlPath + v))) {
            Map<String, String> res = new HashMap<>();
            while (scanner.hasNext()) {
                String s = scanner.nextLine();
                int i = s.indexOf(delimiter);
                String uniqStr = s.substring(0, i);
                String val = s.substring(delimiter.length() + i);
                res.put(uniqStr, val);
            }
            return res;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testActualMap(Map<CategoryTree.CategoryTreeNode, Integer> actualMap) {
        Function<NamedFn<?>, Map<String, String>> f = namedFn ->
            actualMap.entrySet().stream()
                .filter(namedFn)
                .collect(Collectors.toMap(
                    hidExtractorE.andThen(Object::toString),
                    namedFn.andThen(Object::toString)
                ));
        Map<String, Map<String, String>> map = fieldsNFs.stream()
            .collect(Collectors.toMap(NamedFn::getName, f));

        assertEquals(field2Hid2valueExpectedMap.keySet(), map.keySet());
        for (String field : field2Hid2valueExpectedMap.keySet()) {
            Map<String, String> refFields = field2Hid2valueExpectedMap.get(field);
            Map<String, String> actualFields = map.get(field);
            assertEquals("mismatch of keySets on field " + field, refFields.keySet(), actualFields.keySet());

            Set<String> refVals = new HashSet<>(refFields.values());
            Set<String> actVals = new HashSet<>(actualFields.values());
            Similarity valueCompare = Similarity.valueOfComparison(refVals, actVals);
            if (valueCompare.similarity < 1.0) {
                Sets.SetView<String> intersection = Sets.intersection(refVals, actVals);
                HashSet<String> refVals2 = new HashSet<>(refVals);
                HashSet<String> actVals2 = new HashSet<>(actVals);
                refVals2.removeAll(actVals);
                actVals2.removeAll(refVals);
                System.out.println("exp \\ act[" + refVals2.size() + "]: " + refVals2);
                System.out.println("act \\ exp[" + actVals2.size() + "]: " + actVals2);
                System.out.println("act /\\ exp[" + intersection.size() + "]: " + intersection);
            }
            assertEquals("mismatch on field " + field + ": " + valueCompare,
                refVals,
                actVals
            );
        }
        assertEquals(field2Hid2valueExpectedMap, map);
    }

    @BeforeClass
    public static void beforeClass() {
        String dataRoot = ru.yandex.devtools.test.Paths.getSandboxResourcesRoot();
        pbMap = new CategoryTreeDaoPb() {{
            //setFilePath(recentPath);
            setFilePath(dataRoot);
            setTovarTreeFileName("tovar-tree.pb");
        }}.loadCategoryTree();
        field2Hid2valueExpectedMap = fieldsNFs.stream()
            .map(NamedFn::getName)
            .collect(Collectors.toMap(x -> x, CategoryTreeDaoPbTest::readReferenceFile));
    }

    private void testHidUniqueness(Map<CategoryTree.CategoryTreeNode, Integer> actualMap) {
        assertEquals(
            actualMap.size(),
            actualMap.entrySet()
                .stream()
                .map(hidExtractorE)
                .distinct()
                .count()
        );
    }

    @Test
    public void testPbMap() {
        testActualMap(pbMap);
    }

    @Test
    public void testPbHidExtractor() {
        testHidUniqueness(pbMap);
    }
}
