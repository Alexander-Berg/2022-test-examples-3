// eslint-disable-next-line max-len

import PaymentSystemPromoBannerPO from '@self/root/src/components/PaymentSystemPromoBanner/__pageObject';
import {mockLoadCards} from './mockFunctionality.js';

const widgetPath = '@self/root/src/widgets/content/checkout/common/PaymentSystemPromoBanner';

async function initContext(mandrelLayer) {
    await mandrelLayer.initContext();
}

export const checkPaymentSystemPromoBannerNotShown = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockFunction,
    cards
) => {
    await initContext(mandrelLayer);

    await mockFunction(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    await mockLoadCards(cards);

    const paymentSystemPromoBanner = container.querySelector(PaymentSystemPromoBannerPO.root);

    expect(paymentSystemPromoBanner).toBeNull();
};

export const checkPaymentSystemPromoBannerContent = async (
    jestLayer,
    apiaryLayer,
    mandrelLayer,
    mockFunction,
    cards,
    expectTitle,
    expectDescription
) => {
    await initContext(mandrelLayer);

    await mockFunction(jestLayer);

    const {container} = await apiaryLayer.mountWidget(widgetPath, {});

    await mockLoadCards(cards);

    const paymentSystemPromoBanner = container.querySelector(PaymentSystemPromoBannerPO.root);
    expect(paymentSystemPromoBanner).not.toBeNull();

    expect(container.querySelector(PaymentSystemPromoBannerPO.title).textContent).toEqual(expectTitle);
    expect(container.querySelector(PaymentSystemPromoBannerPO.description).textContent).toEqual(expectDescription);
};
