import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {fillBankAccountForm} from '@self/root/src/spec/hermione/scenarios/bankAccount';
import {bankAccount} from '@self/root/src/spec/hermione/configs/returns/formData';
import {
    submitReturnAndCheckRequestDeliveryCompensationValue,
} from '@self/root/src/spec/hermione/scenarios/returns';

export default makeSuite('Опции компенсации обратной доставки скрыты', {
    story: mergeSuites(
        makeSuite('По умолчанию', {
            story: {
                'При сабмите возврата': {
                    'в чекаутер передаётся корректная опция компенсации возврата': makeCase({
                        async test() {
                            await this.browser.yaScenario(this, fillBankAccountForm, {
                                ...bankAccount,
                                shouldSubmit: false,
                            });

                            await this.submitForm.submit();
                            await this.bankAccountForm.waitForVisibleBank();

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
