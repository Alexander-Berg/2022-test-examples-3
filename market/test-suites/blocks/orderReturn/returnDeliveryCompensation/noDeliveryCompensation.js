import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {
    submitReturnAndCheckRequestDeliveryCompensationValue,
} from '@self/root/src/spec/hermione/scenarios/returns';

export default makeSuite('Компенсация обратной доставки возврата отсутствует', {
    story: mergeSuites(
        makeSuite('По умолчанию', {
            story: {
                'При сабмите возврата': {
                    'в чекаутер передаётся корректная опция компенсации возврата': makeCase({
                        async test() {
                            return this.browser.yaScenario(this, submitReturnAndCheckRequestDeliveryCompensationValue, {
                                expectedValue: null,
                            });
                        },
                    }),
                },
            },
        })
    ),
});
