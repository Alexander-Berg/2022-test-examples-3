import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';
import COOKIE from '@self/root/src/constants/cookie';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import CourierTracking from '@self/root/src/widgets/parts/CourierTracking/__pageObject';
import OrderCancellationDialog from '@self/root/src/widgets/parts/CourierTracking/components/OrderCancellationDialog/__pageObject';
import OrderRescheduleDialog from '@self/root/src/widgets/parts/CourierTracking/components/OrderRescheduleDialog/__pageObject';
import BottomDrawerWebview from '@self/root/src/components/BottomDrawerWebview/__pageObject';
import DeliveryStatusViewStub from '@self/root/src/widgets/parts/CourierTracking/components/DeliveryStatusViewStub/__pageObject';
import DeliveryInProgressView from '@self/root/src/widgets/parts/CourierTracking/components/DeliveryInProgressView/__pageObject';
import CourierTrackingMap from '@self/root/src/widgets/parts/CourierTracking/components/Map/__pageObject';
import DeliveryInfo from '@self/root/src/widgets/parts/CourierTracking/components/DeliveryInfo/__pageObject';
import OrderCardList from '@self/root/src/widgets/parts/CourierTracking/components/OrderCardList/__pageObject';
import OrderCancellationModal
    from '@self/root/src/widgets/parts/CourierTracking/components/View/OrderCancellationModal/__pageObject';
import OrderRescheduleModal from '@self/root/src/widgets/parts/CourierTracking/components/View/OrderRescheduleModal/__pageObject';
import RescheduleConfirmationDrawer from '@self/root/src/widgets/parts/CourierTracking/components/RescheduleConfirmationDrawer/__pageObject';
import CourierTrackingPageDeliveryViewStubSuite
    from '@self/platform/spec/hermione/test-suites/blocks/CourierTracking/DeliveryViewStub';
import CourierTrackingPageDeliveryInProgressSuite
    from '@self/platform/spec/hermione/test-suites/blocks/CourierTracking/DeliveryInProgress';
import CourierTrackingPageRescheduleDeliverySuite
    from '@self/platform/spec/hermione/test-suites/blocks/CourierTracking/RescheduleDelivery';
import CourierTrackingPageCancelDeliverySuite
    from '@self/platform/spec/hermione/test-suites/blocks/CourierTracking/CancelDelivery';
import CourierTrackingPageConfirmDeliveryRescheduleSuite
    from '@self/platform/spec/hermione/test-suites/blocks/CourierTracking/ConfirmDeliveryReschedule';

import {
    IN_PREPARATION as DeliveryInPreparationConfig,
    RESCHEDULED as DeliveryRescheduledConfig,
    NOT_DELIVERED as DeliveryFailedConfig,
    DELIVERED as DeliverySuccessfulConfig,
    IN_PROGRESS as DeliveryInProgressConfig,
    CONFIRM_RESCHEDULED as ConfirmRescheduleConfig,
} from '@self/platform/spec/hermione/configs/courierTracking';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Где мой курьер?"', {
    environment: 'kadavr',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    courierTrackingPage: () => this.createPageObject(CourierTracking),
                    deliveryStatusViewStub: () => this.createPageObject(
                        DeliveryStatusViewStub,
                        {parent: this.courierTrackingPage}
                    ),
                });

                await this.browser.windowHandleSize({
                    width: 375,
                    height: 700,
                });

                // Убираем модалку про бесконтактную доставку
                await this.browser.yaSetCookie({
                    name: COOKIE.TPL_CONTACTLESS_DELIVERY_NOTIFIED,
                    value: '1',
                    path: '/',
                });
            },
        },
        {
            'Со статусом доставки': {
                'IN_PREPARATION.':
                    prepareSuite(CourierTrackingPageDeliveryViewStubSuite, {
                        params: DeliveryInPreparationConfig,
                        meta: {id: 'bluemarket-3172'},
                    }),
                'RESCHEDULED.': mergeSuites(
                    {
                        async beforeEach() {
                            this.setPageObjects({
                                rescheduleConfirmationDrawer: () => this.createPageObject(RescheduleConfirmationDrawer),
                            });
                        },
                    },
                    prepareSuite(CourierTrackingPageDeliveryViewStubSuite, {
                        params: DeliveryRescheduledConfig,
                        meta: {id: 'bluemarket-3208'},
                    }),
                    prepareSuite(CourierTrackingPageConfirmDeliveryRescheduleSuite, {
                        params: ConfirmRescheduleConfig,
                    })
                ),

                'NOT_DELIVERED.':
                    prepareSuite(CourierTrackingPageDeliveryViewStubSuite, {
                        params: DeliveryFailedConfig,
                        meta: {id: 'bluemarket-3204'},
                    }),
                'DELIVERED.':
                    prepareSuite(CourierTrackingPageDeliveryViewStubSuite, {
                        params: DeliverySuccessfulConfig,
                        meta: {id: 'bluemarket-3168'},
                    }),
                'IN_PROGRESS.': mergeSuites(
                    {
                        async beforeEach() {
                            this.setPageObjects({
                                deliveryInProgressView: () => this.createPageObject(
                                    DeliveryInProgressView,
                                    {parent: this.courierTrackingPage}
                                ),
                                orderRescheduleModal: () => this.createPageObject(
                                    OrderRescheduleModal,
                                    {parent: this.courierTrackingPage}
                                ),
                                orderCancellationModal: () => this.createPageObject(
                                    OrderCancellationModal,
                                    {parent: this.courierTrackingPage}
                                ),
                                orderRescheduleDialog: () => this.createPageObject(
                                    OrderRescheduleDialog,
                                    {parent: this.orderRescheduleModal}
                                ),
                                orderCancellationDialog: () => this.createPageObject(
                                    OrderCancellationDialog,
                                    {parent: this.orderCancellationModal}
                                ),
                                courierTrackingMap: () => this.createPageObject(
                                    CourierTrackingMap,
                                    {parent: this.deliveryInProgressView}
                                ),
                                bottomDrawer: () => this.createPageObject(
                                    BottomDrawerWebview,
                                    {parent: this.deliveryInProgressView}
                                ),
                                deliveryInfo: () => this.createPageObject(DeliveryInfo, {parent: this.bottomDrawer}),
                                orderCardList: () => this.createPageObject(OrderCardList, {parent: this.bottomDrawer}),
                            });

                            const {courierTracking} = DeliveryInProgressConfig;
                            const {id: trackingId} = courierTracking;
                            await this.browser.setState('marketLogistics.collections.courierTracking', {
                                [trackingId]: courierTracking,
                            });

                            await this.browser.yaOpenPage(PAGE_IDS_TOUCH.COURIER_TRACKING, {trackingId});
                        },
                    },
                    prepareSuite(CourierTrackingPageDeliveryInProgressSuite, {}),
                    prepareSuite(CourierTrackingPageRescheduleDeliverySuite, {}),
                    prepareSuite(CourierTrackingPageCancelDeliverySuite, {})
                ),
            },
        }
    ),
});
