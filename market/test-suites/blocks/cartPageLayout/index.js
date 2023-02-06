import {prepareEmptyCartPage} from '@self/root/src/spec/hermione/scenarios/cart';

const {
    makeCase,
    mergeSuites,
    makeSuite,
// eslint-disable-next-line import/no-commonjs
} = require('ginny');

const authorizedUserText = 'А чтобы их найти, загляните в каталог\nили в раздел со скидками';

/**
 * Тесты на блок n-cart-list.
 * @param {PageObject.CartList} cartList
 */
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Структура страницы.', {
    environment: 'kadavr',
    feature: 'Список офферов',
    story: mergeSuites(
        makeSuite('Авторизованный пользователь', {
            defaultParams: {
                isAuthWithPlugin: true,
            },
            story: {
                'Пустая корзина.': {
                    beforeEach() {
                        return this.browser.yaScenario(
                            this,
                            prepareEmptyCartPage,
                            {
                                region: this.params.region,
                            }
                        );
                    },

                    'Заголовок': {
                        'по умолчанию': {
                            'должен быть отображён': makeCase({
                                id: 'bluemarket-2239',
                                test() {
                                    return testHeaderVisibility(
                                        this.cartEmpty,
                                        'Сложите в корзину нужные товары'
                                    );
                                },
                            }),
                        },
                    },

                    'Тело страницы': {
                        'по умолчанию': {
                            [`содержит текст "${authorizedUserText}"`]: makeCase({
                                id: 'bluemarket-2239',
                                test() {
                                    return testBodyText(
                                        this.cartEmpty,
                                        authorizedUserText
                                    );
                                },
                            }),
                        },
                    },
                },
            },
        }),

        makeSuite('Неавторизованный пользователь', {
            story: {
                'Пустая корзина.': {
                    beforeEach() {
                        return this.browser.yaScenario(
                            this,
                            prepareEmptyCartPage,
                            {
                                region: this.params.region,
                            }
                        );
                    },

                    'Заголовок': {
                        'по умолчанию': {
                            'должен быть отображён': makeCase({
                                id: 'bluemarket-1499',
                                test() {
                                    return testHeaderVisibility(
                                        this.cartEmpty,
                                        'Войдите в аккаунт'
                                    );
                                },
                            }),
                        },
                    },

                    'Тело страницы': {
                        'по умолчанию': {
                            'содержит текст "И если вы уже добавляли товары в корзину — они появятся здесь.\nА новые не потеряются"': makeCase({
                                id: 'bluemarket-1499',
                                test() {
                                    return testBodyText(
                                        this.cartEmpty,
                                        'И если вы уже добавляли товары в корзину — они появятся здесь.\nА новые не потеряются'
                                    );
                                },
                            }),
                        },
                    },
                },
            },
        })
    ),
});

function testHeaderVisibility(cartPageLayout, title) {
    return cartPageLayout.title
        .isVisible()
        .should.eventually.to.be.equal(true, 'Заголовок должен быть отображён')
        .then(() => cartPageLayout.getTitleText())
        .should.eventually.to.be
        .equal(
            title, `Заголовок должен быть "${title}"`
        );
}

function testBodyText(cartPageLayout, text) {
    return cartPageLayout.body
        .isVisible()
        .should.eventually.to.be.equal(true, 'Тело страницы должно быть отображено')
        .then(() => cartPageLayout.getBodyText())
        .should.eventually.to.be.include(text, `Тело страницы должно содержать текст "${text}"`);
}
