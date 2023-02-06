package ru.yandex.market.mbo.tms.photoDeduplication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Anastasiya Emelianova / orphie@ / 2/11/22
 */
@SuppressWarnings("checkstyle:all")
public class ResultProcessingServiceTest {
    ResultProcessingService resultProcessingService = new ResultProcessingService();

    private Object invokeProcessing(
            YTreeMapNode node,
            ResultProcessingService.Model model,
            Map<String, ResultProcessingService.Picture> lastModelPic,
            Consumer<ResultProcessingService.Model> consumer
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = ResultProcessingService.class.getDeclaredMethod(
                "processNode",
                YTreeMapNode.class,
                ResultProcessingService.Model.class,
                Map.class,
                Consumer.class
        );
        method.setAccessible(true);
        return method.invoke(resultProcessingService, node, model, lastModelPic, consumer);
    }

    @Test
    public void testOneModelWithOnePhoto() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Map<String, YTreeNode> map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url", null));
        map.put("value", new YTreeStringNodeImpl("1_url", null));
        YTreeMapNode node = new YTreeMapNodeImpl(map, null);

        ResultProcessingService.Model resultModel = null;
        Map<String, ResultProcessingService.Picture> lastModelPic = new HashMap<>();

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});

        assertNotNull(resultModel);
        assertEquals(1L, (long) resultModel.getModelId());
        assertFalse(resultModel.getPictures().isEmpty());
        assertFalse(resultModel.getPictures().get("url").isDuplicate());
    }

    @Test
    public void testOneModelHasDuplicateWithAnotherModel() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Map<String, YTreeNode> map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url1", null));
        map.put("value", new YTreeStringNodeImpl("1_url1", null));
        YTreeMapNode node = new YTreeMapNodeImpl(map, null);

        ResultProcessingService.Model resultModel = null;
        Map<String, ResultProcessingService.Picture> lastModelPic = new HashMap<>();

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertFalse(resultModel.getPictures().get("url1").isDuplicate());

        map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("2_url2", null));
        map.put("value", new YTreeStringNodeImpl("1_url1", null));
        node = new YTreeMapNodeImpl(map, null);

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertFalse(resultModel.getPictures().get("url2").isDuplicate());
    }

    @Test
    public void testOneModelWithPhotoDuplicates() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Map<String, YTreeNode> map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url1", null));
        map.put("value", new YTreeStringNodeImpl("1_url1", null));
        YTreeMapNode node = new YTreeMapNodeImpl(map, null);

        ResultProcessingService.Model resultModel = null;
        Map<String, ResultProcessingService.Picture> lastModelPic = new HashMap<>();

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertFalse(resultModel.getPictures().get("url1").isDuplicate());

        map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url2", null));
        map.put("value", new YTreeStringNodeImpl("1_url1", null));
        node = new YTreeMapNodeImpl(map, null);

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertTrue(resultModel.getPictures().get("url2").isDuplicate());
        assertEquals(2, resultModel.getPictures().size());
    }

    @Test
    public void testModelWithDuplicateAndDuplicateWithAnotherModel() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Map<String, YTreeNode> map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url1", null));
        map.put("value", new YTreeStringNodeImpl("1_url1", null));
        YTreeMapNode node = new YTreeMapNodeImpl(map, null);

        ResultProcessingService.Model resultModel = null;
        Map<String, ResultProcessingService.Picture> lastModelPic = new HashMap<>();

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertFalse(resultModel.getPictures().get("url1").isDuplicate());

        map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url2", null));
        map.put("value", new YTreeStringNodeImpl("1_url1", null));
        node = new YTreeMapNodeImpl(map, null);

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertTrue(resultModel.getPictures().get("url2").isDuplicate());
        assertEquals(2, resultModel.getPictures().size());

        map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("2_url3", null));
        map.put("value", new YTreeStringNodeImpl("1_url1", null));
        node = new YTreeMapNodeImpl(map, null);

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertFalse(resultModel.getPictures().get("url3").isDuplicate());
        assertEquals(1, resultModel.getPictures().size());
    }

    @Test
    public void testModelWithDuplicateThroughAnotherModel() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Map<String, YTreeNode> map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url1", null));
        map.put("value", new YTreeStringNodeImpl("2_url2", null));
        YTreeMapNode node = new YTreeMapNodeImpl(map, null);

        ResultProcessingService.Model resultModel = null;
        Map<String, ResultProcessingService.Picture> lastModelPic = new HashMap<>();

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertFalse(resultModel.getPictures().get("url1").isDuplicate());

        map = new HashMap<>();
        map.put("key", new YTreeStringNodeImpl("1_url2", null));
        map.put("value", new YTreeStringNodeImpl("2_url2", null));
        node = new YTreeMapNodeImpl(map, null);

        resultModel = (ResultProcessingService.Model) invokeProcessing(node, resultModel, lastModelPic, model -> {});
        assertNotNull(resultModel);
        assertFalse(resultModel.getPictures().isEmpty());
        assertTrue(resultModel.getPictures().get("url2").isDuplicate());
        assertEquals(2, resultModel.getPictures().size());
        assertEquals("url1",resultModel.getPictures().get("url2").getParentId());
    }
}
