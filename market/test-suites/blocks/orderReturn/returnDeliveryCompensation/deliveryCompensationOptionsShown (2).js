import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {fillBankAccountForm} from '@self/root/src/spec/hermione/scenarios/bankAccount';
import {bankAccount} from '@self/root/src/spec/hermione/configs/returns/formData';
import {RETURN_CANDIDATE_DELIVERY_COMPENSATION_TYPE} from '@self/root/src/constants/returnCandidate';
import {
    submitReturnAndCheckRequestDeliveryCompensationValue,
} from '@self/root/src/spec/hermione/scenarios/returns';

export default makeSuite('Опции компенсации обратной доставки отображаются', {
    story: mergeSuites(
        {
            async beforeEach() {
                return this.returnDeliveryCompensationOptions.waitForVisible()
                    .should.eventually.be.equal(
                        true,
                        'Опции компенсации обратной доставки должны быть отображены'
                    );
            },
        },

        makeSuite('По умолчанию', {
            story: {
                'При сабмите возврата': {
                    'в чекаутер передаётся корректная опция компенсации возврата': makeCase({
                        test() {
                            return this.browser.yaScenario(this, submitReturnAndCheckRequestDeliveryCompensationValue, {
                                expectedValue: RETURN_CANDIDATE_DELIVERY_COMPENSATION_TYPE.YANDEX_PLUS,
                            });
                        },
                    }),
                },
            },
        }),

        makeSuite('Выбрана опция денежной компенсации возврата', {
            story: {
                async beforeEach() {
                    await this.returnDeliveryCompensationOptions.selectOptionByType(
                        RETURN_CANDIDATE_DELIVERY_COMPENSATION_TYPE.MONEY
                    );
                },

                'При сабмите возврата': {
                    'в чекаутер передаётся корректная опция компенсации возврата': makeCase({
                        async test() {
                            await this.browser.yaScenario(this, fillBankAccountForm, bankAccount);

                            return this.browser.yaScenario(this, submitReturnAndCheckRequestDeliveryCompensationValue, {
                                expectedValue: RETURN_CANDIDATE_DELIVERY_COMPENSATION_TYPE.MONEY,
                                shouldBankAccountFormBeShown: true,
                            });
                        },
                    }),
                },
            },
        }),

        makeSuite('Вручную выбрана опция компенсации возврата баллами Плюса', {
            story: {
                async beforeEach() {
                    /**
                     * Специально сначала переключаем на опцию возврата деньгами,
                     * чтобы проверить, что при ручном выборе опции возврата баллами
                     * всё работает так же корректно, как при её предвыборе по умолчанию
                     */
                    await this.returnDeliveryCompensationOptions.selectOptionByType(
                        RETURN_CANDIDATE_DELIVERY_COMPENSATION_TYPE.MONEY
                    );

                    await this.returnDeliveryCompensationOptions.selectOptionByType(
                        RETURN_CANDIDATE_DELIVERY_COMPENSATION_TYPE.YANDEX_PLUS
                    );
                },

                'При сабмите возврата': {
                    'в чекаутер передаётся корректная опция компенсации возврата': makeCase({
                        test() {
                            return this.browser.yaScenario(this, submitReturnAndCheckRequestDeliveryCompensationValue, {
                                expectedValue: RETURN_CANDIDATE_DELIVERY_COMPENSATION_TYPE.YANDEX_PLUS,
                            });
                        },
                    }),
                },
            },
        })
    ),
});
