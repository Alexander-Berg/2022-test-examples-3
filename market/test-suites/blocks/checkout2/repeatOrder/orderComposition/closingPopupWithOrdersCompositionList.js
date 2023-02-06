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

import {simpleCarts} from './helpers';
import {ADDRESSES, CONTACTS} from '../../constants';

export default makeSuite('Список товаров в попапе "Состав заказа".', {
    feature: 'Список товаров в попапе "Состав заказа".',
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
                    carts: simpleCarts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Отображение списка с последующим закрытием попапа.': {
            async beforeEach() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Кликнуть на изображение товара.',
                    async () => {
                        await this.parcelView.clickOrderItemClickable();
                    }
                );
            },
            'Открывается попап "Состав заказа".': makeCase({
                async test() {
                    const title = 'Состав заказа';

                    await this.cartItemsDetails.waitForTitleVisible();
                    await this.cartItemsDetails.getTitleText()
                        .should.eventually.to.be.equal(
                            title,
                            `Заголовок должен быть ${title}.`
                        );

                    await this.allure.runStep(
                        'Присутствует список товаров.', async () =>
                            this.orderItemsList.isOrdersListVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Список товаров должен быть виден.'
                                )
                    );

                    await this.allure.runStep(
                        'Есть изображения товара.', async () =>
                            this.orderItemsList.isEveryImageExisting()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Картинка товара должена быть видна для каждой позиции.'
                                )
                    );

                    await this.allure.runStep(
                        'Есть названия товаров.', async () =>
                            this.orderItemsList.isEveryTitlesExisting()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Название товара должено быть видно для каждой позиции.'
                                )
                    );

                    await this.allure.runStep(
                        'Есть количество штук каждой позиции.', async () =>
                            this.orderItemsList.isEveryCountExisting()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Количество товара должено быть видно для каждой позиции.'
                                )
                    );
                },
            }),
            'Возврат к главному экрану чекаута по нажатию на кнопку "Продолжить".': makeCase({
                async test() {
                    await this.cartItemsDetails.waitForContinueButtonVisible();
                    await this.cartItemsDetails.clickOnContinueButton();

                    await this.cartItemsDetails.waitForInvisible();
                },
            }),
            'Возврат к главному экрану чекаута по нажатию на стрелочку.': makeCase({
                async test() {
                    await this.cartItemsDetails.waitForBackArrowButtonVisible();
                    await this.cartItemsDetails.clickOnBackArrowButton();

                    await this.cartItemsDetails.waitForInvisible();
                },
            }),
            'Возврат к главному экрану чекаута по нажатию на "X".': makeCase({
                async test() {
                    await this.popupBase.waitForVisible();
                    await this.popupBase.clickOnCrossButton();

                    await this.cartItemsDetails.waitForInvisible();
                },
            }),
            'Возврат к главному экрану чекаута по нажатию на "ESC".': makeCase({
                async test() {
                    await this.popupBase.waitForVisible();
                    await this.popupBase.closeOnEscape();

                    await this.cartItemsDetails.waitForInvisible();
                },
            }),
        },
    },
});
