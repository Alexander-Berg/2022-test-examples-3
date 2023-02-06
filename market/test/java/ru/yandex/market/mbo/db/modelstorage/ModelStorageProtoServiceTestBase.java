package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Before;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.user.TestAutoUser;

import java.util.Random;

import static org.mockito.Mockito.mock;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 03.09.2018
 */
public class ModelStorageProtoServiceTestBase {

    protected static final long CATEGORY_ID = 621163604;

    private static final long SEED = 991144508L;

    protected ModelStorageProtoService protoService;

    protected ModelStorageHealthService modelStorageHealthService;

    protected StatsModelStorageService storageService;

    protected ModelStoreInterface modelStore;

    protected Random random;

    protected AutoUser autoUser;

    protected OperationStatus okStatus;

    @Before
    public void before() {
        modelStorageHealthService = mock(ModelStorageHealthService.class);
        storageService = mock(StatsIndexedModelStorageService.class);
        modelStore = mock(ModelStoreInterface.class);

        random = new Random(SEED);
        autoUser = TestAutoUser.create();

        protoService = new ModelStorageProtoService();
        protoService.setStorageService(storageService);
        protoService.setModelStorageHealthService(modelStorageHealthService);
        protoService.setAutoUser(autoUser);

        okStatus = new OperationStatus(OperationStatusType.OK, OperationType.CHANGE, -1L);
    }

    protected ModelStorage.Model.Builder model() {
        return ModelStorage.Model.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setId(random.nextLong());
    }
}
