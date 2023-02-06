import {makeSuite, mergeSuites, makeCase} from 'ginny';

import YandexHelpInfo from '@self/root/src/widgets/parts/OrderConfirmation/components/YandexHelpInfo/__pageObject';
import {outlet1 as outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {formatDate} from '@self/root/src/spec/utils/formatDate';
import {MSEC_IN_DAY} from '@self/root/src/constants/ttl';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

const TITLE_TEXT = 'Спасибо за помощь';
const MAIN_TEXT = '₽ перечислены в «Помощь рядом»';
const ORDER_ID = 11111;
const DEFAULT_ORDER = {
    orderId: ORDER_ID,
    items: [{
        skuId: 11,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: 111,
    deliveryType: 'PICKUP',
    outletId: outletMock.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        outletId: outletMock.id,
        outletStoragePeriod: 3,
        outlet: outletMock,
        buyerPrice: 100,
        dates: {
            fromDate: formatDate(),
            // Сегодня + 2 дня
            toDate: formatDate(new Date(Date.now() + (MSEC_IN_DAY * 2))),
        },
    },
};

module.exports = makeSuite('Помощь рядом. Плашка на Спасибо.', {
    environment: 'kadavr',
    feature: 'Помощь рядом',
    story: mergeSuites(
        {
            async beforeEach() {
                const defaultState = mergeState([
                    {
                        data: {
                            results: [outletMock],
                            search: {
                                results: [],
                            },
                        },
                    },
                ]);
                await this.browser.yaScenario(this, setReportState, {state: defaultState});
                await this.browser.yaScenario(this, 'thank.prepareThankPage', {
                    region: 213,
                    orders: [{
                        ...DEFAULT_ORDER,
                    }],
                    pageParams: {
                        withYandexHelp: true,
                        yandexHelpDonationAmount: '42',
                    },
                });
                this.setPageObjects({
                    yandexHelpInfo: () => this.createPageObject(YandexHelpInfo),
                });
            },
        },
        {
            'Отображение плашки на Спасибо': makeCase({
                id: 'marketfront-4592',
                issue: 'MARKETFRONT-36815',
                async test() {
                    await this.yandexHelpInfo.isVisible()
                        .should.eventually.to.be.equal(true, 'Отображается плашка Помощи');

                    await this.yandexHelpInfo.isIconVisible()
                        .should.eventually.to.be.equal(true, 'Иконка должна отображаться');

                    await this.yandexHelpInfo.getTitle()
                        .should.eventually.to.be.equal(TITLE_TEXT, `Заголовок блока "${TITLE_TEXT}"`);

                    await this.yandexHelpInfo.getMainText()
                        .should.eventually.include(MAIN_TEXT, `Основной текст блока содержит "${MAIN_TEXT}"`);
                },
            }),
        }
    ),
});
