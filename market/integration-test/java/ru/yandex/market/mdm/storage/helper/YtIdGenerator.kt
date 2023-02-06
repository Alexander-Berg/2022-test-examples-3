package ru.yandex.market.mdm.storage.helper

import ru.yandex.common.util.db.MultiIdGenerator
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.market.mbo.yt.utils.UnstableInit
import java.util.concurrent.TimeUnit

class YtIdGenerator(
    private val unstableYt: UnstableInit<Yt>,
    private val sequencePath: YPath
) : MultiIdGenerator {

    private var id: Long = 0L
    private var limitId: Long = 0L
    private lateinit var yt: Yt

    fun init() {
        yt = unstableYt.get(30, TimeUnit.SECONDS)
        preloadIds(BATCH_SIZE)
    }

    override fun getId(): Long {
        if (id < limitId) {
            return id++
        }
        preloadIds(BATCH_SIZE)
        return id++
    }

    override fun getIds(count: Int): List<Long> {
        return (0..count).map{ getId() }
    }

    private fun preloadIds(amount: Int) {
        id = if (yt.cypress().list(sequencePath).isNotEmpty()) {
            val node = yt.cypress().list(sequencePath)[0]
            yt.cypress().remove(sequencePath.child(node.stringValue()))
            node.stringValue().toLong()
        } else {
            DEFAULT_ID
        }

        limitId = id + amount
        yt.cypress().create(sequencePath.child(limitId.toString()), CypressNodeType.STRING)
    }

    companion object {
        const val DEFAULT_ID = 5075464322L
        const val BATCH_SIZE = 100000
    }
}
