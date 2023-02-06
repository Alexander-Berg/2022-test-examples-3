import {makeSuite, makeCase, mergeSuites} from 'ginny';

const WAITING_LOADING_TIMEOUT = 10000;
const WAITING_GOAL_TIMEOUT = 4000;

/**
 * Метрика: тест на клик по ссылке
 */
export default makeSuite('Метрика: клик по ссылке.', {
    feature: 'Metrica',
    params: {
        expectedGoalName: 'ожидаемое имя цели',
        counterId: 'Идентификатор счётчика Яндекс Метрики',
        payloadSchema: 'Схема payload`а цели',
        scrollSelector: 'Селектор, до которого будет осуществлена прокрутка',
        lazySelector: 'Селектор, по которому определяется, что ленивый блок загрузился',
        selector: 'Селектор нажимаемой ссылки',
    },
    story: mergeSuites(
        {
            'По клику': {
                'цель срабатывает': makeCase({
                    async test() {
                        const {
                            expectedGoalName,
                            counterId,
                            payloadSchema,
                            selector,
                            lazySelector,
                            scrollSelector = selector,
                        } = this.params;

                        await this.browser.yaSlowlyScroll(scrollSelector);

                        if (lazySelector) {
                            await this.browser.waitUntil(() => this.browser.isExisting(lazySelector),
                                WAITING_LOADING_TIMEOUT, 'Блок не загрузился');
                        }

                        await this.browser.yaPreventDefaultLinkAction(selector);
                        await this.allure.runStep(
                            'Кликаем по ссылке',
                            () => this.browser.click(selector)
                        );

                        const schema = JSON.stringify(payloadSchema);

                        const goal = await this.allure.runStep(
                            `Ищем цель "${expectedGoalName}" по схеме: ${schema}.`,
                            () => this.browser.yaGetMetricaGoal(
                                counterId,
                                expectedGoalName,
                                payloadSchema,
                                WAITING_GOAL_TIMEOUT
                            )
                        );

                        return this.expect(Boolean(goal)).be.equal(
                            true,
                            `Найденная цель должна соответствовать схеме ${schema}.`
                        );
                    },
                }),
            },
        }
    ),
});
