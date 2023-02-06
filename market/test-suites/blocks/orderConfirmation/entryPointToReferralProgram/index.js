import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {pathOrUnsafe} from 'ambar';

import entryPointToReferralProgram from '@self/root/src/spec/hermione/test-suites/blocks/entryPointToReferralProgram';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {PAYMENT_TYPE, PAYMENT_METHOD} from '@self/root/src/entities/payment';

async function prepareStateForCheckEntryPointToReferralProgram() {
    await this.browser.yaScenario(this, 'checkoutResource.prepareOrder', {
        region: region['Москва'],
        orders: [{
            items: [{
                skuId: checkoutItemIds.asus.skuId,
                offerId: checkoutItemIds.asus.offerId,
            }],
            deliveryType: DELIVERY_TYPES.DELIVERY,
        }],
        status: DELIVERY_TYPES.DELIVERY,
        paymentType: PAYMENT_TYPE.POSTPAID,
        paymentMethod: PAYMENT_METHOD.CASH_ON_DELIVERY,
    })
        .then(result => this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS_CONFIRMATION, {
            orderId: pathOrUnsafe('', ['orders', '0', 'id'], result),
        }));

    await this.referralLandingLink.scrollToLink();
}

export default makeSuite('Точка входа в реферальную программу.', {
    story: mergeSuites(
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Пользователь не достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4814',
            },
            params: {
                linkLabel: 'Получить 300 баллов за друга',
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgram,
            },
        }),
        prepareSuite(entryPointToReferralProgram(), {
            suiteName:
                'Пользователь достиг максимального количества баллов.',
            meta: {
                id: 'marketfront-4815',
            },
            params: {
                linkLabel: 'Рекомендовать Маркет друзьям',
                isGotFullReward: true,
                specialPrepareState: prepareStateForCheckEntryPointToReferralProgram,
            },
        })
    ),
});
