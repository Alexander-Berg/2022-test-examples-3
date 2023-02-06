package ru.yandex.direct.core.copyentity.graph

import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.reflections.Reflections
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.copyentity.EntityGraphNavigator
import ru.yandex.direct.core.copyentity.EntityService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.model.Entity
import ru.yandex.direct.model.Relationship

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class CopyEntityGraphTest {

    @Autowired
    private lateinit var entityGraphNavigator: EntityGraphNavigator

    @Test
    fun `every Entity has an EntityService`() {
        val reflections = Reflections("ru.yandex.direct.core.entity")

        val entities: Set<Class<out Entity<*>>> = reflections.getSubTypesOf(Entity::class.java)

        val baseEntities: Set<Class<out Entity<*>>> = entities
            .map { entity -> entityGraphNavigator.getClosestToEntityAncestor(entity) }
            .toSet()

        assertSoftly {
            baseEntities.forEach { entity ->
                it.assertThatCode {
                    val nav: EntityService<Entity<Any>, Any> = entityGraphNavigator.getEntityService(entity)
                }
                    .describedAs("Entity %s does not have an entity service", entity)
                    .doesNotThrowAnyException()
            }
        }
    }

    @Test
    fun `every Relationship has an RelationshipService`() {
        val reflections = Reflections("ru.yandex.direct.core.entity")

        val relationships: Set<Class<out Relationship<*, *, *>>> = reflections.getSubTypesOf(Relationship::class.java)

        assertSoftly {
            relationships.forEach { relationship ->
                it.assertThatCode {
                    entityGraphNavigator.getRelationshipServiceByClass(
                        relationship as Class<out Relationship<Entity<Any>, Entity<Any>, Any>>)
                }
                    .describedAs("Relationship %s does not have an RelationshipService", relationship)
                    .doesNotThrowAnyException()
            }
        }
    }
}
