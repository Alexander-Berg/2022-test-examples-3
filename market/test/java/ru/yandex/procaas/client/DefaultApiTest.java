/*
 * Yandex Taxi processing Service
 * Yandex Taxi processing Service
 *
 * The version of the OpenAPI document: v1
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package ru.yandex.procaas.client;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.procaas.model.Checkpoint;
import ru.yandex.procaas.model.CreateEventBatch;
import ru.yandex.procaas.model.CurrentState;
import ru.yandex.procaas.model.EventBatchCreated;
import ru.yandex.procaas.model.EventCreated;
import ru.yandex.procaas.model.QueueData;
import ru.yandex.procaas.model.TaskCreated;

/**
 * API tests for DefaultApi
 */
@Disabled
public class DefaultApiTest {

    private final DefaultApi api = new DefaultApi();


    /**
     * Добавить несколько событий пачкой. Атомарность не поддерживается. Гарантируется соблюдения порядка событий
     * внутри пачки и идемпотентность вставки. Успешный статус возвращается только если вставленны (или уже были
     * вставленны) все события из пачки запроса.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void v1ScopeCreateEventBatchPostTest() {
        String scope = null;
        CreateEventBatch body = null;
        EventBatchCreated response = api.v1ScopeCreateEventBatchPost(scope, body);

        // TODO: test validations
    }

    /**
     * Получить чекпоинт упавшего события. Чекпоинт - &#x60;shared_state&#x60; и стадия, с которой будет перезапущен
     * пайплайн в случае ошибки.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void v1ScopeQueueCheckpointGetTest() {
        String scope = null;
        String queue = null;
        String itemId = null;
        Checkpoint response = api.v1ScopeQueueCheckpointGet(scope, queue, itemId);

        // TODO: test validations
    }

    /**
     * Добавить новое событие в очередь событий.  Примеры употребления путей: POST /v1/taxi/orders/create-event POST
     * /v1/taxi/transactions/create-event POST /v1/lavka/invoices/create-event POST
     * /v1/lavka/support-tickets/create-event
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void v1ScopeQueueCreateEventPostTest() {
        String scope = null;
        String queue = null;
        String itemId = null;
        String xIdempotencyToken = null;
        OffsetDateTime due = null;
        Integer extraOrderKey = null;
        Object body = null;
        EventCreated response = api.v1ScopeQueueCreateEventPost(scope, queue, itemId, xIdempotencyToken, due,
                extraOrderKey, body);

        // TODO: test validations
    }

    /**
     * Получить текущее состояние для данного &#x60;item_id&#x60;. Текущим считается состояние из &#x60;
     * state-manager&#x60;&#39;a, которое будет в нем на момент после последнего обработанного события.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void v1ScopeQueueCurrentStateGetTest() {
        String scope = null;
        String queue = null;
        String itemId = null;
        Boolean allowRestore = null;
        String terminalEventId = null;
        CurrentState response = api.v1ScopeQueueCurrentStateGet(scope, queue, itemId, allowRestore, terminalEventId);

        // TODO: test validations
    }

    /**
     * Получить очередь событий.
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void v1ScopeQueueEventsGetTest() {
        String scope = null;
        String queue = null;
        String itemId = null;
        Boolean allowRestore = null;
        QueueData response = api.v1ScopeQueueEventsGet(scope, queue, itemId, allowRestore);

        // TODO: test validations
    }

    /**
     * Запустить новую задачу в пайплайн  Примеры употребления путей: POST /v1/taxi/orders/run-pipeline
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void v1ScopeSinglePipelineRunPipelinePostTest() {
        String scope = null;
        String singlePipeline = null;
        String itemId = null;
        String xIdempotencyToken = null;
        OffsetDateTime due = null;
        Object body = null;
        TaskCreated response = api.v1ScopeSinglePipelineRunPipelinePost(scope, singlePipeline, itemId,
                xIdempotencyToken, due, body);

        // TODO: test validations
    }

}