import {makeCase, makeSuite} from 'ginny';

import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {CART_TITLE} from '@self/root/src/entities/checkout/cart/constants';

const KETTLE_START_COUNT = 3;

export default makeSuite('Недостаточное количества товара.', {
    environment: 'kadavr',
    id: 'bluemarket-2980',
    issue: 'BLUEMARKET-8172',
    story: {
        async beforeEach() {
            await prepareState.call(this);

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: this.params.region});
            return this.browser.yaScenario(this, waitForCartActualization);
        },

        [`Заголовок корзины содержит текст "${CART_TITLE}"`]: makeCase({
            test() {
                return this.cartHeader.getTitleText()
                    .should.eventually.to.be.include(
                        CART_TITLE,
                        `Заголовок корзины должен содержать текст "${CART_TITLE}"`
                    );
            },
        }),

        [`Надпись "Осталось ${KETTLE_START_COUNT - 1} штуки" отображается на оффере`]: makeCase({
            async test() {
                const offerNotification = `Осталось ${KETTLE_START_COUNT - 1} штуки`;

                await this.availableCartOfferAvailabilityInfo.isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Надпись должна отображаться'
                    );

                return this.availableCartOfferAvailabilityInfo.getStatusText()
                    .should.eventually.to.be.include(
                        offerNotification,
                        `Надпись должна содержать текст ${offerNotification}`
                    );
            },
        }),

        'Сумма и количество товаров в саммари отображается с учетом доступного количества': makeCase({
            async test() {
                const count = KETTLE_START_COUNT - 1;
                await this.orderTotal.getItemsCount()
                    .should.eventually.to.be.equal(
                        count,
                        `Количество товаров в саммари должно быть равно ${count}`
                    );

                const price = count * Number(kettle.offerMock.prices.value);
                return this.orderTotal.getItemsValue()
                    .should.eventually.to.be.equal(
                        price,
                        `Цена товаров равна ${price}`
                    );
            },
        }),
    },
});

async function prepareState() {
    const cart = buildCheckouterBucket({
        items: [{
            count: KETTLE_START_COUNT,
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
        }],
        region: this.params.region,
        itemsTotal: (KETTLE_START_COUNT - 1) * Number(kettle.offerMock.prices.value),
    });

    const testState = await this.browser.yaScenario(
        this,
        prepareMultiCartState,
        [cart]
    );

    await this.browser.setState(
        'Checkouter.collections.cartItem',
        {
            ...testState.checkouterState.cartItem,
            [kettle.skuMock.id]: {
                ...testState.checkouterState.cartItem[kettle.skuMock.id],
                count: KETTLE_START_COUNT - 1,
                changes: ['COUNT'],
            },
        }
    );
}

