import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import SpecificFinalText
    from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/SpecificFinalText/__pageObject';

import {replaceBreakChars} from '@self/root/src/spec/utils/text';
import RETURN_TEXT_CONSTANTS from '@self/root/src/widgets/parts/ReturnCandidate/constants/i18n';
import {
    FIRST_STEP_TITLE,
    getFirstStepTexts,
} from '../constants/texts';


export default makeSuite('Лёгкий возврат отключён. Инструкция по возврату', {
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
                        replaceBreakChars(RETURN_TEXT_CONSTANTS.FINAL_STEP_SEND_BOX_ALTERNATIVE),
                        `Заголовок ${stepNumber}-го шага должен быть корректным`
                    );

                return this.specificFinalText.getStepTextByIndex(stepNumber)
                    .should.eventually.be.deep.equal(
                        [
                            `Вы выбрали почтовое отделение по адресу: ${this.params.postAddress}.`,
                            replaceBreakChars(RETURN_TEXT_CONSTANTS.FINAL_STEP_SEND_BOX_POST_1_ALTERNATIVE),
                        ],
                        `Текст ${stepNumber}-го шага должен быть корректным`
                    );
            },
        }),

        'Тексты третьего шага отображаются корректно': makeCase({
            async test() {
                const stepNumber = 3;

                await this.specificFinalText.getStepTitleTextByIndex(stepNumber)
                    .should.eventually.be.equal(
                        replaceBreakChars(RETURN_TEXT_CONSTANTS.FINAL_SEND_PHOTO_TITLE),
                        `Заголовок ${stepNumber}-го шага должен быть корректным`
                    );

                return this.specificFinalText.getStepTextByIndex(stepNumber)
                    .should.eventually.be.deep.equal(
                        [
                            'Через форму — вернём расходы, если товар с браком или не тот, что вы заказывали. ' +
                            'А ещё сможем отслеживать посылку.',
                        ],
                        `Текст ${stepNumber}-го шага должен быть корректным`
                    );
            },
        }),
    },
});
