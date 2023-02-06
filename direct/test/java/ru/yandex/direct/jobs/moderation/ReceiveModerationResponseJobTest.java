package ru.yandex.direct.jobs.moderation;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.moderation.repository.bulk_update.BulkUpdateHolder;
import ru.yandex.direct.core.entity.moderation.service.receiving.BaseModerationReceivingService;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.moderation.processor.handlers.BaseModerationHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JobsTest
@ExtendWith(SpringExtension.class)
class ReceiveModerationResponseJobTest {

    @Autowired
    private ReceiveModerationResponseJob first;

    @Autowired
    private ReceiveModerationResponseJob second;

    /**
     * Проверяет, что разные инстансы джобы используют разные наборы BulkUpdateHolder-ов (не используют совместно один
     * и тот же экземпляр BulkUpdateHolder). Также косвенно проверяет, что у хэндлеров
     * (наследников {@link BaseModerationHandler}) и сервисов (наследников {@link BaseModerationReceivingService})
     * обработки вердиктов проставлена аннотация {@code @Scope(SCOPE_PROTOTYPE)}, и разные джобы не используют совместно
     * один и тот же хэндлер или сервис обработки вердиктов.
     *
     * Тест достаточно "хрупкий", очень сильно завязан на реализацию хэндлеров и сервисов обработки вердиктов, и будет
     * ломаться при изменениях реализаций этих классов.
     * Но он полезный, потому что позволяет отловить ошибки, которые по-другому поймать очень сложно (пару раз удавалось
     * поймать, в продакшене, после того как загорались мониторинги).
     **/
    @Test
    void testBulkUpdateHoldersInstantiation() throws ReflectiveOperationException {
        var firstJobBulkUpdateHolders = getBulkUpdateHolders(first);
        var secondJobBulkUpdateHolders = getBulkUpdateHolders(second);

        assertAll(
                () -> assertThat(firstJobBulkUpdateHolders).isNotEmpty(),
                () -> assertThat(secondJobBulkUpdateHolders).isNotEmpty(),
                () -> assertThat(firstJobBulkUpdateHolders.size()).isEqualTo(firstJobBulkUpdateHolders.size()),
                () -> firstJobBulkUpdateHolders.forEach(holder ->
                            assertThat(secondJobBulkUpdateHolders).doesNotContain(holder)),
                () -> secondJobBulkUpdateHolders.forEach(holder ->
                        assertThat(firstJobBulkUpdateHolders).doesNotContain(holder))
        );
    }

    private List<BulkUpdateHolder> getBulkUpdateHolders(ReceiveModerationResponseJob job)
            throws ReflectiveOperationException {

        var handlersField = ReceiveModerationResponseJob.class.getDeclaredField("handlers");
        handlersField.setAccessible(true);
        var handlers = (List<?>) handlersField.get(job);

        List<BulkUpdateHolder> result = new ArrayList<>();

        for (var handler : handlers) {
            var serviceField = BaseModerationHandler.class.getDeclaredField("moderationReceivingService");
            serviceField.setAccessible(true);
            var service = serviceField.get(handler);

            if (service instanceof BaseModerationReceivingService) {
                var bulkUpdateHolderField =
                        BaseModerationReceivingService.class.getDeclaredField("bulkUpdateHolder");
                bulkUpdateHolderField.setAccessible(true);
                result.add((BulkUpdateHolder) bulkUpdateHolderField.get(service));
            }
        }

        return result;
    }
}
