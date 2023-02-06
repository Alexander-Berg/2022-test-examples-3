import {makeSuite, makeCase, mergeSuites} from 'ginny';

const WAITING_LOADING_TIMEOUT = 10000;
const WAITING_GOAL_TIMEOUT = 4000;

/**
 * Тесты на видимость блока.
 */

export default makeSuite('Метрика: видимость блока.', {
    feature: 'Metrica',
    params: {
        expectedGoalName: 'ожидаемое имя цели',
        counterId: 'Идентификатор счётчика Яндекс Метрики',
        payloadSchema: 'Схема payload`а цели',
        scrollSelector: 'Селектор, до которого будет осуществлена прокрутка',
        lazySelector: 'Селектор, по которому определяется, что ленивый блок загрузился',
        selector: '[deprecated - use scrollSelector] Селектор, до которого будет осуществлена прокрутка',
    },
    story: mergeSuites(
        {
            'По умолчанию': {
                'отображается': makeCase({
                    async test() {
                        const {
                            expectedGoalName,
                            counterId,
                            payloadSchema,
                            selector,
                            lazySelector,
                            scrollSelector = selector,
                        } = this.params;
                        const schema = JSON.stringify(payloadSchema);

                        await this.browser.yaSlowlyScroll(scrollSelector);

                        if (lazySelector) {
                            await this.browser.waitUntil(() => this.browser.isExisting(lazySelector),
                                WAITING_LOADING_TIMEOUT, 'Блок не загрузился');
                        }

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
