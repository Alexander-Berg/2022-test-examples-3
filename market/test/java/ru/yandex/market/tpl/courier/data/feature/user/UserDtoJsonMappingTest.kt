package ru.yandex.market.tpl.courier.data.feature.user

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.data.parse
import ru.yandex.market.tpl.courier.testApplication

@RunWith(RobolectricTestRunner::class)
class UserDtoJsonMappingTest {

    @Test
    fun `UserDto is properly parsed`() {
        val parser = testApplication.component.jsonMapper
        val result = parser.parse<UserDto>(
            """
              {
                "id": 42,
                "uid": 1234567890,
                "email": "test@email.kek",
                "name": "Иван Иванов",
                "role": "ADMIN",
                "properties": {
                    "address_removed_from_routing_list": true,
                    "arrived_to_rp_distance_can_skip": true,
                    "arrived_to_rp_distance_filter_enabled": false,
                    "call_to_recipient_enabled": true,
                    "custom_version_number": "0",
                    "demo_enabled": true,
                    "feature_allow_switch": true,
                    "feature_life_pos_enabled": true,
                    "fixed_order_of_task_enabled": true,
                    "phone_removed_from_routing_list": true,
                    "pickup_accept_not_sorted_enabled": true,
                    "rerouting_enabled": true,
                    "rerouting_with_precise_interval_enabled": true,
                    "sequential_delivery_enabled": true,
                    "service_time_multiplier_CAR": 1,
                    "service_time_multiplier_NONE": 1,
                    "shared_service_time_multiplier_CAR": 1,
                    "shared_service_time_multiplier_NONE": 1,
                    "travel_time_multiplier_CAR": 1,
                    "travel_time_multiplier_CAR_enabled": true,
                    "travel_time_multiplier_NONE": 1,
                    "travel_time_multiplier_NONE_enabled": true,
                    "feature_support_chats_enabled": false,
                    "locker_order_rescanning_enabled": false,
                    "locker_cell_size_selection_enabled": false,
                    "courier_training_enabled": false,
                    "collecting_location_report_interval_seconds": 5,
                    "sending_location_report_interval_seconds": 30,
                }
            }
            """
        ).orThrow()

        val expected = UserDto(
            id = 42L,
            uid = 1234567890,
            email = "test@email.kek",
            name = "Иван Иванов",
            role = "ADMIN",
            createdAt = null,
            properties = userPropertiesDtoTestInstance(
                isLifePosEnabled = true,
                isLockerOrderRescanningEnabled = false,
                isLockerCellSizeSelectionEnabled = false,
                isSupportChatsFeatureEnabled = false,
                isCourierTrainingEnabled = false,
                needClearOfflineScheduler = false,
                locationCollectInterval = 5,
                locationReportInterval = 30,
            )
        )

        result shouldBe expected
    }
}
