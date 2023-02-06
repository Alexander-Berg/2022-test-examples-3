package ru.yandex.market.mdm.lib.application

import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer

/**
 * Класс с "правильной" общей шапкой аннотаций для интеграционных тестов,
 * чтобы создавался и переиспользовался один контекст.
 *
 * @author yuramalinov
 * @created 16.04.18
 */
@RunWith(SpringRunner::class)
@ContextConfiguration(initializers = [IntegrationTestSourcesInitializer::class, PGaaSZonkyInitializer::class])
@Transactional
abstract class MdmBaseIntegrationDbTestClass
