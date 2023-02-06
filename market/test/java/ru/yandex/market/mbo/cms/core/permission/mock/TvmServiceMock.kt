package ru.yandex.market.mbo.cms.core.permission.mock

import ru.yandex.market.mbo.cms.core.service.tvm.TvmService

class TvmServiceMock : TvmService {
    override fun getServiceId(ticketBody: String?): Int? {
        return null
    }

    override fun getUserId(ticketBody: String?): Long? {
        return 1
    }

    override fun getServiceTicketFor(tvmId: Int): String {
        return "";
    }
}
