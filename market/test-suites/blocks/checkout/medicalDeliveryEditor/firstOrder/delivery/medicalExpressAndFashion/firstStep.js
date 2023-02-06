import {makeSuite} from 'ginny';

import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import selectPickup from '../../../selectPickup';

export default makeSuite('Оформление первого заказа. Шаг 1.', {
    id: 'marketfront-5899',
    issue: 'MARKETFRONT-81900',
    feature: 'Оформление первого заказа. Шаг 1',
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
