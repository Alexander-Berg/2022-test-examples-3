import {makeSuite, makeCase} from 'ginny';
import {head} from 'ambar';
import schema from 'js-schema';
import nodeConfig from '@self/platform/configs/current/node';

/**
 * Тест на фильтры на вкладке Цены страницы КМ
 * @property {PageObject.FilterGoodStateItem} filterGoodStateItem - radiobutton состояния фильтра "Состояние товара"
 */
export default makeSuite('Фильтры выдачи', {
    story: {
        'Метрика': {
            'При переключении фильтра "Состояние товара"': {
                'цель срабатывает': makeCase({
                    id: 'marketfront-3089',
                    issue: 'MARKETVERSTKA-32628',
                    async test() {
                        await this.filterGoodStateItem.click();

                        const goals = await this.browser.yaGetMetricaGoal(
                            nodeConfig.yaMetrika.market.id,
                            'FLTRUSE_OFFERS',
                            schema({
                                info: {
                                    id: String,
                                    value: String,
                                },
                                reqId: String,
                            })
                        );

                        const goal = head(goals);

                        await this.expect(goal.info.id)
                            .to.be.equal(
                                'good-state',
                                'Goal метрики содержит поле id = "good-state"'
                            );

                        return this.expect(goal.info.value)
                            .to.be.equal(
                                'new',
                                'Goal метрики содержит поле value = "new"'
                            );
                    },
                }),
            },
        },
    },
});
