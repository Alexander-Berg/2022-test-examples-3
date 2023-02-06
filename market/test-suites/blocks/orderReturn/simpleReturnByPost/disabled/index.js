import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {disabled} from '@self/root/src/spec/hermione/kadavr-mock/tarantino/mp_simple_return_by_post_toggle';

import fulfillmentSuite from './fulfillment';

export default makeSuite('Функционал лёгкого возврата выключен в CMS', {
    params: {
        items: 'Товары',
        fillFormScenarioParams: 'Параметры для сценария fillReturnFormAndGoToMapStep',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                /**
                 * CMS-мок с доступностью лёгкого возврата
                 */
                await this.browser.setState(
                    'Tarantino.data.result',
                    [disabled]
                );
            },
        },

        prepareSuite(fulfillmentSuite)
    ),
});
