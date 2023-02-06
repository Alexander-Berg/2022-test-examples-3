import {makeCase, makeSuite} from 'ginny';
import AnswerForm from '@self/platform/spec/page-objects/AnswerForm';

/**
 * @param {PageObject.ProductQuestionPage | PageObject.CategoryQuestionPage} questionPage
 * @param {PageObject.AnswerForm} productAnswerForm
 */
export default makeSuite('Вопрос от пользователя, на который еще никто не ответил.', {
    feature: 'Структура страницы',
    story: {
        async beforeEach() {
            this.setPageObjects({
                productAnswerForm: () => this.createPageObject(AnswerForm),
            });
        },
        'При оставлении первого ответа к вопросу': {
            'зеленый блок оповещения об успешном оставлении вопроса исчезает': makeCase({
                issue: 'MOBMARKET-9107',
                id: 'm-touch-2245',
                async test() {
                    await this.productAnswerForm.clickTextField();
                    await this.productAnswerForm.setTextFieldInput('My awesome answer');
                    await this.productAnswerForm.clickOnSendAnswerButton();

                    const isGreenBlockNotVisible = await this.questionPage.waitForInlineBlockNotVisible();
                    await this.expect(isGreenBlockNotVisible).to.be.equal(true, 'Блок оповещения скрыт');
                },
            }),
            'блок "Пока нет ответов" с космонавтом скрывается': makeCase({
                issue: 'MOBMARKET-10882',
                id: 'm-touch-2940',
                async test() {
                    const isZeroStateCardVisible = await this.questionPage.isZeroStateCardVisible();
                    await this.expect(isZeroStateCardVisible).to.be.equal(
                        true,
                        'Блок "Пока нет ответов" с космонавтом отображается.'
                    );

                    await this.productAnswerForm.clickTextField();
                    await this.productAnswerForm.setTextFieldInput('Answer');
                    await this.productAnswerForm.clickOnSendAnswerButton();

                    const isZeroStateCardNotVisible = await this.questionPage.waitForZeroStateCardNotVisible();
                    await this.expect(isZeroStateCardNotVisible).to.be.equal(
                        true,
                        'Блок "Пока нет ответов" с космонавтом скрыт.'
                    );
                },
            }),
        },
    },
});
