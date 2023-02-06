import assert from 'assert';
import {makeSuite, makeCase, mergeSuites, prepareSuite} from 'ginny';

import RatingInput from '@self/root/src/components/RatingInput/__pageObject';

import OrderFeedbackFormView from '@self/root/src/widgets/content/OrderFeedback/components/View/__pageObject';
import OrderQuestions from '@self/root/src/widgets/content/OrderFeedback/components/OrderQuestions/__pageObject';
import Submit from '@self/root/src/widgets/content/OrderFeedback/components/Submit/__pageObject';
import OrderFeedbackScenario from '@self/root/src/widgets/content/OrderFeedback/components/Scenario/__pageObject';
import TerminalScreen from '@self/root/src/widgets/content/OrderFeedback/components/TerminalScreen/__pageObject';

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
                        RatingInput
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
                        Button
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

                    // await this.browser.yaRepl({_this: this});

                    await this.ratingControl.getSelectedStarsCountFromData()
                        .should.eventually.to.be.equal(3, 'Значение контрола совпадает с параметров в урле');
                },
            }),

            'Можно проставить оценку не отправляя форму целиком': makeCase({
                id: 'bluemarket-3504',
                issue: 'BLUEMARKET-10940',
                async test() {
                    await setupTest(this);
                    await this.ratingControl.setRating(3);
                    await this.ratingControl.getSelectedStarsCountFromData()
                        .should.eventually.to.be.equal(3, 'Значение контрола обновилось');
                },
            }),
            'Отправка формы с оценкой меньше 4': makeCase({
                id: 'bluemarket-3504',
                issue: 'BLUEMARKET-10940',
                async test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETVERSTKA-31797 нужно доделать тесты с выбором вопросов и товаров');

                    // eslint-disable-next-line no-unreachable
                    await setupTest(this, {
                        feedback: {
                            id: ORDER_ID,
                            orderId: ORDER_ID,
                            questions: [
                                {id: 0, title: 'Товар повреждён', category: 'PRODUCT'},
                                {id: 1, title: 'Привезли не то или не все товары', category: 'DELIVERY'},
                                {id: 2, title: 'Курьер был невежлив', category: 'DELIVERY'},
                                {id: 3, title: 'Упаковка повреждён', category: 'DELIVERY'},
                                {id: 4, title: 'Товар другого цвета', category: 'PRODUCT'},
                                {id: 5, title: 'Товар не работает', category: 'PRODUCT'},
                            ],
                            questionsByGrade: {
                                5: [3, 4, 5],
                                4: [3, 4, 5],
                                3: [0, 1, 2, 3, 4],
                                2: [0, 1, 2],
                                1: [0, 1, 2],
                            },
                        },
                    });
                    await this.ratingControl.setRating(3);
                    await this.ratingControl.getSelectedStarsCountFromData()
                        .should.eventually.to.be.equal(3, 'Значение контрола обновилось');
                },
            }),

            'Отправка формы c оценкой больше 3': makeCase({
                id: 'bluemarket-3504',
                issue: 'BLUEMARKET-10940',
                async test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETFRONT-43154 нужно доделать тесты');

                    // eslint-disable-next-line no-unreachable
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
