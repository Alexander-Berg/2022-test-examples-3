package ru.yandex.market.tpl.courier.data.feature.user

import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.tpl.courier.arch.fp.Exceptional
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.data.feature.datetime.DurationMapper
import ru.yandex.market.tpl.courier.domain.feature.user.UserProperties
import ru.yandex.market.tpl.courier.domain.feature.user.userPropertiesTestInstance
import ru.yandex.market.tpl.courier.extensions.successWith
import java.time.Duration

class UserPropertiesMapperTest {

    private val durationMapper = mockk<DurationMapper> {
        every { mapFromSeconds(any()) } returns success(userPropertiesTestInstance().locationCollectInterval!!)
    }
    private val mapper = UserPropertiesMapper(durationMapper)

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("data")
    fun `Результат должен совпадать с ожидаемым`(
        value: UserPropertiesDto,
        match: Matcher<Exceptional<UserProperties>>,
    ) {
        val mapped = mapper.map(value)
        mapped should match
    }

    companion object {

        @JvmStatic
        fun data() = listOf(
            args(
                value = userPropertiesDtoTestInstance(
                    isLifePosEnabled = false,
                ),
                matchResult = successWith(
                    userPropertiesTestInstance(
                        isLifePosEnabled = false,
                    ),
                ),
            ),

            args(
                value = userPropertiesDtoTestInstance(
                    isLockerCellSizeSelectionEnabled = null,
                ),
                matchResult = successWith(
                    userPropertiesTestInstance(
                        isLockerCellSizeSelectionEnabled = null,
                    )
                )
            ),

            args(
                value = userPropertiesDtoTestInstance(
                    isLockerCellSizeSelectionEnabled = true,
                ),
                matchResult = successWith(
                    userPropertiesTestInstance(
                        isLockerCellSizeSelectionEnabled = true,
                    )
                )
            ),

            args(
                value = userPropertiesDtoTestInstance(
                    isLockerCellSizeSelectionEnabled = false,
                ),
                matchResult = successWith(
                    userPropertiesTestInstance(
                        isLockerCellSizeSelectionEnabled = false,
                    )
                )
            ),

            args(
                value = userPropertiesDtoTestInstance(
                    isCourierTrainingEnabled = true,
                ),
                matchResult = successWith(
                    userPropertiesTestInstance(
                        isCourierTrainingEnabled = true,
                    )
                )
            ),

            args(
                value = userPropertiesDtoTestInstance(
                    isCourierTrainingEnabled = false,
                ),
                matchResult = successWith(
                    userPropertiesTestInstance(
                        isCourierTrainingEnabled = false,
                    )
                )
            ),

            args(
                value = userPropertiesDtoTestInstance(
                    isCourierTrainingEnabled = null,
                ),
                matchResult = successWith(
                    userPropertiesTestInstance(
                        isCourierTrainingEnabled = null,
                    )
                )
            ),
        )

        private fun args(
            value: UserPropertiesDto,
            matchResult: Matcher<Exceptional<UserProperties>>,
        ): Array<Any?> = arrayOf(value, matchResult)
    }
}