import {makeSuite} from 'ginny';

import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import selectPickup from '../../../selectPickup';

export default makeSuite('Оформление повторного заказа. Шаг 1.', {
    id: 'marketfront-5900',
    issue: 'MARKETFRONT-81908',
    feature: 'Оформление повторного заказа. Шаг 1',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.medicalCartDeliveryEditorCheckoutWizard,
                }),
            });

            await this.medicalCartDeliveryEditorCheckoutWizard.waitForVisible();
            await this.deliveryTypes.waitForVisible();
        },
        'Открыть страницу чекаута.': {
            'Флоу оформления заказа фармы курьером и самовывозом".': selectPickup({isCourierAvailable: true, isFillDelivery: true}),
        },
    },
});
