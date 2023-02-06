import SubscriptionAgreementPO from '@self/root/src/components/AgreementNote/components/SubscriptionAgreement/__pageObject';
import {mockCheckoutAgreementWithSubscription} from './mockFunctionality';
import {checkoutCartGroupId} from './mockData';

const widgetPath = '@self/root/src/widgets/content/checkout/common/CheckoutAgreementNote';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const hasPlusSubscriptionLegal = async (jestLayer, apiaryLayer, mandrelLayer) => {
    await initContext(mandrelLayer);
    await mockCheckoutAgreementWithSubscription(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {
        cartGroupId: checkoutCartGroupId,
    });

    const subscriptionAgreement = container.querySelector(SubscriptionAgreementPO.root);

    expect(subscriptionAgreement).toBeTruthy();
};
