package ru.yandex.market.checkout.checkouter.health;

import com.google.common.collect.ImmutableSet;

import ru.yandex.market.checkout.checkouter.controllers.OrderMigrateController;
import ru.yandex.market.checkout.checkouter.controllers.documentation.status.StatusDocumentationController;
import ru.yandex.market.checkout.checkouter.controllers.lock.LockMvpController;
import ru.yandex.market.checkout.checkouter.controllers.oms.PaymentSupportController;
import ru.yandex.market.checkout.checkouter.controllers.oms.ReceiptSupportController;
import ru.yandex.market.checkout.checkouter.controllers.service.ArchiveController;
import ru.yandex.market.checkout.checkouter.controllers.service.EventsExportSupportController;
import ru.yandex.market.checkout.checkouter.controllers.service.PublicSupportController;
import ru.yandex.market.checkout.checkouter.controllers.service.PushExtraSubsidyController;
import ru.yandex.market.checkout.checkouter.controllers.service.QueuedCallsController;
import ru.yandex.market.checkout.checkouter.controllers.service.ShootingController;
import ru.yandex.market.checkout.checkouter.controllers.service.SupportController;
import ru.yandex.market.checkout.checkouter.controllers.service.TaskV2Controller;
import ru.yandex.market.checkout.checkouter.controllers.service.ZooTaskConfigController;

public class CheckouterAlertAnnotationsTest extends AlertAnnotationsTestBase {

    public CheckouterAlertAnnotationsTest() {
        super(ImmutableSet.of(
                ShootingController.class,
                SupportController.class,
                PublicSupportController.class,
                QueuedCallsController.class,
                EventsExportSupportController.class,
                ReceiptSupportController.class,
                ZooTaskConfigController.class,
                ArchiveController.class,
                PaymentSupportController.class,
                TaskV2Controller.class,
                PushExtraSubsidyController.class,
                OrderMigrateController.class,
                StatusDocumentationController.class,
                LockMvpController.class
        ), "ru.yandex.market.checkout.checkouter.controllers");
    }
}
