import assert from 'assert';
import {makeSuite, makeCase, mergeSuites, prepareSuite} from 'ginny';

import OrderFeedbackFormView from '@self/root/src/widgets/content/OrderFeedback/components/View/__pageObject';
import OrderQuestions from '@self/root/src/widgets/content/OrderFeedback/components/OrderQuestions/__pageObject';
import Submit from '@self/root/src/widgets/content/OrderFeedback/components/Submit/__pageObject';
import OrderFeedbackScenario from '@self/root/src/widgets/content/OrderFeedback/components/Scenario/__pageObject';
import TerminalScreen from '@self/root/src/widgets/content/OrderFeedback/components/TerminalScreen/__pageObject';
import RatingControl from '@self/root/src/uikit/components/RatingControl/__pageObject';
import {TextField} from '@self/root/src/uikit/components/TextField/__pageObject';
import Checkbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import yaBuildURL from '@self/root/src/spec/hermione/commands/yaBuildURL';

import {ORDER_ID, setupFeedback, setupTest as _st} from './utils/common';
import EntryPointToReferralProgram from './entryPointToReferralProgram';

const setupTest = async (ctx, {pageParams = {}, feedback = {}, ...other} = {}) => {
    await setupFeedback(ctx, feedback);
    await _st(ctx, {
        pageParams: {
            orderId: ORDER_ID,
            grade: 4,
            ...pageParams,
        },
        ...other,
    });
};

function openPage(ctx, pageParams = {}) {
    return ctx.browser.yaOpenPage(ctx.params.pageId, {
        orderId: ORDER_ID,
        ...pageParams,
    });
}

export default makeSuite('Форма оценки заказа', {
    environment: 'kadavr',
    feature: 'Оценка заказа',
    story: mergeSuites(
        {
            async beforeEach() {
                assert(this.params.pageId, 'Param pageId must be defined');
                this.setPageObjects({
                    form: () => this.createPageObject(
                        OrderFeedbackFormView,
                        {parent: this.orderFeedbackPage}
                    ),
                    orderFeedbackScenario: () => this.createPageObject(
                        OrderFeedbackScenario,
                        {parent: this.form}
                    ),
                    terminalScreen: () => this.createPageObject(
                        TerminalScreen,
                        {parent: this.orderFeedbackPage}
                    ),
                    ratingControl: () => this.createPageObject(
                        RatingControl,
                        {parent: this.form}
                    ),
                    orderQuestions: () => this.createPageObject(
                        OrderQuestions,
                        {parent: this.form}
                    ),
                    submit: () => this.createPageObject(
                        Submit,
                        {parent: this.form}
                    ),
                    textField: () => this.createPageObject(
                        TextField,
                        {parent: this.form}
                    ),
                    checkbox: () => this.createPageObject(
                        Checkbox,
                        {parent: OrderQuestions.getQuestionByIndex(1)}
                    ),
                    submitButton: () => this.createPageObject(
                        Button,
                        {parent: this.submit}
                    ),
                    primaryButton: () => this.createPageObject(
                        Button,
                        {root: TerminalScreen.primaryButton}
                    ),
                });
            },
            'Количество звезд совпадает с количеством, переданным в параметре grade': makeCase({
                id: 'bluemarket-3504',
                issue: 'BLUEMARKET-10940',
                async test() {
                    await setupTest(this, {feedback: {grade: 1}, pageParams: {grade: 3}});
                    await this.ratingControl.currentGrade
                        .should.eventually.to.be.equal('3', 'Значение контрола совпадает с параметров в урле');
                    await openPage(this);
                    await this.ratingControl.currentGrade
                        .should.eventually.to.be.equal('3', 'Значение контрола совпадает с заданным ранее');
                },
            }),

            'Можно проставить оценку не отправляя форму целиком': makeCase({
                id: 'bluemarket-3504',
                issue: 'BLUEMARKET-10940',
                async test() {
                    await setupTest(this);
                    await this.ratingControl.clickOnGrade('3');
                    await this.ratingControl.currentGrade
                        .should.eventually.to.be.equal('3', 'Значение контрола обновилось');

                    // Два события putOrderFeedback в логах кадавра
                    // 1 - заход на страницу, 2 - клик по звезде
                    await this.browser.waitUntil(
                        async () => (
                            await this.browser.yaGetKadavrLogByBackendMethod('OrderFeedback', 'putOrderFeedback')
                        ).length === 2,
                        1500
                    );
                    await openPage(this);
                    await this.ratingControl.currentGrade
                        .should.eventually.to.be.equal('3', 'Значение контрола совпадает с заданным ранее');
                },
            }),

            'Отправка формы': makeCase({
                id: 'bluemarket-3504',
                issue: 'BLUEMARKET-10940',
                async test() {
                    const comment = 'Короткий отзыв о моей радости общения с курьером';
                    await setupTest(this);
                    await this.textField.setText(comment);
                    const question = await this.orderQuestions.getQuestionTextByIndex(1);
                    await this.checkbox.toggle();
                    await this.ratingControl.clickOnGrade('1');
                    await this.orderQuestions.getQuestionTextByIndex(1).should.eventually
                        .not.equal(question, 'Проверяем, что текст вопроса для более низкой оценки поменяется');
                    // в аллуру не пишется второй аргумент eventually. Ставим степ руками
                    await this.browser.allure.runStep('Проверяем, что для более низкой оценки галочка снимается', () =>
                        this.checkbox.isChecked().should.eventually.equal(false)
                    );
                    await this.ratingControl.clickOnGrade('5');
                    await this.orderQuestions.getQuestionTextByIndex(1).should.eventually
                        .equal(question, 'Проверяем, что текст вопроса поменяется обратно');
                    await this.browser.allure.runStep('Проверяем, что галочка проставится обратно', () =>
                        this.checkbox.isChecked().should.eventually.equal(true)
                    );

                    await this.browser.allure.runStep('Отправляем результат заполнения формы', () =>
                        this.submitButton.click()
                    );

                    await this.orderFeedbackScenario.waitForVisible(2000).should.eventually
                        .equal(true, 'Проверяем, что сценарий THANK_YOU отобразился');
                    await this.terminalScreen.message.getText().should.eventually
                        .equal(
                            'Мы это очень ценим. Возвращайтесь на Яндекс.Маркет!',
                            'Проверяем текст сообщения'
                        );
                    await this.browser.allure.runStep('Жмем "продолжить"', () => this.primaryButton.click());
                    await this.browser.allure.runStep('Попадаем на главную страницу', () => this.browser.getUrl()
                        .should.eventually.be.link({
                            pathname: yaBuildURL(PAGE_IDS_COMMON.INDEX, {}),
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                            skipPathname: false,
                        })
                    );
                    await openPage(this);
                    await this.browser.allure.runStep('Галочка на месте', () =>
                        this.checkbox.isChecked().should.eventually.equal(true)
                    );
                    await this.browser.allure.runStep('Текст комментария на месте', () =>
                        this.browser.yaGetValue(this.textField.getTextFieldElement()).should.eventually.equal(comment)
                    );
                },
            }),
        },
        prepareSuite(EntryPointToReferralProgram)
    ),
});
