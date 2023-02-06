package ru.yandex.market.pricingmgmt.config.tvm

import ru.yandex.passport.tvmauth.BlackboxEnv
import ru.yandex.passport.tvmauth.CheckedServiceTicket
import ru.yandex.passport.tvmauth.CheckedUserTicket
import ru.yandex.passport.tvmauth.ClientStatus
import ru.yandex.passport.tvmauth.TicketStatus
import ru.yandex.passport.tvmauth.TvmClient
import ru.yandex.passport.tvmauth.roles.Roles

class DummyTvmClient : TvmClient {

    companion object {
        val VALID_TVM_TICKET = "valid_tvm_ticket"
        val EXPIRED_TVM_TICKET = "expired_tvm_ticket"
    }

    private var srcTvmId: Int = 1212

    override fun close() {
    }

    override fun getStatus(): ClientStatus {
        return ClientStatus(ClientStatus.Code.OK, "OK")
    }

    override fun getServiceTicketFor(alias: String?): String {
        return "123"
    }

    override fun getServiceTicketFor(tvmId: Int): String {
        return "321"
    }

    override fun checkServiceTicket(ticketBody: String): CheckedServiceTicket {
        when (ticketBody) {
            VALID_TVM_TICKET -> return getTicket(TicketStatus.OK)
            EXPIRED_TVM_TICKET -> return getTicket(TicketStatus.EXPIRED)
            else -> return getTicket(TicketStatus.MALFORMED)
        }
    }

    override fun checkUserTicket(ticketBody: String?): CheckedUserTicket {
        throw NotImplementedError("not used")
    }

    override fun checkUserTicket(ticketBody: String?, overridedBbEnv: BlackboxEnv?): CheckedUserTicket {
        throw NotImplementedError("not used")
    }

    override fun getRoles(): Roles =
        throw UnsupportedOperationException("Not implemented")

    fun mockSourceTvmId(sourceTvmId: Int) {
        srcTvmId = sourceTvmId
    }

    private fun getTicket(ticketStatus: TicketStatus): CheckedServiceTicket {
        return CheckedServiceTicket(ticketStatus, "tvm dummy client", srcTvmId, 0)
    }
}
