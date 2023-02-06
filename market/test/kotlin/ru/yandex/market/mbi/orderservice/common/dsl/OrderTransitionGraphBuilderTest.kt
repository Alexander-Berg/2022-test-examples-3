package ru.yandex.market.mbi.orderservice.common.dsl

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus.*

class OrderTransitionGraphBuilderTest {

    @Test
    fun `verify that duplicating transitions are not allowed`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            transitions {
                transition(PLACING to RESERVED) {
                    require {
                        it.actor == Actor.BUYER
                    }
                    onTransition {
                        + { ctx: TransitionContext -> println(ctx) }
                    }
                }
                transition(PLACING to RESERVED) {}
            }
        }.withMessage("Edge PLACING -> RESERVED is already in the graph")
    }

    @Test
    fun `verify that self loops are not allowed`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            transitions {
                transition(PLACING to PLACING) {}
            }
        }.withMessageContaining("Cannot add self-loop edge")
    }

    @Test
    fun `verify hasTransition`() {
        val graph = transitions {
            transition(PLACING to RESERVED) {}
            transition(RESERVED to UNPAID) {}
            transition(UNPAID to PENDING) {}
            transition(PENDING to PROCESSING) {}
            transition(PROCESSING to CANCELLED_IN_PROCESSING) {}
        }

        // no direct edge
        assertThat(graph.hasTransition(from = UNPAID, to = PROCESSING, transitive = false)).isFalse
        // has transitive path
        assertThat(graph.hasTransition(from = UNPAID, to = PROCESSING, transitive = true)).isTrue
    }
}
