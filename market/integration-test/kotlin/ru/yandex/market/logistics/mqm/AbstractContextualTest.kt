package ru.yandex.market.logistics.mqm

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.hibernate.id.enhanced.PooledOptimizer
import org.hibernate.id.enhanced.SequenceStyleGenerator
import org.hibernate.metamodel.internal.MetamodelImpl
import org.hibernate.persister.entity.EntityPersister
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import org.springframework.util.ReflectionUtils
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.application.properties.utils.Environments
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.configuration.IntegrationTestConfiguration
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener
import ru.yandex.market.request.trace.RequestContext
import ru.yandex.market.request.trace.RequestContextHolder
import java.lang.reflect.Field
import java.time.Instant
import java.time.LocalDateTime
import javax.persistence.EntityManager


@ExtendWith(
    SpringExtension::class,
    SoftAssertionsExtension::class,
)
@SpringBootTest(
    classes = [IntegrationTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.config.name=integration-test"]
)
@AutoConfigureMockMvc
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    ResetDatabaseTestExecutionListener::class,
    TransactionalTestExecutionListener::class,
    DbUnitTestExecutionListener::class,
    MockitoTestExecutionListener::class,
    ResetMocksTestExecutionListener::class
)
@CleanDatabase
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader::class)
@ActiveProfiles(Environments.INTEGRATION_TEST)
abstract class AbstractContextualTest {
    companion object {
        const val REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
        const val TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + ParameterizedTest.INDEX_PLACEHOLDER + "] {0}"
    }

    @Autowired
    protected lateinit var clock: TestableClock

    @InjectSoftAssertions
    lateinit var softly: SoftAssertions

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun mockTest() {
        RequestContextHolder.setContext(RequestContext(REQUEST_ID))
    }

    protected fun String.toInstant(): Instant =
        LocalDateTime.parse(this).atZone(DateTimeUtils.MOSCOW_ZONE).toInstant()

    //Хибер прихранивает 50 id в памяти, поэтому сгенерированный id зависит от количества и порядка запущенных тестов.
    //Нормального метода сброса этого кэша нет. Поэтому приходится использовать такой костыль с рефлекшеном
    protected fun resetSequenceIdGeneratorCache(entityClass: Class<*>, entityManager: EntityManager) {
        val metamodel = entityManager.metamodel as MetamodelImpl
        val entityPersister = metamodel.entityPersisters().values.first { it.mappedClass == entityClass }
        val optimizer = (getEligibleOptimiser(entityPersister)
            ?: throw RuntimeException("Can't reset sequence cache of ${entityClass.simpleName}"))
        val noTenantStateField: Field = ReflectionUtils.findField(PooledOptimizer::class.java, "noTenantState")!!
        noTenantStateField.isAccessible = true
        noTenantStateField.set(optimizer, null)
    }

    private fun getEligibleOptimiser(entityPersister: EntityPersister): PooledOptimizer? {
        if (entityPersister.hasIdentifierProperty()
            && entityPersister.identifierGenerator is SequenceStyleGenerator
        ) {
            val generator = entityPersister.identifierGenerator as SequenceStyleGenerator
            if (generator.optimizer is PooledOptimizer) {
                return generator.optimizer as PooledOptimizer
            }
        }
        return null
    }
}
