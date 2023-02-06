import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import SpecificFinalText
    from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/SpecificFinalText/__pageObject';

import {
    FIRST_STEP_TITLE,
    SECOND_STEP_TITLE,
    getFirstStepTexts,
    getSecondStepTexts,
} from '../constants/texts';


export default makeSuite('Лёгкий возврат включён. Инструкция по возврату', {
    id: 'marketfront-4847',
    params: {
        orderId: 'Идентификатор заказа',
        postAddress: 'Адрес почтового отделения',
    },
    story: {
        async beforeEach() {
            assert(this.params.orderId !== undefined, 'Param orderId must be defined');

            this.setPageObjects({
                specificFinalText: () => this.createPageObject(SpecificFinalText, {
                    parent: this.finalScreen,
                }),
            });

            await this.specificFinalText.isVisible()
                .should.eventually.to.be.equal(
                    true,
                    'Содержимое экрана успешного оформления заявления должно отобразиться'
                );
        },

        'Тексты первого шага отображаются корректно': makeCase({
            async test() {
                const stepNumber = 1;

                await this.specificFinalText.getStepTitleTextByIndex(stepNumber)
                    .should.eventually.be.equal(
                        FIRST_STEP_TITLE,
                        `Заголовок ${stepNumber}-го шага должен быть корректным`
                    );

                return this.specificFinalText.getStepTextByIndex(stepNumber)
                    .should.eventually.be.deep.equal(
                        getFirstStepTexts({orderId: this.params.orderId}),
                        `Текст ${stepNumber}-го шага должен быть корректным`
                    );
            },
        }),

        'Тексты второго шага отображаются корректно': makeCase({
            async test() {
                const stepNumber = 2;

                await this.specificFinalText.getStepTitleTextByIndex(stepNumber)
                    .should.eventually.be.equal(
                        SECOND_STEP_TITLE,
                        `Заголовок ${stepNumber}-го шага должен быть корректным`
                    );

                return this.specificFinalText.getStepTextByIndex(stepNumber)
                    .should.eventually.be.deep.equal(
                        getSecondStepTexts(this.params.postAddress),
                        `Текст ${stepNumber}-го шага должен быть корректным`
                    );
            },
        }),
    },
});
