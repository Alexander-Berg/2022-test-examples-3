package ru.yandex.market.wms.picking.usecases

import ru.yandex.market.resources.ResourceManager

class ResourceManagerStub : ResourceManager {
    override fun getString(stringId: Int): String = ""

    override fun getString(stringId: Int, vararg args: Any): String = ""
}
