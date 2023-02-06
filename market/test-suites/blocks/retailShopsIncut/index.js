import {makeCase, makeSuite} from 'ginny';

import {createState, ROUTE_PARAMS} from './fixtures';

export default makeSuite('Врезка магазинов Ритейла.', {
    environment: 'kadavr',
    params: {
        isTouch: 'Тестируем тач чи нет',
    },
    defaultParams: {
        isTouch: false,
    },
    story: {
        beforeEach() {
            return prepareState.call(this);
        },

        'По дефолту': {
            'отображается и содержит правильный контент': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Проверяем видимость врезки',
                        () => this.retailShopsIncut.isVisible()
                            .should.eventually
                            .to.be.equal(true)
                    );

                    await this.browser.allure.runStep(
                        'Проверяем сниппет с 51 товаром и временем доставки',
                        async () => {
                            await this.shopSnippet1.getFoundOffersText()
                                .should.eventually
                                .to.be.contain(`${this.params.isTouch ? '' : 'Найден '}51 товар`);
                            await this.shopSnippet1.getDeliveryMessageText()
                                .should.eventually
                                .to.be.contain('от 25 минут');
                        }
                    );

                    await this.browser.allure.runStep(
                        'Проверяем сниппет с 52 товарами и временем работы',
                        async () => {
                            await this.shopSnippet2.getFoundOffersText()
                                .should.eventually
                                .to.be.contain(`${this.params.isTouch ? '' : 'Найдено '}52 товара`);
                            await this.shopSnippet2.getDeliveryMessageText()
                                .should.eventually
                                .to.be.contain('с 8:00');
                        }
                    );

                    await this.browser.allure.runStep(
                        'Проверяем сниппет с 55 товарами и без инфы о доставке',
                        async () => {
                            await this.shopSnippet3.getFoundOffersText()
                                .should.eventually
                                .to.be.contain(`${this.params.isTouch ? '' : 'Найдено '}55 товаров`);
                            await this.shopSnippet3.isDeliveryElementExisting()
                                .should.eventually.equal(false);
                        }
                    );
                },
            }),
        },
    },
});

async function prepareState() {
    await this.browser.setState('report', createState());

    const pageId = this.params.isTouch ? 'touch:list' : 'market:catalog';

    return this.browser.yaOpenPage(pageId, ROUTE_PARAMS);
}
