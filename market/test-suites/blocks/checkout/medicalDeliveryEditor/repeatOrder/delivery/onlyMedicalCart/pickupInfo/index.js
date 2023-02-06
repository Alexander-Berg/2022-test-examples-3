import {
    makeSuite,
} from 'ginny';

import DeliveryOffersInfo
    from '@self/root/src/widgets/content/checkout/common/CheckoutMedicalCartDeliveryEditor/components/DeliveryOffersInfo/__pageObject';
import DeliveryOfferItem
    from '@self/root/src/widgets/content/checkout/common/CheckoutMedicalCartDeliveryEditor/components/DeliveryOfferItem/__pageObject';
import selectPickup from '../../../../selectPickup';

export default makeSuite('Самовывоз. Информация о пвз.', {
    id: 'marketfront-5842',
    issue: 'MARKETFRONT-91160',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryOffersInfo: () => this.createPageObject(DeliveryOffersInfo, {
                    parent: this.medicalCartDeliveryEditorCheckoutWizard,
                }),
                deliveryOfferItem: () => this.createPageObject(DeliveryOfferItem, {
                    parent: this.deliveryOffersInfo,
                }),
            });
        },
        'Выбрать любую точку самовывоза на карте': selectPickup({
            isCourierAvailable: true,
            isCheckPickupInfo: true,
        }),
    },
});
