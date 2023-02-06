import {makeSuite, makeCase} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {ORDER_STATUS} from '@self/root/src/entities/order';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import OutletLegalInfo from '@self/root/src/components/OutletLegalInfo/__pageObject';

import {outletMock, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';
import {ALCOHOL_ORGANISATION_LEGAL_INFO} from '@self/root/src/spec/hermione/configs/alco';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import formDataPost from '@self/root/src/spec/hermione/configs/checkout/formData/user-post-postpaid';

import OrderOutlet from '@self/root/src/components/OrderOutlet/__pageObject';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

const ORDER_ID = 11111;

const order = {
    orderId: ORDER_ID,
    items: [{
        skuId: skuMock.id,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: formDataPost.recipient,
    deliveryType: 'PICKUP',
    outletId: outletMock.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 100,
        dates: {
            fromDate: '10-10-2000',
            toDate: '15-10-2000',
            fromTime: '13:00',
            toTime: '19:00',
        },
    },
};

export default makeSuite('Алкоголь', {
    feature: 'Алкоголь',
    id: 'bluemarket-2715',
    environment: 'kadavr',
    issue: 'BLUEMARKET-6893',
    story: {
        async beforeEach() {
            this.setPageObjects({
                outletLegalInfo: () => this.createPageObject(OutletLegalInfo, {
                    parent: this.orderConfirmation,
                }),
                orderOutlet: () => this.createPageObject(OrderOutlet),
            });

            const state = mergeState([
                {
                    data: {
                        results: [
                            outletMock,
                        ],
                        search: {results: []},
                    },
                },
            ]);

            await this.browser.yaScenario(this, setReportState, {state});

            const result = await this.browser.yaScenario(this, prepareOrder, {
                region: this.params.region,
                orders: [order],
                paymentType: 'YANDEX',
                paymentMethod: 'CARD_ON_DELIVERY',
                status: ORDER_STATUS.DELIVERED,
            });

            const orderId = result.orders[0].id;
            return this.browser.yaOpenPage(
                PAGE_IDS_COMMON.ORDER,
                {orderId}
            );
        },
        'Юридическая информация': makeCase({
            async test() {
                await this.orderOutlet.clickOnOutletInfoMapLink();

                return this.expect(await this.outletLegalInfo.getLegalInfoText())
                    .to.be.equal(
                        // TODO: BLUEMARKET-7967 Перевести константу на кадавровую
                        ALCOHOL_ORGANISATION_LEGAL_INFO,
                        'Текст с юридической информацией должен быть правильным'
                    );
            },
        }),
    },
});
