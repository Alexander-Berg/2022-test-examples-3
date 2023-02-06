import {makeSuite} from 'ginny';

import selectPickup from '../../../selectPickup';

export default makeSuite('Оформление повторного заказа. Шаг 1.', {
    feature: 'Оформление повторного заказа. Шаг 1',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.medicalCartDeliveryEditorCheckoutWizard.waitForVisible();
            await this.deliveryTypes.waitForVisible();
        },
        'Открыть страницу чекаута.': {
            'Флоу оформления заказа фармы курьером и самовывозом".': selectPickup({isCourierAvailable: true}),
        },
    },
});
