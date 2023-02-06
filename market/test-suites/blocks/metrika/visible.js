import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';
import schema from 'js-schema';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const path = require('path');

const nodeConfig = require(path.resolve('.', 'configs/current/node'));

const VISIBLE = 'CIA-VISIBLE';

/**
 * Тесты на видимость блока.
 */
module.exports = makeSuite('Метрика: видимость блока.', {
    params: {
        goalNamePrefix: 'Тестируемая зона',
        payloadSchema: 'Обязательные параметры метрики',
        counterId: 'id счетчика метрики',
    },
    defaultParams: {
        counterId: nodeConfig.yaMetrika.market.id,
    },
    story: mergeSuites(
        {
            'По умолчанию': {
                'срабатывает событие показа': makeCase({
                    async test() {
                        const {goalNamePrefix, counterId, payloadSchema} = this.params;
                        const testedElement = this.testedElement;
                        const selector = await testedElement.getSelector();
                        const expectedGoalName = buildGoalName(goalNamePrefix, VISIBLE);

                        await this.browser.allure.runStep(`Скроллим к селектору ${selector}`, () =>
                            this.browser.scroll(selector)
                        );

                        const reachedGoals = await this.browser.yaGetMetricaGoal(
                            counterId,
                            expectedGoalName,
                            schema(payloadSchema)
                        );

                        return this.expect(reachedGoals.length).to.equal(
                            1,
                            'Должна сгенерироваться одна цель показа'
                        );
                    },
                }),
            },
        }
    ),
});
