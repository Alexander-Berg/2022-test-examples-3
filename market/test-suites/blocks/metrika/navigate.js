import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';
import schema from 'js-schema';
import {parseAppUrl} from '@self/root/src/spec/utils/url';
import {buildGoalName} from '@self/root/src/spec/utils/metrika';

const path = require('path');

const nodeConfig = require(path.resolve('.', 'configs/current/node'));

const CLICK_ACTION = 'NAVIGATE';

/**
 * Метрика: тест на клик по ссылке
 */
module.exports = makeSuite('Метрика: клик.', {
    params: {
        goalNamePrefix: 'Тестируемая зона',
        payloadSchema: 'Обязательные параметры метрики',
        counterId: 'id счетчика метрики',
        isScrolled: 'нужно ли скроллить до селектора',
    },
    defaultParams: {
        counterId: nodeConfig.yaMetrika.market.id,
        isScrolled: true,
    },
    story: mergeSuites(
        {
            'По клику': {
                'срабатывает событие навигации': makeCase({
                    async test() {
                        const {goalNamePrefix, counterId, payloadSchema} = this.params;
                        const testedElement = this.testedElement;
                        const selector = await testedElement.getSelector();
                        const expectedGoalName = buildGoalName(goalNamePrefix, CLICK_ACTION);
                        let actualUrl;
                        let reachedGoals;

                        if (this.params.isScrolled) {
                            await this.allure.runStep(
                                `Скроллим к селектору ${selector}`, () =>
                                    this.browser.scroll(selector, 1)
                            );
                        }

                        // Отключить переход нужно, чтобы прочитать отправленное событие.
                        await this.allure.runStep(
                            'Отключаем переход по ссылке', () =>
                                this.browser.yaPreventDefaultLinkAction(selector)
                                    .then(url => { actualUrl = url; })
                        );

                        await this.allure.runStep(
                            'Кликаем по элементу', () =>
                                this.browser.click(selector)
                        );

                        await this.allure.runStep(
                            'Получаем метрику, соответствующую схеме', async () => {
                                reachedGoals = await this.browser.yaGetMetricaGoal(
                                    counterId,
                                    expectedGoalName,
                                    schema(payloadSchema)
                                );

                                return this.expect(reachedGoals.length).to.equal(
                                    1,
                                    'Должна сгенерироваться одна цель клика'
                                );
                            }
                        );

                        await this.allure.runStep(
                            'Переходим на страницу, соответствующую полученной цели', async () => {
                                const parsedActualUrl = parseAppUrl(actualUrl, 'GET');
                                const metrikaParams = reachedGoals[0];

                                return this.expect({
                                    pageId: parsedActualUrl.pageId,
                                    params: parsedActualUrl.params,
                                }).to.deep.equal({
                                    pageId: metrikaParams.pageId,
                                    params: metrikaParams.params,
                                });
                            }
                        );
                    },
                }),
            },
        }
    ),
});
