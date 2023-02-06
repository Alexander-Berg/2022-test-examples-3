import {makeSuite, makeCase} from '@yandex-market/ginny';

import {
    waitForLoad,
    waitForMessage,
    checkMessageCount,
    checkThatMessageWasDispatched,
    postMessage,
} from './utils';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Встраиваемое окно корзины.', {
    environment: 'kadavr',
    params: {
        offerId: 'ИД оффера, который добавляем в корзину',
    },
    story: {
        'Изменение тайтла': {
            'при успешном добавлении в корзину': makeCase({
                id: 'MARKETFRONT-32345',
                issue: 'MARKETFRONT-32345',

                async test() {
                    await waitForLoad.call(this);
                    const loaderTitleText = await this.view.getTitleText();
                    await this.browser.expect(loaderTitleText).to.be.equal('Добавляем в корзину', 'Отображение процесса загрузки при добавлении');

                    await postMessage.call(this, {
                        event: 'addToCart',
                    });

                    await this.preloader.waitForHidden(5000);
                    const afterLoadTitleText = await this.view.getTitleText();
                    await this.browser.expect(afterLoadTitleText).to.be.equal('Добавлен в корзину', 'Отображается сообщение об успешном добавлении');
                },
            }),
        },
        'Проверка отправки сообщений': {
            'при успешном добавлении в корзину': makeCase({
                id: 'MARKETFRONT-32345',
                issue: 'MARKETFRONT-32345',

                async test() {
                    await waitForLoad.call(this);
                    await postMessage.call(this, {
                        event: 'addToCart',
                    });

                    await waitForMessage.call(this, 'changeCount');
                    await checkThatMessageWasDispatched.call(this, {
                        event: 'changeCount',
                        params: {
                            diffValue: 1,
                            actualCount: 1,
                            offerId: this.params.offerId,
                        },
                    }, 'Событие changeCount должно отправиться со всеми обязательными параметрами');

                    await checkMessageCount.call(this, 'ready', 1, 'Событие ready должно отправиться только один раз');
                },
            }),

            'при изменение кол-ва товаров': makeCase({
                id: 'MARKETFRONT-32345',
                issue: 'MARKETFRONT-32345',

                async test() {
                    await waitForLoad.call(this);
                    await postMessage.call(this, {
                        event: 'addToCart',
                    });

                    await this.counterButton.clickPlus();

                    await waitForMessage.call(this, 'changeCount');
                    await checkMessageCount.call(this, 'changeCount', 2, 'Событие changeCount должно отправиться дважды');
                    await checkThatMessageWasDispatched.call(this, {
                        event: 'changeCount',
                        params: {
                            diffValue: 1,
                            actualCount: 2,
                            offerId: this.params.offerId,
                        },
                    }, 'Событие changeCount должно содержать информацию об увеличении кол-ва товаров');

                    await this.counterButton.clickMinus();

                    await waitForMessage.call(this, 'changeCount');
                    await checkMessageCount.call(this, 'changeCount', 3, 'Событие changeCount должно отправиться трижды');
                    await checkThatMessageWasDispatched.call(this, {
                        event: 'changeCount',
                        params: {
                            diffValue: -1,
                            actualCount: 1,
                            offerId: this.params.offerId,
                        },
                    }, 'Событие changeCount должно содержать информацию об уменьшении кол-ва товаров');

                    await checkMessageCount.call(this, 'ready', 1, 'Событие ready должно отправиться только один раз');
                },
            }),

            'при удалении товара из корзины': makeCase({
                id: 'MARKETFRONT-32345',
                issue: 'MARKETFRONT-32345',

                async test() {
                    await waitForLoad.call(this);
                    await postMessage.call(this, {
                        event: 'addToCart',
                    });

                    await this.counterButton.clickMinus();

                    await waitForMessage.call(this, 'changeCount');
                    await checkMessageCount.call(this, 'changeCount', 2, 'Событие changeCount должно отправиться дважды');
                    await checkThatMessageWasDispatched.call(this, {
                        event: 'changeCount',
                        params: {
                            diffValue: -1,
                            actualCount: 0,
                            offerId: this.params.offerId,
                        },
                    }, 'Событие changeCount должно содержать информацию об удалении товара');

                    await this.view.putBack();

                    await waitForMessage.call(this, 'changeCount');
                    await checkMessageCount.call(this, 'changeCount', 3, 'Событие changeCount должно отправиться трижды');
                    await checkThatMessageWasDispatched.call(this, {
                        event: 'changeCount',
                        params: {
                            diffValue: 1,
                            actualCount: 1,
                            offerId: this.params.offerId,
                        },
                    }, 'Событие changeCount должно содержать информацию об увеличении кол-ва товара');

                    await checkMessageCount.call(this, 'ready', 1, 'Событие ready должно отправиться только один раз');
                },
            }),
        },
    },
});
