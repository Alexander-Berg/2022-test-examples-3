package ru.yandex.market.mboc.common.dict.backgroundaction;

import java.io.IOException;

import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.lightmapper.test.IntGenericMapperRepositoryMock;

/**
 * @author yuramalinov
 * @created 14.05.18
 */
public class BackgroundActionRepositoryMock
    extends IntGenericMapperRepositoryMock<BackgroundAction> implements BackgroundActionRepository {

    public BackgroundActionRepositoryMock() {
        super(BackgroundAction::setId, BackgroundAction::getId);
    }

    @Override
    protected void validate(BackgroundAction instance) {
        // Validate serializability of items
        try {
            String result = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(instance.getResult());
            JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(result, Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markCancel(int actionId, boolean cancel) {
        var backgroundAction = findByIdForUpdate(actionId);
        backgroundAction.setCancelRequested(cancel);
        update(backgroundAction);
    }
}
