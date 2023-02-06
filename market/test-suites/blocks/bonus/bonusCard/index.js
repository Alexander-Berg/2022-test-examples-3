import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import allBonuses from '@self/root/src/spec/hermione/kadavr-mock/loyalty/bonuses';

import coinPopup from './coinPopup';

module.exports = makeSuite('Экран купона.', {
    feature: 'Экран купона.',
    params: {
        coin: 'Выбранный купон',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('Loyalty.collections.bonus', [this.params.coin]);

                return this.browser.yaOpenPage('market:bonus');
            },
        },

        prepareSuite(coinPopup, {
            suiteName: 'Попап купона. Активный купон.',
            meta: {
                id: 'bluemarket-3237',
                issue: 'BLUEMARKET-9943',
            },
            params: {
                coin: allBonuses.WITH_CATEGORY_REF,
            },
        })
    ),
});
