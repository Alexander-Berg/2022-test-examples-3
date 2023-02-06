import {makeSuite, makeCase, mergeSuites} from 'ginny';

const WAITING_GOAL_TIMEOUT = 4000;

/**
 * Метрика: тест на клик по ссылке через pageObject
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
                        } = this.params;

                        await this.snippet.waitForVisible();

                        if (this.snippet.clickoutButton) {
                            await this.snippet.clickoutButton.click();
                        }

                        if (this.snippet.cartButton) {
                            await this.snippet.clickCartButton();
                        }

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
