package ru.yandex.market.logistics.lom.controller.tracker;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;

@ParametersAreNonnullByDefault
@DatabaseSetup({
    "/billing/before/billing_service_products.xml",
    "/controller/tracker/before/setup_use_order_external_id_as_track_code.xml",
})
class UseOrderExternalIdAsTrackCodeTrackerNotificationControllerTest extends TrackerNotificationControllerTest {
}
