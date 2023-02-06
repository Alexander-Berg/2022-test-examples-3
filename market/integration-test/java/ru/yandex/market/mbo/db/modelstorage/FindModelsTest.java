package ru.yandex.market.mbo.db.modelstorage;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.http.ServiceException;
import ru.yandex.market.mbo.http.ModelStorage;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Тесты ручки FindModels.
 *
 * @author s-ermakov
 */
@Ignore("MBO-15344")
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:mbo-card-api/custom-configs/find-models-config.xml"})
public class FindModelsTest {

    private static final Logger log = Logger.getLogger(FindModelsTest.class);

    @Resource(name = "findModelsService")
    private ModelStorageProtoService modelStorageService;

    @Test
    public void testEmptyRequest() throws Exception {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .build();

        ModelStorage.GetModelsResponse response = modelStorageService.findModels(request);

        assertTrue(response.getModelsCount() > 0);
        assertTrue(response.getModelsFound() > 0);
        assertThat(response.getNextCursorMark(), not(isEmptyString()));
    }

    @Test
    @Ignore("MBO-13695")
    public void testRequestWithIdAscSort() throws Exception {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.ID)
            .build();

        ModelStorage.GetModelsResponse response = modelStorageService.findModels(request);

        ModelStorage.Model firstModel = response.getModels(0);
        ModelStorage.Model secondModel = response.getModels(1);

        assertTrue(response.getModelsCount() > 0);
        assertTrue(firstModel.getId() < secondModel.getId());
        assertThat(response.getNextCursorMark(), isEmptyString());
    }

    @Test
    public void testRequestWithIdDescSort() throws Exception {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.ID)
            .setOrderType(ModelStorage.FindModelsRequest.OrderType.DESC)
            .build();

        ModelStorage.GetModelsResponse response = modelStorageService.findModels(request);

        ModelStorage.Model firstModel = response.getModels(0);
        ModelStorage.Model secondModel = response.getModels(1);

        assertTrue(response.getModelsCount() > 0);
        assertTrue(firstModel.getId() > secondModel.getId());
        assertThat(response.getNextCursorMark(), isEmptyString());
    }

    @Test
    public void testCursorRequests() throws Exception {
        ModelStorage.FindModelsRequest firstRequest = ModelStorage.FindModelsRequest.newBuilder()
            .build();

        ModelStorage.GetModelsResponse firstResponse = modelStorageService.findModels(firstRequest);
        assertThat(firstResponse.getNextCursorMark(), not(isEmptyString()));

        ModelStorage.FindModelsRequest secondRequest = ModelStorage.FindModelsRequest.newBuilder()
            .setCursorMark(firstResponse.getNextCursorMark())
            .build();

        ModelStorage.GetModelsResponse secondResponse = modelStorageService.findModels(secondRequest);

        assertDistinctModels(firstResponse.getModelsList(), secondResponse.getModelsList());
    }

    @Test
    public void testRequestWithOffset() throws Exception {
        List<Integer> batchSizes = Arrays.asList(1, 10, 11, 99, 1000);
        for (Integer batchSize: batchSizes) {
            ModelStorage.FindModelsRequest firstRequest = ModelStorage.FindModelsRequest.newBuilder()
                .setOffset(0)
                .setLimit(batchSize)
                .setOrderBy(ModelStorage.FindModelsRequest.OrderField.ID)
                .build();
            ModelStorage.GetModelsResponse firstResponse = modelStorageService.findModels(firstRequest);

            ModelStorage.FindModelsRequest secondRequest = ModelStorage.FindModelsRequest.newBuilder()
                .setOffset(batchSize)
                .setLimit(batchSize)
                .setOrderBy(ModelStorage.FindModelsRequest.OrderField.ID)
                .build();

            ModelStorage.GetModelsResponse secondResponse = modelStorageService.findModels(secondRequest);

            assertDistinctModels(firstResponse.getModelsList(), secondResponse.getModelsList());
        }
    }

    @Test
    public void testRequestWithModelIds() throws Exception {
        // get model ids
        ModelStorage.FindModelsRequest firstRequest = ModelStorage.FindModelsRequest.newBuilder()
            .setLimit(3)
            .build();
        ModelStorage.GetModelsResponse firstResponse = modelStorageService.findModels(firstRequest);
        Set<Long> modelIds = firstResponse.getModelsList().stream()
            .map(ModelStorage.Model::getId)
            .collect(Collectors.toSet());

        ModelStorage.FindModelsRequest secondRequest = ModelStorage.FindModelsRequest.newBuilder()
            .addAllModelIds(modelIds)
            .build();
        ModelStorage.GetModelsResponse secondResponse = modelStorageService.findModels(secondRequest);

        assertEqualModels(firstResponse.getModelsList(), secondResponse.getModelsList());
    }

    @Test
    public void testRequestWithLimit() throws Exception {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setLimit(10)
            .build();

        ModelStorage.GetModelsResponse response = modelStorageService.findModels(request);

        assertEquals(10, response.getModelsCount());
        assertTrue(response.getModelsFound() > 0);
        assertThat(response.getNextCursorMark(), not(isEmptyString()));
    }

    @Test
    @Ignore("MBO-14091")
    public void testRequestWithLimitAndOffset() throws Exception {
        ModelStorage.FindModelsRequest request = ModelStorage.FindModelsRequest.newBuilder()
            .setLimit(10)
            .setOffset(100)
            .build();

        ModelStorage.GetModelsResponse response = modelStorageService.findModels(request);

        assertEquals(10, response.getModelsCount());
        assertTrue(response.getModelsFound() > 0);
        assertThat(response.getNextCursorMark(), isEmptyString());
    }

    /**
     * Тест проверяет, что от корректных запросов не падает метод.
     */
    @Test
    public void testSuccessfulRequests() throws Exception {
        ModelStorage.FindModelsRequest noneDescSort = ModelStorage.FindModelsRequest.newBuilder()
            .setLimit(3)
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.NONE)
            .setOrderType(ModelStorage.FindModelsRequest.OrderType.DESC)
            .build();
        ModelStorage.FindModelsRequest categorySort = ModelStorage.FindModelsRequest.newBuilder()
            .setLimit(3)
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.CATEGORY_ID)
            .build();
        ModelStorage.FindModelsRequest vendorSort = ModelStorage.FindModelsRequest.newBuilder()
            .setLimit(3)
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.VENDOR_ID)
            .build();
        ModelStorage.FindModelsRequest createdDateSort = ModelStorage.FindModelsRequest.newBuilder()
            .setLimit(3)
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.CREATED_DATE)
            .setOrderType(ModelStorage.FindModelsRequest.OrderType.DESC)
            .build();

        List<ModelStorage.FindModelsRequest> requests = Arrays.asList(noneDescSort, categorySort, vendorSort,
            createdDateSort);

        for (int i = 0; i < requests.size(); i++) {
            log.debug("Iterating over " + (i + 1) + " request");
            ModelStorage.GetModelsResponse response = modelStorageService.findModels(requests.get(i));
            assertTrue(response.getModelsFound() > 0);
        }
    }

    @Test(expected = ServiceException.class)
    public void testRequestWithSortAndCursorShouldFail() throws Exception {
        ModelStorage.FindModelsRequest sortAndCursorMaskIds = ModelStorage.FindModelsRequest.newBuilder()
            .setOrderBy(ModelStorage.FindModelsRequest.OrderField.MODIFIED_DATE)
            .setCursorMark("invalid_cursor")
            .build();

        modelStorageService.findModels(sortAndCursorMaskIds);
    }

    @Test(expected = ServiceException.class)
    public void testRequestWithOrderAndCursorShouldFail() throws Exception {
        ModelStorage.FindModelsRequest sortAndCursorMaskIds = ModelStorage.FindModelsRequest.newBuilder()
            .setOffset(10)
            .setCursorMark("invalid_cursor")
            .build();

        modelStorageService.findModels(sortAndCursorMaskIds);
    }

    @Test
    @Ignore("MBO-14091")
    public void testDeletedModels() throws Exception {
        // prepare
        ModelStorage.FindModelsRequest aliveModelRequest = ModelStorage.FindModelsRequest.newBuilder()
            .setDeleted(ModelStorage.FindModelsRequest.DeletedState.ALIVE)
            .setLimit(100)
            .build();
        ModelStorage.FindModelsRequest deletedModelRequest = ModelStorage.FindModelsRequest.newBuilder()
            .setDeleted(ModelStorage.FindModelsRequest.DeletedState.DELETED)
            .setLimit(100)
            .build();

        List<ModelStorage.Model> expectedAlive = modelStorageService.findModels(aliveModelRequest).getModelsList();
        List<ModelStorage.Model> expectedDeleted = modelStorageService.findModels(deletedModelRequest).getModelsList();

        ModelStorage.Model aliveModel = expectedAlive.get(0);
        ModelStorage.Model deletedModel = expectedDeleted.get(0);

        assertFalse(aliveModel.getDeleted());
        assertTrue(deletedModel.getDeleted());

        // do
        ModelStorage.FindModelsRequest findAllModels = ModelStorage.FindModelsRequest.newBuilder()
            .setDeleted(ModelStorage.FindModelsRequest.DeletedState.ALL)
            .addModelIds(aliveModel.getId())
            .addModelIds(deletedModel.getId())
            .build();
        ModelStorage.FindModelsRequest findAliveModels = ModelStorage.FindModelsRequest.newBuilder()
            .setDeleted(ModelStorage.FindModelsRequest.DeletedState.ALIVE)
            .addModelIds(aliveModel.getId())
            .addModelIds(deletedModel.getId())
            .build();
        ModelStorage.FindModelsRequest findDeletedModels = ModelStorage.FindModelsRequest.newBuilder()
            .setDeleted(ModelStorage.FindModelsRequest.DeletedState.DELETED)
            .addModelIds(aliveModel.getId())
            .addModelIds(deletedModel.getId())
            .build();

        List<ModelStorage.Model> all = modelStorageService.findModels(findAllModels).getModelsList();
        List<ModelStorage.Model> alive = modelStorageService.findModels(findAliveModels).getModelsList();
        List<ModelStorage.Model> deleted = modelStorageService.findModels(findDeletedModels).getModelsList();

        // assert
        List<ModelStorage.Model> expectedAll = Arrays.asList(aliveModel, deletedModel);

        assertEqualModels(expectedAll, all);
        assertEqualModels(Collections.singletonList(aliveModel), alive);
        assertEqualModels(Collections.singletonList(deletedModel), deleted);
    }

    private void assertDistinctModels(Collection<ModelStorage.Model> firstCollection,
                                      Collection<ModelStorage.Model> secondCollection) {
        Set<Long> firstModelIds = firstCollection.stream()
            .map(ModelStorage.Model::getId)
            .collect(Collectors.toSet());

        for (ModelStorage.Model model : secondCollection) {
            if (firstModelIds.contains(model.getId())) {
                throw new AssertionError("First request already contains model with id: " + model.getId());
            }
        }
    }

    private void assertEqualModels(List<ModelStorage.Model> expected, List<ModelStorage.Model> actual) {
        assertEquals(expected, actual);
    }
}
