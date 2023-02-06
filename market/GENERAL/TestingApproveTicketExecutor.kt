package ru.yandex.market.abo.tms.premod.verticalshare

import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import ru.yandex.market.abo.core.premod.PremodManager
import ru.yandex.market.abo.core.premod.PremodTicketService
import ru.yandex.market.abo.core.premod.model.PremodCheckType
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus
import ru.yandex.market.abo.core.premod.model.PremodTicketSubstatus
import ru.yandex.market.abo.util.FakeUsers
import ru.yandex.market.tms.quartz2.model.VerboseExecutor
import ru.yandex.market.tms.quartz2.spring.CronTrigger

@Profile("!production")
@CronTrigger(cronExpression = "0 0/10 * * * ?", description = "Генерация тикетов на лайт-преподерацию ТВ")
class TestingApproveTicketExecutor : VerboseExecutor() {

    @Autowired
    private lateinit var premodTicketService: PremodTicketService

    @Autowired
    private lateinit var premodManager: PremodManager

    override fun doRealJob(context: JobExecutionContext?) {
        var list = premodTicketService.loadRunningTicketsByTypes(PremodCheckType.GOODS_TYPES)
        list.forEach { t ->
            t.substatus = PremodTicketSubstatus.PASS
            t.status = PremodTicketStatus.PASS
            t.userId = FakeUsers.PREMOD_AUTO.id
            premodManager.updatePremodTicket(t)
        }
    }

}
