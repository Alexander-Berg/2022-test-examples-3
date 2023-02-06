import {makeSuite, makeCase} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import OutletLegalInfo from '@self/root/src/components/OutletLegalInfo/__pageObject';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {skuMock, outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';
import formDataPost from '@self/root/src/spec/hermione/configs/checkout/formData/user-post-postpaid';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import {ALCOHOL_ORGANISATION_LEGAL_INFO} from '@self/root/src/spec/hermione/configs/alco';
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
    deliveryType: 'POST',
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
                orderConfirmation: () => this.createPageObject(OrderConfirmation),
                outletLegalInfo: () => this.createPageObject(OutletLegalInfo, {
                    parent: this.orderConfirmation,
                }),
            });

            const defaultState = mergeState([
                {
                    data: {
                        results: [
                            outletMock,
                        ],
                        search: {results: []},
                    },
                },
            ]);

            await this.browser.yaScenario(this, setReportState, {state: defaultState});

            await this.browser.yaPageReloadExtended();

            await this.browser.yaScenario(this, 'thank.prepareThankPage', {
                orders: [order],
                region: region[this.params.regionName],
                paymentOptions: {
                    paymentType: 'PREPAID',
                    paymentMethod: 'YANDEX',
                    paymentStatus: 'HOLD',
                    status: 'PROCESSING',
                },
            });
        },
        'Юридическая информация': makeCase({
            async test() {
                await this.orderConfirmation.detailsClick();

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
