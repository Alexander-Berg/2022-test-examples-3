package ru.yandex.market.logistics.calendaring.util

import org.hibernate.resource.jdbc.spi.StatementInspector
import java.lang.Thread.currentThread

class QueriesCountInspector : StatementInspector {
    companion object {
        private val holder: ThreadLocal<MutableList<QueryInfo>> = ThreadLocal.withInitial { ArrayList() }
        fun reset() {
            holder.get().clear()
        }

        fun getCount(): Int {
            return holder.get().size
        }

        fun getQueries(): List<QueryInfo> {
            return java.util.List.copyOf(holder.get())
        }

        private fun getSource(): String? {
            val thisClassName = QueriesCountInspector::class.java.simpleName
            return currentThread().stackTrace.find { element ->
                !element.className.contains(thisClassName) && element.className.contains("yandex")
            }?.let { getFileName(it) + ':' + it.lineNumber }
                ?: "Unknown source"
        }

        private fun getFileName(ste: StackTraceElement): String {
            return ste.fileName?.let { name -> name.split("\\.")[0] } ?: "Unknown file"
        }
    }

    override fun inspect(sql: String?): String? {
        holder.get().add(QueryInfo(sql, getSource()))
        return sql
    }

}

data class QueryInfo(
    val sql: String? = null,
    val source: String? = null
)
