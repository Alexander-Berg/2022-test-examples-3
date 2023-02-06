package ru.yandex.direct.ess.config;

import java.util.List;

import ru.yandex.direct.ess.common.converter.LogicObjectWithSystemInfoConverter;
import ru.yandex.direct.ess.common.models.BaseLogicObject;
import ru.yandex.direct.ess.common.models.LogicObjectListWithInfo;

import static org.assertj.core.api.AssertionsForClassTypes.fail;

public class ConverterUtils {

    private ConverterUtils() {
    }

    private static final long DEFAULT_REQ_ID = 1L;
    private static final long DEFAULT_TIMESTAMP = 1554912655L;
    private static final String DEFAULT_GTID = "";
    private static final Long DEFAULT_SEQ_NO = 1L;
    private static final String DEFAULT_SOURCE = "";

    public static <T extends BaseLogicObject> List<T> logicObjectsSerializeDeserialize(List<T> objects) {
        LogicObjectWithSystemInfoConverter<T> converter =
                new LogicObjectWithSystemInfoConverter<>(objects.get(0).getClass());
        LogicObjectListWithInfo<T> logicObjectListWithInfo = new LogicObjectListWithInfo.Builder<T>()
                .withLogicObjects(objects)
                .build();
        byte[] jsonObject;
        try {
            jsonObject = converter.toJson(logicObjectListWithInfo);
        } catch (Exception ex) {
            fail("Failure to serialize objects " + objects, ex);
            throw ex;
        }
        LogicObjectListWithInfo<T> logicObjectFromJson;
        try {
            logicObjectFromJson = converter.fromJson(jsonObject);
        } catch (Exception ex) {
            fail("Failure to deserialize objects " + objects, ex);
            throw ex;
        }
        return logicObjectFromJson.getLogicObjectsList();
    }
}
