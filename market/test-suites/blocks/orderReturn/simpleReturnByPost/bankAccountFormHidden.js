import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import {postOutlet} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import {selectOutletAndGoToNextStep} from '@self/root/src/spec/hermione/scenarios/returns';

export default makeSuite('Форма реквизитов не отображается', {
    params: {
        fillFormScenarioParams: 'Параметры для сценария fillReturnFormAndGoToMapStep',
        isOrderPrepaid: 'Является ли заказ предоплаченным',
    },
    story: {
        beforeEach() {
            assert(this.params.isOrderPrepaid !== undefined, 'Param isOrderPrepaid must be defined');
        },

        'На шаге про возврат средств': {
            'форма для заполнения реквизитов не отображается': makeCase({
                async test() {
                    await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {
                        outlet: postOutlet,
                    });

                    return this.bankAccountForm.isVisible()
                        .should.eventually.be.equal(
                            false,
                            'Форма реквизитов не должна отображаться'
                        );
                },
            }),
        },
    },
});
