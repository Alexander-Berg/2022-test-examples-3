package ru.yandex.market.partner.status.status.resolver.impl

import kotlinx.coroutines.runBlocking
import ru.yandex.market.partner.status.status.resolver.UpdateResolverService
import ru.yandex.market.partner.status.status.resolver.UpdateResolverTaskQueue
import ru.yandex.market.partner.status.status.resolver.model.ProgramResolverType
import java.util.concurrent.Executors

/**
 * Реализация [UpdateResolverTaskQueue] для тестов. Проксирует все методы в оригинальную очередь,
 * но сразу в методе добавления вызывает расчет.
 * То есть, расчет происходит во время добавления, а не когда-то в будущем в фоне.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TestUpdateResolverTaskQueue(
    updateResolverServices: List<UpdateResolverService>,
    private val base: UpdateResolverTaskQueue
) : UpdateResolverTaskQueue by base {

    private val updateResolverCalculatorsStarter = UpdateResolverCalculatorsStarter(
        updateResolverServices,
        this,
        Executors.newSingleThreadExecutor(),
        Executors.newSingleThreadExecutor(),
        10
    )

    override fun add(resolverType: ProgramResolverType, chunk: Int, partnerIds: List<Long>) {
        base.add(resolverType, chunk, partnerIds)
        runBlocking {
            updateResolverCalculatorsStarter.doTasks(chunk)
        }
    }
}
