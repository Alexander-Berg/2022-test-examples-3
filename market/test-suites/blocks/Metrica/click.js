import {makeSuite, makeCase, mergeSuites} from 'ginny';

const WAITING_GOAL_TIMEOUT = 4000;

/**
 * Метрика: тест на клик по ссылке
 */
export default makeSuite('Метрика: клик по ссылке.', {
    feature: 'Metrica',
    story: mergeSuites(
        {
            'По клику': {
                'цель срабатывает': makeCase({
                    async test() {
                        const {
                            expectedGoalName,
                            counterId,
                            payloadSchema: filterPredicate,
                            selector,
                            scrollDown = false,
                            scrollOffset = [0, 0],
                        } = this.params;

                        if (scrollDown) {
                            for (let i = 1; i <= 10; i++) {
                                // eslint-disable-next-line no-await-in-loop
                                await this.browser.scroll(undefined, 0, i * 400);
                                // eslint-disable-next-line no-await-in-loop, market/ginny/no-pause
                                await this.browser.pause(200);
                            }
                            // eslint-disable-next-line market/ginny/no-pause
                            await this.browser.pause(2000);
                        }

                        await this.browser.scroll(selector, ...scrollOffset);
                        await this.browser.yaPreventDefaultLinkAction(selector);
                        await this.allure.runStep(
                            'Кликаем по ссылке',
                            () => this.browser.click(selector)
                        );

                        const goal = await this.browser.yaGetMetricaGoal(
                            counterId,
                            expectedGoalName,
                            filterPredicate,
                            WAITING_GOAL_TIMEOUT
                        );
                        const schema = JSON.stringify(filterPredicate);

                        await this.allure.runStep(
                            `Ищем цель из полученных по схеме: ${schema}.`,
                            () => {}
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
