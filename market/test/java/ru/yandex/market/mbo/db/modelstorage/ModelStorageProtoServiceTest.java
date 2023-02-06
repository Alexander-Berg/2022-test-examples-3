package ru.yandex.market.mbo.db.modelstorage;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.08.2018
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ModelStorageProtoServiceSaveModelsGroupTest.class,
    ModelStorageProtoServiceFindModelsTest.class,
    ModelStorageProtoServiceImageUploadTest.class,
    ModelStorageProtoServiceSaveContextTest.class,
    ModelStorageProtoServiceGetModelsTest.class,
    ModelStorageProtoServiceRemoveModelsTest.class
})
public class ModelStorageProtoServiceTest {

}
