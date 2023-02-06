import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// pageObjects
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject';
import LiftingToFloor from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/LiftingToFloor/__pageObject';
import LiftingToFloorPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/LiftingToFloorPopup/__pageObject';


// suites
import checkboxStateSuite from './checkboxStateSuite';
import PopupSuite from './popupSuite';

import {getLiftingToFloorCarts} from './helpers';

/**
 * Тесты подъема на этаж.
 * Ожидается что мы находимся на странице confirmationPage и вызвано создание мультикорзины
 * с параметром this.params.carts. Должен быть задан PageObject this.confirmationPage
 */
export default makeSuite('Подъем на этаж', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.confirmationPage,
                }),
                liftingToFloor: () => this.createPageObject(LiftingToFloor, {
                    parent: this.deliveryInfo,
                }),
                liftingToFloorPopup: () => this.createPageObject(LiftingToFloorPopup, {
                    parent: this.confirmationPage,
                }),
            });
        },
        'Если не передана доступность подъема.': prepareSuite(checkboxStateSuite, {
            params: {
                carts: getLiftingToFloorCarts(),
                isExist: false,
            },
        }),
        'Если пришел бесплатный подъем.': prepareSuite(checkboxStateSuite, {
            params: {
                carts: getLiftingToFloorCarts({
                    type: 'INCLUDED',
                }),
                liftingAvailability: 'INCLUDED',
                isExist: false,
            },
        }),
        'Если подъем не доступен.': prepareSuite(checkboxStateSuite, {
            params: {
                carts: getLiftingToFloorCarts({
                    type: 'NOT_AVAILABLE',
                }),
                liftingAvailability: 'NOT_AVAILABLE',
                isExist: false,
            },
        }),
        'Если подъем доступен.': mergeSuites(
            prepareSuite(checkboxStateSuite, {
                params: {
                    carts: getLiftingToFloorCarts({
                        manualLiftPerFloorCost: 50,
                        elevatorLiftCost: 100,
                        type: 'AVAILABLE',
                    }),
                    liftingAvailability: 'AVAILABLE',
                },
            }),
            prepareSuite(PopupSuite, {
                params: {
                    carts: getLiftingToFloorCarts({
                        manualLiftPerFloorCost: 50,
                        elevatorLiftCost: 100,
                        type: 'AVAILABLE',
                    }),
                    manualLiftPerFloorCost: 50,
                    elevatorLiftCost: 100,
                },
            })
        ),
    },
});
