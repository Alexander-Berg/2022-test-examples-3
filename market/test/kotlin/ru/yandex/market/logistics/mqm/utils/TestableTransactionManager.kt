package ru.yandex.market.logistics.mqm.utils

import org.slf4j.LoggerFactory
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus

class TestableTransactionManager: PlatformTransactionManager {
    private val log = LoggerFactory.getLogger(this::class.java)!!
    var commitsNumber = 0
        private set

    var rollbacksNumber = 0
        private set

    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus? {
        log.info("transaction started, $definition")
        return null
    }

    override fun commit(status: TransactionStatus?) {
        log.info("transaction commit")
        commitsNumber++
    }

    override fun rollback(status: TransactionStatus?) {
        log.info("transaction rollback")
        rollbacksNumber++
    }

    fun reset(){
        commitsNumber = 0
        rollbacksNumber = 0
    }
}
