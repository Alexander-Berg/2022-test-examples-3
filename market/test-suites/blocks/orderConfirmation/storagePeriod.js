import {makeCase, makeSuite} from 'ginny';

import {formatDate} from '@self/root/src/spec/utils/formatDate';
import {formatDaysInterval} from '@self/root/src/utils/datetime';
import {MSEC_IN_DAY} from '@self/root/src/constants/ttl';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import {skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';
import {alcohol as alcoOutlet} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {recipient} from '@self/root/src/spec/hermione/configs/checkout/formData/office-address-and-recipient';

import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import StoragePeriod from '@self/root/src/components/StoragePeriod/__pageObject';

import {checkStoragePeriod} from '@self/root/src/spec/hermione/scenarios/storagePeriod';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';

import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
const storagePeriod = 3;

const order = {
    orderId: 123456,
    items: [{
        skuId: skuMock.id,
        count: 1,
        buyerPrice: 500,
    }],
    recipient,
    deliveryType: 'PICKUP',
    outletId: alcoOutlet.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 0,
        dates: {
            fromDate: formatDate(),
            // Сегодня + 2 дня
            toDate: formatDate(new Date(Date.now() + (MSEC_IN_DAY * 2))),
        },
        outletStoragePeriod: storagePeriod,
        outlet: alcoOutlet,
    },
};
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Сроки хранения.', {
    id: 'bluemarket-3365',
    issue: 'BLUEMARKET-10247',
    environment: 'kadavr',
    feature: 'C&C',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderConfirmation: () => this.createPageObject(OrderConfirmation),
                storagePeriod: () => this.createPageObject(StoragePeriod, {
                    parent: this.orderConfirmation,
                }),
            });

            const defaultState = mergeState([
                {
                    data: {
                        results: [
                            {...alcoOutlet, storagePeriod},
                        ],
                        search: {results: []},
                    },
                },
            ]);

            await this.browser.yaScenario(this, setReportState, {state: defaultState});

            await this.browser.yaScenario(this,
                prepareThankPage,
                {
                    orders: [order],
                    region: region[this.params.regionName],
                    paymentOptions: {
                        paymentType: 'POSTPAID',
                        paymentMethod: 'CASH_ON_DELIVERY',
                        paymentStatus: 'HOLD',
                        status: 'PROCESSING',
                    },
                });
        },

        'Блок "Состав заказа"': {
            'содержит информацию о сроках хранения заказа в точке поставщика': makeCase({
                async test() {
                    const expectedText = `Срок хранения ${formatDaysInterval(storagePeriod, storagePeriod)}`;

                    await this.orderConfirmation.isVisible()
                        .should.eventually.be.equal(true, 'Блок с подтверждением заказа должен отображаться');

                    return this.browser.yaScenario(
                        this,
                        checkStoragePeriod,
                        {
                            storagePeriodPO: this.storagePeriod,
                            expectedText,
                        }
                    );
                },
            }),
        },
    },
});
