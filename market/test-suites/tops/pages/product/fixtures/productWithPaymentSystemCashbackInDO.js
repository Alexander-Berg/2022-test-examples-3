import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {paymentSystemExtraCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
import {
    phoneProductRoute,
    productWithDefaultCashbackOffer,
    productWithDefaultPaymentSystemOffer,
    productWithDefaultPaymentSystemAndSimpleCashbackOffer,
} from '@self/platform/spec/hermione/fixtures/product';

export async function createAndSetPaymentSystemCashbackState({
    isPromoAvailable,
    isPromoProduct,
    hasCashback,
}) {
    let product = productWithDefaultCashbackOffer;
    const dataMixin = {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    };

    if (isPromoProduct && hasCashback) {
        product = productWithDefaultPaymentSystemAndSimpleCashbackOffer;
    } else if (isPromoProduct) {
        product = productWithDefaultPaymentSystemOffer;
    }

    await this.browser.setState('report', mergeState([
        product,
        dataMixin,
    ]));

    await this.browser.setState('Loyalty.collections.perks', isPromoAvailable ? [paymentSystemExtraCashbackPerk] : []);
    return this.browser.yaOpenPage('touch:product', phoneProductRoute);
}
