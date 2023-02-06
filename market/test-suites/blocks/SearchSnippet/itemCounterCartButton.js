import {makeSuite, makeCase, mergeSuites} from 'ginny';

// page-objects
import SearchSnippetCartButton from '@self/platform/spec/page-objects/containers/SearchSnippet/CartButton';

/**
 * Тест на работу кнопки "В корзину" и кнопки удаления/добавления (каунтера)
 */
export default makeSuite('Кнопки удаления/добавления товара в корзину (каунтер товара)', {
    environment: 'kadavr',
    params: {
        counterStep: 'number - минимальный шаг изменения товара в корзине',
        offerId: 'string - id оффера, дообавляемого в корзину',
    },
    story: mergeSuites({
        'Если товар не добавлен в Корзину, то': {
            'по клику на кнопку "В Корзину",': {
                'в Корзину добавляется единица товара и кнопка "В Корзину" заменяется на кнопку с каунтером': makeCase({
                    issue: 'MARKETFRONT-21869',
                    async test() {
                        const {counterStep} = this.params;

                        await this.browser.yaWaitForPageReady();
                        await this.browser.setState('Carter.items', []);
                        await this.browser.yaReactPageReload();
                        await this.snippetCartButton.click();

                        if (this.cartPopup) {
                            await this.cartPopup.waitForAppearance();
                            await this.cartPopup.close();
                        }

                        await this.browser.allure.runStep(
                            'Дожидаемся появления кнопки с каунтерами на месте кнопки "В Корзину"',
                            () => this.browser.waitForVisible(SearchSnippetCartButton.counter, 5000)
                        );

                        const sentToCarterCounterValue = await waitForSentToCarterCounterValue(this, 'addItem');
                        const expectedCounterValue = counterStep;

                        return this.browser.allure.runStep(
                            'Проверяем значение каунтера в запросе в Carter',
                            () => this.expect(sentToCarterCounterValue)
                                .to.be.equal(expectedCounterValue, `В запросе в Картер количество товара равно ${sentToCarterCounterValue}`)
                        );
                    },
                }),
            },
        },

        'Если в Корзину добавлена одна единица товара, то': {
            'по клику на кнопку увеличения количества товара,': {
                'значение каунтера увеличивается на единицу товара': makeCase({
                    issue: 'MARKETFRONT-21869',
                    async test() {
                        const {counterStep, offerId} = this.params;
                        const initialCount = counterStep;

                        await this.browser.yaWaitForPageReady();
                        await this.browser.setState('Carter.items', [{
                            offerId,
                            id: '12345456',
                            creationTime: Date.now(),
                            label: 'g06judyquh9',
                            objType: 'OFFER',
                            objId: offerId,
                            count: initialCount,
                        }]);
                        await this.browser.yaReactPageReload();
                        await this.snippetCounterCartButton.increaseButton.click();

                        const expectedCounterValue = initialCount + counterStep;

                        await this.snippetCounterCartButton.waitUntilCounterChanged(initialCount, expectedCounterValue);

                        const sentToCarterCounterValue = await waitForSentToCarterCounterValue(this, 'changeItems');

                        return this.browser.allure.runStep(
                            'Проверяем значение каунтера в запросе в Carter',
                            () => this.expect(sentToCarterCounterValue)
                                .to.be.equal(expectedCounterValue, `В запросе в Картер количество товара равно ${sentToCarterCounterValue}`)
                        );
                    },
                }),
            },
            'по клику на кнопку уменьшения количества товара,': {
                'товар удаляется из корзины и кнопка каунтера заменяется на кнопку "В Корзину"': makeCase({
                    issue: 'MARKETFRONT-21869',
                    async test() {
                        const {counterStep = 1, offerId} = this.params;

                        await this.browser.yaWaitForPageReady();
                        await this.browser.setState('Carter.items', [{
                            offerId,
                            id: '12345456',
                            creationTime: Date.now(),
                            label: 'g06judyquh9',
                            objType: 'OFFER',
                            objId: offerId,
                            count: counterStep,
                        }]);
                        await this.browser.yaReactPageReload();
                        await this.snippetCounterCartButton.decreaseButton.click();
                        await this.browser.allure.runStep(
                            'Дожидаемся, что исчезновения каунтера',
                            () => this.browser.waitForVisible(SearchSnippetCartButton.counter, 10000, true)
                        );
                        await this.browser.allure.runStep(
                            'Дожидаемся появления кнопки "В Корзину"',
                            () => this.browser.waitForVisible(SearchSnippetCartButton.root, 10000)
                        );

                        let isRemovedFromCart = false;

                        await this.browser.allure.runStep(
                            'Ждём отправку запроса в Carter на удаление товара из Корзины',
                            async () => {
                                await this.browser.waitUntil(
                                    async () => {
                                        const logs = await this.browser
                                            .yaGetKadavrLogByBackendMethod('Carter', 'removeItems');

                                        if (logs.length > 0) {
                                            isRemovedFromCart = true;

                                            return true;
                                        }

                                        return false;
                                    },
                                    5000,
                                    'Запрос "Carter.removeItems" на удаление товара из корзины не найден в логах',
                                    200
                                );
                            }
                        );

                        return this.browser.allure.runStep(
                            'Проверяем, что товар удалён из Корзины',
                            () => this.expect(isRemovedFromCart)
                                .to.be.equal(true, 'Товар удалён из Корзины')
                        );
                    },
                }),
            },
        },

        'Если в Корзину добавлено несколько (> 1) единиц товара, то': {
            'по клику на кнопку уменьшения количества товара,': {
                'значение каунтера уменьшается на единицу товара': makeCase({
                    issue: 'MARKETFRONT-21869',
                    async test() {
                        const {counterStep, offerId} = this.params;
                        const initialCount = 2 * counterStep;

                        await this.browser.yaWaitForPageReady();
                        await this.browser.setState('Carter.items', [{
                            offerId,
                            id: '12345456',
                            creationTime: Date.now(),
                            label: 'g06judyquh9',
                            objType: 'OFFER',
                            objId: offerId,
                            count: initialCount,
                        }]);
                        await this.browser.yaReactPageReload();
                        await this.snippetCounterCartButton.decreaseButton.click();

                        const expectedCounterValue = initialCount - counterStep;

                        await this.snippetCounterCartButton.waitUntilCounterChanged(initialCount, expectedCounterValue);

                        const sentToCarterCounterValue = await waitForSentToCarterCounterValue(this, 'changeItems');

                        return this.browser.allure.runStep(
                            'Проверяем значение каунтера в запросе в Carter',
                            () => this.expect(sentToCarterCounterValue)
                                .to.be.equal(expectedCounterValue, `В запросе в Картер количество товара равно ${sentToCarterCounterValue}`)
                        );
                    },
                }),
            },
        },
    }),
});

async function waitForSentToCarterCounterValue(ctx, method) {
    const backend = 'Carter';
    let action = 'на изменение количества';

    if (method === 'addItem') {
        action = 'на добавление';
    }

    let sentToCarterCounterValue;

    await ctx.browser.allure.runStep(
        `Ждём отправку запроса в Carter ${action} товара`,
        () =>
            ctx.browser.waitUntil(
                async () => {
                    const logs = await ctx.browser
                        .yaGetKadavrLogByBackendMethod(backend, method);

                    if (logs.length > 0) {
                        if (method === 'changeItems') {
                            sentToCarterCounterValue = logs[0].request.body.items[0].count;
                        }

                        if (method === 'addItem') {
                            sentToCarterCounterValue = logs[0].request.body.count;
                        }

                        return true;
                    }

                    return false;
                },
                7000,
                `Запрос "${backend}.${method}" на ${action} не найден в логах`,
                200
            )
    );

    return sentToCarterCounterValue;
}
