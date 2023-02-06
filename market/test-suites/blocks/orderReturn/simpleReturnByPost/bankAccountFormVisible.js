import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import {postOutlet} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import {selectOutletAndGoToNextStep} from '@self/root/src/spec/hermione/scenarios/returns';

export default makeSuite('Форма реквизитов отображается', {
    params: {
        fillFormScenarioParams: 'Параметры для сценария fillReturnFormAndGoToMapStep',
        isOrderPrepaid: 'Является ли заказ предоплаченным',
    },
    story: {
        beforeEach() {
            assert(this.params.isOrderPrepaid !== undefined, 'Param isOrderPrepaid must be defined');
        },

        'На шаге про возврат средств': {
            'форма для заполнения реквизитов отображается': makeCase({
                async test() {
                    await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {
                        outlet: postOutlet,
                    });

                    await this.bankAccountForm.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Форма реквизитов должна отображаться'
                        );

                    await this.bankAccountForm.isFullNameVisible()
                        .should.eventually.be.equal(
                            true,
                            'Поле для имени владельца счёта должно быть отображено'
                        );

                    await this.bankAccountForm.isBikVisible()
                        .should.eventually.be.equal(
                            true,
                            'Поле для БИК должно быть отображено'
                        );

                    return this.bankAccountForm.isAccountVisible()
                        .should.eventually.be.equal(
                            true,
                            'Поле для номера расчётного счёта должно быть отображено'
                        );
                },
            }),
        },
    },
});
