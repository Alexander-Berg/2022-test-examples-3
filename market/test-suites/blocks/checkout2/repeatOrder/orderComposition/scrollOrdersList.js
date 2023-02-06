import {
    makeSuite,
    makeCase,
} from 'ginny';

// scenarios
import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {bigCarts} from './helpers';
import {ADDRESSES, CONTACTS} from '../../constants';

export default makeSuite('Список товаров на главном экране.', {
    feature: 'Список товаров на главном экране.',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
        region: 'Регион',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: ADDRESSES.MOSCOW_ADDRESS,
                    contact: CONTACTS.DEFAULT_CONTACT,
                }
            );

            await this.browser.yaScenario(
                this,
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts: bigCarts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Свайп списка товаров.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Свайпнуть список товаров влево.',
                    async () => {
                        await this.parcelView.scrollItemsLeft();

                        await this.browser.allure.runStep(
                            'Список проскролен в конец вправо.',
                            async () => {
                                await this.parcelView.isOrdersListScrolledRight()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Список должен быть проскролен в конец.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Свайпнуть список товаров вправо.',
                    async () => {
                        await this.parcelView.scrollItemsRight();

                        await this.browser.allure.runStep(
                            'Список проскролен в конец влево.',
                            async () => {
                                await this.parcelView.isOrdersListScrolledLeft()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Список должен быть проскролен в конец.'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
