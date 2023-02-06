import {makeCase, makeSuite} from 'ginny';
import {isEqual} from 'ambar';
import formDataPost from '@self/root/src/spec/hermione/configs/checkout/formData/user-post-postpaid-rostov';
import formDataDelivery from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid-rostov';
import {skuMock as kettleSkuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {skuMock as televizorSkuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {rostovOutlet} from '@self/root/src/spec/hermione/kadavr-mock/report/multicheckout';

const FIRST_ORDER_ID = 11111;
const SECOND_ORDER_ID = 22222;

const ORDER_IDS = [FIRST_ORDER_ID, SECOND_ORDER_ID];

const firstOrder = {
    orderId: FIRST_ORDER_ID,
    items: [{
        skuId: kettleSkuMock.id,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: formDataPost.recipient,
    deliveryType: 'POST',
    outletId: rostovOutlet.id,
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

const secondOrder = {
    orderId: SECOND_ORDER_ID,
    items: [{
        skuId: televizorSkuMock.id,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: formDataDelivery.recipient,
    deliveryType: 'DELIVERY',
    address: formDataDelivery.address,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 200,
        dates: {
            fromDate: '11-11-2000',
            toDate: '11-11-2000',
            fromTime: '12:00',
            toTime: '22:00',
        },
    },
};
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Привязка мульти-заказа', {
    id: 'bluemarket-2816',
    issue: 'BLUEMARKET-7577',
    environment: 'kadavr',
    feature: 'Привязка заказа',
    params: {
        region: 'Регион',
        regionName: 'Имя региона',
    },
    defaultParams: {
        isAuth: false,
    },
    story: {
        async beforeEach() {
            await this.browser.yaScenario(this, 'thank.prepareThankPage', {
                orders: [firstOrder, secondOrder],
                region: region[this.params.regionName],
                paymentOptions: {
                    paymentType: 'PREPAID',
                    paymentMethod: 'YANDEX',
                    paymentStatus: 'HOLD',
                    status: 'PROCESSING',
                },
            });
        },

        'В url переданы id и bindKey заказов.': {
            'OrderIds переданы правильно': makeCase({
                async test() {
                    // получаем query запроса
                    const query = await this.browser.yaParseUrl().then(urlParsed => urlParsed.query);
                    const orderIds = query.orderId.map(orderId => +orderId);
                    await this.expect(isEqual(orderIds, ORDER_IDS))
                        .to.be.equal(true, 'ID заказов в url должны совпадать');
                },
            }),
            'bindKey присутвует в url': makeCase({
                async test() {
                    // получаем query запроса
                    const query = await this.browser.yaParseUrl().then(urlParsed => urlParsed.query);
                    // так как bindKey задаются рандомом мы не можем их сравнить, проверяем их наличие
                    await this.expect(query.bindKey.length)
                        .to.be.equal(ORDER_IDS.length, 'bindKey должны быть заданы!');
                    await this.browser.deleteCookie();
                },
            }),
        },
    },
});
