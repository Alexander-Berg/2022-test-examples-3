// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/data-structures/queue.ts >>>

package com.yandex.xplat.testopithecus.common

import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.pop

public open class Queue<T> {
    var q1: YSArray<T> = mutableListOf()
    var q2: YSArray<T> = mutableListOf()
    open fun push(item: T): Unit {
        this.q1.add(item)
    }

    open fun pop(): Unit {
        this.move()
        this.q2.pop()
    }

    open fun clear(): Unit {
        this.q1 = mutableListOf()
        this.q2 = mutableListOf()
    }

    open fun size(): Int {
        return this.q1.size + this.q2.size
    }

    open fun front(): T {
        this.move()
        return this.q2[this.q2.size - 1]
    }

    private fun move(): Unit {
        if (this.q2.size > 0) {
            return
        }
        while (this.q1.size > 0) {
            val element = this.q1.pop()
            this.q2.add(element!!)
        }
    }

}

