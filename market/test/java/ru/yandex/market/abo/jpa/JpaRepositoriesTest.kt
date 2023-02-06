package ru.yandex.market.abo.jpa

import java.io.Serializable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.test.util.AopTestUtils
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.jpa.PgJpaRepository
import ru.yandex.market.abo.cpa.order.delivery.ShopOrderDeliveryRepo
import ru.yandex.market.abo.cpa.order.service.OrderOperationRepo

/**
 * @author artemmz
 * @date 28.05.18.
 */
class JpaRepositoriesTest @Autowired constructor(
    private val repositories: List<PgJpaRepository<*, out Serializable>>,
    orderOperRepository: OrderOperationRepo,
    shopOrderDeliveryRepo: ShopOrderDeliveryRepo
) : EmptyTest() {
    private val excludes = hashSetOf<PgJpaRepository<*, out Serializable>>(
        orderOperRepository, shopOrderDeliveryRepo
    )

    @Test
    fun daoTest() {
        repositories.asSequence().filter { !excludes.contains(it) }.forEach { repo ->
            try {
                repo.findAll()
            } catch (e: Throwable) {
                throw RuntimeException("error with repo for class " + extractRepoDomainClass(repo), e)
            }
        }
    }

    /**
     * if u know how to implement it better - go ahead pls.
     * I didn't manage to use anything from http://roufid.com/retreive-spring-jpa-repository-domain-type/
     * cause we don't know actual classes of repos in test.
     */
    private fun extractRepoDomainClass(repoBean: PgJpaRepository<*, out Serializable?>): Class<*> {
        return try {
            val realRepo = AopTestUtils.getTargetObject<SimpleJpaRepository<Any, Serializable>>(repoBean)
            val domainClassGetter = realRepo.javaClass.getDeclaredMethod("getDomainClass")
            domainClassGetter.isAccessible = true
            domainClassGetter.invoke(realRepo) as Class<*>
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
