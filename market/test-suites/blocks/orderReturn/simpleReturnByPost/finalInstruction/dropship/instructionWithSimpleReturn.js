import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import SpecificFinalText
    from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/SpecificFinalText/__pageObject';
import DropshipReturnContactsStep
    from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/DropshipReturnContacts/__pageObject';
import ReturnContacts from '@self/root/src/widgets/parts/ReturnCandidateContacts/components/View/__pageObject';
import ReturnContactItem from '@self/root/src/widgets/parts/ReturnCandidateContacts/components/ReturnContactItem/__pageObject';

import {replaceBreakChars} from '@self/root/src/spec/utils/text';
import RETURN_TEXT_CONSTANTS from '@self/root/src/widgets/parts/ReturnCandidate/constants/i18n';

import {
    FIRST_STEP_TITLE,
    SECOND_STEP_TITLE,
    DROPSHIP_RETURN_CONTACTS_LINK_TEXT,
    getFirstStepTexts,
    getSecondStepTexts,
} from '../constants/texts';


export default makeSuite('Лёгкий возврат включён. Дропшип / ДСБС. Инструкция по возврату', {
    id: 'marketfront-4848',
    params: {
        orderId: 'Идентификатор заказа',
        postAddress: 'Адрес почтового отделения',
        returnContacts: 'Контакты продавца для возврата',
        isDsbs: 'Является ли ДСБС-заказом',
    },
    story: {
        async beforeEach() {
            assert(this.params.orderId !== undefined, 'Param orderId must be defined');

            this.setPageObjects({
                specificFinalText: () => this.createPageObject(SpecificFinalText, {
                    parent: this.finalScreen,
                }),
                dropshipReturnContactsStep: () => this.createPageObject(DropshipReturnContactsStep, {
                    parent: this.returnsForm,
                }),
                returnContacts: () => this.createPageObject(ReturnContacts, {
                    parent: this.dropshipReturnContactsStep,
                }),
                returnContactItemPost: () => this.createPageObject(ReturnContactItem, {
                    parent: this.returnContacts,
                    root: `${ReturnContactItem.root}[data-auto="POST"]`,
                }),
                returnContactItemCarrier: () => this.createPageObject(ReturnContactItem, {
                    parent: this.returnContacts,
                    root: `${ReturnContactItem.root}[data-auto="CARRIER"]`,
                }),
                returnContactItemSelf: () => this.createPageObject(ReturnContactItem, {
                    parent: this.returnContacts,
                    root: `${ReturnContactItem.root}[data-auto="SELF"]`,
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

                const {orderId, isDsbs} = this.params;

                return this.specificFinalText.getStepTextByIndex(stepNumber)
                    .should.eventually.be.deep.equal(
                        getFirstStepTexts({orderId, isDsbs}),
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

        'Тексты третьего шага отображаются корректно': makeCase({
            async test() {
                const stepNumber = 3;

                await this.specificFinalText.getStepTitleTextByIndex(stepNumber)
                    .should.eventually.be.equal(
                        replaceBreakChars(
                            RETURN_TEXT_CONSTANTS.FINAL_STEP_SEND_BOX_POST_DROPSHIP_ALTERNATIVE_SIMPLE_RETURN
                        ),
                        `Заголовок ${stepNumber}-го шага должен быть корректным`
                    );

                return this.specificFinalText.getStepTextByIndex(stepNumber)
                    .should.eventually.be.deep.equal(
                        [
                            DROPSHIP_RETURN_CONTACTS_LINK_TEXT.HIDDEN,
                        ],
                        `Текст ${stepNumber}-го шага должен быть корректным`
                    );
            },
        }),

        'Другие способы возврата': {
            'по умолчанию не отображаются': makeCase({
                async test() {
                    await this.returnContacts.isVisible()
                        .should.eventually.be.equal(
                            false,
                            'Контакты продавца для возврата должны быть скрыты'
                        );

                    return this.dropshipReturnContactsStep.getLinkText()
                        .should.eventually.be.equal(
                            DROPSHIP_RETURN_CONTACTS_LINK_TEXT.HIDDEN,
                            'Текст ссылки в блоке с другими способами возврата должен быть корректным'
                        );
                },
            }),

            'при клике по ссылке появляются': makeCase({
                async test() {
                    await this.dropshipReturnContactsStep.clickLink();

                    await this.dropshipReturnContactsStep.getLinkText()
                        .should.eventually.be.equal(
                            DROPSHIP_RETURN_CONTACTS_LINK_TEXT.SHOWN,
                            'Текст ссылки в блоке с другими способами возврата должен быть корректным'
                        );

                    await this.returnContacts.waitForVisible();
                    await this.returnContacts.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Контакты продавца для возврата должны быть видны'
                        );

                    await this.returnContactItemPost.isVisible()
                        .should.eventually.be.equal(
                            false,
                            'Контакт продавца для возврата почтой не должен быть виден'
                        );

                    await this.returnContactItemCarrier.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Контакт продавца для возврата курьерской службой должен быть виден'
                        );

                    return this.returnContactItemSelf.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Контакт продавца для возврата своим ходом должен быть виден'
                        );
                },
            }),

            'при повторном клике скрываются': makeCase({
                async test() {
                    await this.dropshipReturnContactsStep.clickLink();

                    await this.returnContacts.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Контакты продавца для возврата должны быть видны'
                        );

                    await this.dropshipReturnContactsStep.clickLink();

                    await this.returnContacts.isVisible()
                        .should.eventually.be.equal(
                            false,
                            'Контакты продавца для возврата должны быть скрыты'
                        );

                    return this.dropshipReturnContactsStep.getLinkText()
                        .should.eventually.be.equal(
                            DROPSHIP_RETURN_CONTACTS_LINK_TEXT.HIDDEN,
                            'Текст ссылки в блоке с другими способами возврата должен быть корректным'
                        );
                },
            }),
        },
    },
});
