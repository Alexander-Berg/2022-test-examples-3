import {makeSuite, makeCase} from '@yandex-market/ginny';

//* @param {PageObject.YandexGoCatalog} yandexGoCatalog
//* @param {PageObject.Snippet} snippet
//* @param {PageObject.YandexGoCartButtonPopup} yandexGoCartButtonPopup

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Многостраничный тест главной Yandex Go и выдачи', {
    id: 'm-touch-3877',
    tags: ['Контур#Интеграции'],
    story: {
        'По-умолчанию': {
            'проходит успешно': makeCase({
                async test() {
                    await this.yandexGoCatalog.waitForVisible();

                    await this.browser.allure.runStep(
                        'Клик по категории уводит пользователя на поисковую выдачу',
                        () => this.browser.yaWaitForPageReloadedExtended(
                            () => this.yandexGoCatalog.clickNthCategory(1)
                        )
                    );

                    // без этого мы будем не попадать в кнопку перехода в корзину на попапе
                    await this.browser.yaDisableCSSAnimation();

                    await this.browser.allure.runStep(
                        'Добавляем товар в корзину',
                        () => this.browser.yaWaitForChangeValue({
                            action: () => this.snippet.clickOnCartButton(),
                            valueGetter: async () => this.browser.isVisibleWithinViewport(
                                await this.yandexGoCartButtonPopup.getSelector()
                            ),
                        })
                    );

                    // Даже с отключенной анимацией, и с isVisibleWithinViewport
                    // попап иногда не успевает выехать
                    await this.browser.allure.runStep(
                        'Даем возможность попапу появиться',
                        // eslint-disable-next-line market/ginny/no-pause
                        () => this.browser.pause(1000)
                    );

                    await this.browser.allure.runStep(
                        'Ожидаем появления попапа корзины',
                        () => this.yandexGoCartButtonPopup.waitForVisible()
                    );

                    await this.browser.allure.runStep(
                        'Клик по кнопке "Корзина" на попапе уводит пользователя в корзину',
                        () => this.browser.yaWaitForPageReloadedExtended(
                            () => this.yandexGoCartButtonPopup.clickOnCartButton()
                        )
                    );

                    await this.browser.getUrl()
                        .should.eventually.to.be.link({
                            pathname: '/yandex-go/cart',
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
